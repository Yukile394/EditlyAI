/**
 * Editly AI - Sunucu Tarafı Fonksiyonlar
 * ==========================================
 * Bu dosya, istemcide (Android uygulaması) kolayca kandırılabilecek iki
 * kritik kontrolü SUNUCU TARAFINDA güvenilir şekilde yapar:
 *
 *   1) verifyPurchase   -> Google Play Developer API ile gerçek abonelik
 *                          durumunu doğrular, Firestore'a yazar.
 *   2) consumeCredit    -> Günlük AI düzenleme hakkını Firestore'da,
 *                          transaction ile (yarış durumu olmadan) yönetir.
 *   3) claimRewardedCredit -> Ödüllü reklam sonrası +1 hak (bkz. aşağıdaki
 *                          GÜVENLİK NOTU - bunu AdMod SSV ile güçlendirin).
 *
 * KURULUM:
 *   cd functions && npm install
 *   firebase deploy --only functions
 *
 * Google Play Developer API için servis hesabı:
 *   1) Play Console > Kurulum > API erişimi > "Yeni servis hesabı oluştur"
 *      linkinden Google Cloud Console'a gidin, bir servis hesabı JSON
 *      anahtarı indirin.
 *   2) Play Console'da bu servis hesabına "Finansal veriler" iznini verin.
 *   3) JSON anahtarını ASLA repoya commitlemeyin. Bunun yerine:
 *        firebase functions:secrets:set PLAY_SERVICE_ACCOUNT_KEY
 *      komutuyla (JSON içeriğini yapıştırarak) Secret Manager'a kaydedin.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { google } = require("googleapis");

admin.initializeApp();
const db = admin.firestore();

const PLAY_SERVICE_ACCOUNT_KEY = defineSecret("PLAY_SERVICE_ACCOUNT_KEY");

const PACKAGE_NAME = "com.editlyai.app";
const FREE_DAILY_LIMIT = 5;
const PREMIUM_DAILY_LIMIT = 100;

function todayUtcString() {
  return new Date().toISOString().slice(0, 10); // "YYYY-MM-DD"
}

function requireAuth(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Bu işlem için giriş yapılmış olmalı.");
  }
  return request.auth.uid;
}

/**
 * Google Play Developer API istemcisi oluşturur (servis hesabı anahtarıyla).
 */
async function getAndroidPublisherClient() {
  const keyJson = PLAY_SERVICE_ACCOUNT_KEY.value();
  if (!keyJson) {
    throw new HttpsError(
      "failed-precondition",
      "PLAY_SERVICE_ACCOUNT_KEY secret'ı tanımlı değil. README'deki kurulum adımlarını uygulayın."
    );
  }
  const credentials = JSON.parse(keyJson);
  const auth = new google.auth.GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  return google.androidpublisher({ version: "v3", auth });
}

/**
 * 1) SATIN ALMA DOĞRULAMA
 * İstemci (BillingManager) bir satın alma/abonelik sonrası bunu çağırır.
 * purchaseToken'ı Google'a karşı doğrular; sonuca göre Firestore'daki
 * kullanıcı belgesini (users/{uid}) günceller.
 */
exports.verifyPurchase = onCall({ secrets: [PLAY_SERVICE_ACCOUNT_KEY] }, async (request) => {
  const uid = requireAuth(request);
  const { purchaseToken, subscriptionId } = request.data || {};

  if (!purchaseToken || !subscriptionId) {
    throw new HttpsError("invalid-argument", "purchaseToken ve subscriptionId gerekli.");
  }

  const publisher = await getAndroidPublisherClient();

  let subscription;
  try {
    const res = await publisher.purchases.subscriptions.get({
      packageName: PACKAGE_NAME,
      subscriptionId,
      token: purchaseToken,
    });
    subscription = res.data;
  } catch (err) {
    logger.error("Play Developer API doğrulama hatası", err);
    throw new HttpsError("internal", "Satın alma doğrulanamadı.");
  }

  const expiryTimeMillis = parseInt(subscription.expiryTimeMillis || "0", 10);
  const isActive = expiryTimeMillis > Date.now();
  // paymentState: 0 = beklemede, 1 = ödendi, 2 = ücretsiz deneme, 3 = ertelenmiş bekleme
  const isCancelled = subscription.cancelReason !== undefined && subscription.cancelReason !== null;

  await db.collection("users").doc(uid).set(
    {
      isPremium: isActive,
      premiumExpiryEpochMillis: expiryTimeMillis,
      subscriptionCancelled: isCancelled,
      lastVerifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      purchaseToken,
    },
    { merge: true }
  );

  return { isActive, expiryTimeMillis };
});

/**
 * Play'den gelen Realtime Developer Notifications (RTDN) - abonelik iptal /
 * yenileme / süre dolumu gibi olayları anlık yakalamak için Pub/Sub tetikleyen
 * bir HTTPS endpoint. Play Console > Monetize > Monetization setup kısmından
 * bir Pub/Sub konusu bağlayıp bu fonksiyonu (veya bir onMessagePublished
 * fonksiyonunu) hedef gösterebilirsiniz. Basit bir iskelet olarak bırakıldı.
 */
exports.playRtdnWebhook = onCall({ secrets: [PLAY_SERVICE_ACCOUNT_KEY] }, async (request) => {
  // TODO: Pub/Sub push mesajını çözümleyip ilgili kullanıcının aboneliğini
  // verifyPurchase ile yeniden doğrulayacak şekilde genişletin.
  logger.info("RTDN alındı", request.data);
  return { received: true };
});

/**
 * 2) GÜNLÜK HAK TÜKETİMİ (sunucu tarafı, transaction ile yarış-durumu güvenli)
 */
exports.consumeCredit = onCall(async (request) => {
  const uid = requireAuth(request);
  const userRef = db.collection("users").doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const doc = await tx.get(userRef);
    const today = todayUtcString();
    const data = doc.exists ? doc.data() : {};

    const isPremium = data.isPremium === true;
    const limit = isPremium ? PREMIUM_DAILY_LIMIT : FREE_DAILY_LIMIT;

    let creditsUsed = data.lastResetDate === today ? (data.creditsUsed || 0) : 0;

    if (!isPremium && creditsUsed >= limit) {
      return { allowed: false, remaining: 0, limit };
    }

    creditsUsed += 1;

    tx.set(
      userRef,
      { creditsUsed, lastResetDate: today, isPremium },
      { merge: true }
    );

    return { allowed: true, remaining: Math.max(0, limit - creditsUsed), limit };
  });

  if (!result.allowed) {
    throw new HttpsError("resource-exhausted", "Günlük hakların bitti.");
  }
  return result;
});

/**
 * 3) ÖDÜLLÜ REKLAM SONRASI BONUS HAK
 *
 * GÜVENLİK NOTU: Bu fonksiyon, istemcinin "reklamı izledim" demesine
 * güveniyor - tek başına spoof edilebilir. Kötüye kullanımı ciddi şekilde
 * azaltmak için AdMob'un Sunucu Tarafı Doğrulama (SSV - Server-Side
 * Verification) özelliğini kullanın: AdMob konsolunda ödüllü reklam
 * biriminize bir SSV callback URL'i tanımlayıp (bu fonksiyonu genel bir
 * HTTPS endpoint - onRequest - olarak da yayınlayıp) Google'ın imzasını
 * doğrulayan ayrı bir fonksiyon eklemeniz önerilir. Bu iskelette basit
 * (imza doğrulamasız) sürüm var; production'a almadan önce güçlendirin.
 */
exports.claimRewardedCredit = onCall(async (request) => {
  const uid = requireAuth(request);
  const userRef = db.collection("users").doc(uid);

  await db.runTransaction(async (tx) => {
    const doc = await tx.get(userRef);
    const data = doc.exists ? doc.data() : {};
    const creditsUsed = Math.max(0, (data.creditsUsed || 0) - 1);
    tx.set(userRef, { creditsUsed }, { merge: true });
  });

  return { success: true };
});

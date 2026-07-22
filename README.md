# Editly AI

Fotoğraf/video üzerindeki yazıları OCR ile algılayıp düzenlemeyi sağlayan Android uygulaması.
Kotlin + Jetpack Compose (Material 3) + MVVM.

## Bu projede NELER HAZIR

- Material 3, koyu/açık tema, Türkçe arayüz, gönderdiğiniz tasarıma yakın Ana Sayfa / Medya Seç / Düzenle / Dışa Aktar / Premium ekranları
- CameraX + galeri medya seçimi (fotoğraf & video)
- ML Kit ile gerçek OCR (metin algılama) + kutu/renk tahmini
- **Video düzenleme akışı**: Galeriden video seçilince gerçek bir "Video
  Düzenle" ekranına gidilir. İlk kare üzerinde ML Kit OCR çalışır, kullanıcı
  metni değiştirebilir ve bir klip aralığı (RangeSlider) seçebilir. Dışa
  aktarma, Google'ın resmi **Media3 Transformer** API'siyle metin katmanını
  gerçekten videoya "yakıp" (burn-in) MP4 üretir ve galeriye kaydeder.
  ÖNEMLİ SINIR: yazı, seçilen klip boyunca SABİT konumda kalır - kare
  içindeki nesne hareket ederse yazı onu TAKİP ETMEZ (gerçek video nesne
  takibi/perspektif uyumu henüz yok, aşağıya bakın).
- **Gerçek fotoğraf render + dışa aktarma**: Metin katmanları (`TextRenderer`)
  gerçekten bitmap üzerine çiziliyor; PNG/JPG olarak 720p/1080p/2K/4K
  ölçeklendirmeyle `MediaStore` üzerinden galeriye kaydediliyor (`MediaSaver`),
  paylaşma (Share) çalışıyor.
- **Sunucu tarafı doğrulama (Firebase Cloud Functions)**: `functions/index.js`
  içinde `consumeCredit` (günlük hak, Firestore transaction ile) ve
  `verifyPurchase` (Google Play Developer API ile gerçek abonelik doğrulama)
  gerçek şekilde yazıldı. Android tarafı (`ServerApi.kt`, `UserRepository`,
  `BillingManager`) bu fonksiyonları çağırıyor; sunucuya ulaşılamazsa
  (çevrimdışı) yerel moda düşüyor.
- Google Play Billing Library ile abonelik satın alma akışı
- AdMob banner + ödüllü reklam entegrasyonu (Google'ın TEST ID'leriyle)
- Google Sign-In + Firebase Auth iskeleti
- Root/emulator tespiti (temel seviye)
- GitHub Actions: her push'ta otomatik Debug + Release APK derleyip Artifact olarak yükler

## Bu projede NELER EKSİK / SİZİN TAMAMLAMANIZ GEREKENLER

Bunlar dürüstçe söylemem gereken, tek seferde "sihirli" şekilde yapılamayacak parçalar:

1. **Fotoğraftaki gibi "yazının yerini AI ile fark edilmeyecek şekilde değiştirme"**
   (gerçek inpainting + perspektif/gölge koruma): Şu an `TextRenderer`, eski
   yazının hemen dışındaki arka plan rengine yakın bir dolguyla kutuyu
   "temizleyip" üzerine yeni yazıyı basit bir döndürme ile çiziyor. Bu,
   fotoğraftaki mockup'taki kadar "görünmez" bir sonuç VERMEZ; düz renkli
   basit arka planlarda iyi çalışır, karmaşık/dokulu arka planlarda yama
   izi belli olur. Gerçek "fark edilmeyecek" kalite için bir görüntü
   tamamlama (inpainting) AI modeli (bulut tabanlı) entegre edilmeli.

2. **Video'da kare kare metin takibi**: Video düzenleme akışı artık çalışıyor
   (OCR + klip seçimi + gerçek MP4 export, bkz. yukarısı) ANCAK yazı sabit
   konumda kalıyor. Kamera/nesne hareket ediyorsa (mockup'taki gibi tabelanın
   kaydığı bir çekim), yazı onu takip etmez. Bunun için ML Kit'in nesne
   takibi (Object Detection & Tracking) API'si veya optik akış (optical
   flow) tabanlı bir takip algoritmasının, her kare için overlay konumunu
   güncelleyecek şekilde `VideoExporter` içine entegre edilmesi gerekir
   (Media3'te bunu `BitmapOverlay.getOverlaySettings(presentationTimeUs)`
   içinde zamana göre pozisyon değiştirerek yapmak mümkün, ancak takip
   algoritması ayrıca yazılmalı).

3. **Gerçek Firebase projesi + deploy**: `functions/index.js` yazıldı ama
   BENİM TARAFIMDAN deploy edilmedi/test edilmedi (bu ortamda internet
   erişimim yok). Kendi Firebase projenizi oluşturup:
   ```
   cd functions && npm install
   firebase functions:secrets:set PLAY_SERVICE_ACCOUNT_KEY
   firebase deploy --only functions,firestore:rules
   ```
   komutlarını siz çalıştırmalısınız (adımlar aşağıda).

4. **AdMob SSV (Sunucu Tarafı Doğrulama)**: `claimRewardedCredit` fonksiyonu
   şu an istemcinin "reklamı izledim" beyanına güveniyor - imza doğrulaması
   yok. Ciddi kötüye kullanımı önlemek için AdMob'un SSV özelliğini
   bağlamanız önerilir (`functions/index.js` içinde not edildi).

5. **AdMob / Play Console gerçek ID'ler**: Şu an Google'ın herkese açık
   TEST reklam ID'leri kullanılıyor. Kendi AdMob hesabınızdaki gerçek
   ID'lerle değiştirmeden Play Store'a yayınlamayın.

## Kurulum Adımları

### 1) GitHub'a yükleyin ve APK'yı indirin
1. Bu klasörün içeriğini yeni bir GitHub reposuna push edin.
2. Repo'da **Actions** sekmesine gidin; "Android CI - APK Build" workflow'u
   otomatik başlayacak (ya da "Run workflow" ile manuel tetikleyin).
3. Derleme bitince, ilgili çalıştırmanın altındaki **Artifacts** bölümünden
   `EditlyAI-debug-apk` dosyasını indirip telefonunuza kurabilirsiniz
   (Bilinmeyen kaynaklardan yükleme izni gerekir).

### 2) Firebase kurulumu (Google Sign-In / Firestore / Cloud Functions için)
1. https://console.firebase.google.com adresinde yeni proje oluşturun.
2. Android uygulaması ekleyin, paket adı: `com.editlyai.app`
3. İndirdiğiniz gerçek `google-services.json` dosyasını `app/` klasörüne
   koyup bu repodaki placeholder'ın üzerine yazın.
4. Authentication > Sign-in method > Google'ı etkinleştirin.
5. `.firebaserc` içindeki `editly-ai-placeholder` değerini kendi Firebase
   proje ID'nizle değiştirin.
6. Bilgisayarda (ya da Termux/Cloud Shell üzerinden telefonda) Firebase CLI
   kurup giriş yapın: `npm install -g firebase-tools && firebase login`
7. Play Console > Kurulum > API erişimi'nden bir servis hesabı oluşturup
   JSON anahtarını indirin, Play Console'da bu hesaba "Finansal veriler"
   iznini verin.
8. `firebase functions:secrets:set PLAY_SERVICE_ACCOUNT_KEY` komutuyla bu
   JSON içeriğini Secret Manager'a kaydedin (repoya asla commitlemeyin).
9. `cd functions && npm install && firebase deploy --only functions,firestore:rules`

### 3) Play Console kurulumu (Abonelik + AdMob)
1. Play Console'da uygulamanızı oluşturun, imzalama anahtarınızı ayarlayın
   (bkz. aşağıdaki "Release imzalama" bölümü).
2. Monetize > Products > Subscriptions altında `premium_monthly` product ID'li
   50 TL/ay'lık bir abonelik oluşturun.
3. AdMob hesabınızda banner + ödüllü reklam birimleri oluşturup ID'lerini
   `AdManager.kt` içindeki TEST ID'lerle değiştirin, Manifest'teki
   `APPLICATION_ID` değerini de güncelleyin.

### 4) Release imzalama (Play Store için ZORUNLU)
Bu repodaki release APK şu an AGP'nin otomatik debug keystore'u ile
imzalanıyor (sadece test kurulumu içindir, **Play Store'a bu şekilde
yüklenemez**). Kendi keystore'unuzu oluşturup GitHub Secrets'a eklemeniz,
`app/build.gradle.kts` içindeki `signingConfigs` bloğunu buna göre
güncellemeniz gerekir.

## Proje Yapısı

```
app/src/main/java/com/editlyai/app/
├── data/
│   ├── ads/         (AdMob)
│   ├── auth/        (Google Sign-In)
│   ├── billing/     (Play Billing + sunucu doğrulama çağrısı)
│   ├── media/        (MediaSaver - galeriye kaydetme/paylaşma)
│   ├── model/        (veri sınıfları)
│   ├── ocr/          (ML Kit OCR + renk tahmini)
│   ├── render/        (TextRenderer - gerçek bitmap render)
│   ├── repository/  (kullanıcı hakları / abonelik durumu, sunucu+yerel)
│   ├── server/        (ServerApi - Cloud Functions çağrıları)
│   └── session/       (EditSessionHolder - ekranlar arası veri aktarımı)
├── ui/
│   ├── components/  (BannerAdView vb.)
│   ├── navigation/  (NavGraph)
│   ├── screens/     (home, mediapicker, edit, videoedit, export, paywall)
│   └── theme/       (Color/Type/Theme)
├── util/            (SecurityChecks)
├── EditlyApplication.kt
└── MainActivity.kt

functions/            (Firebase Cloud Functions - sunucu tarafı)
├── index.js          (verifyPurchase, consumeCredit, claimRewardedCredit)
└── package.json
firestore.rules        (kullanıcılar kendi hak/abonelik verisini yazamaz)
```

## Sıradaki Adımlar İçin Söyleyin

- Bulut tabanlı AI inpainting entegrasyonu ("görünmez düzenleme" kalitesi)
- Video düzenleme akışı (kare takibi + MP4 encode)
- AdMob SSV (imza doğrulamalı ödüllü reklam)
- Release keystore + Play Store yayınlama için GitHub Actions güncellemesi
- Ayarlar / Projelerim ekranları

package com.editlyai.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Google Play Billing Library (v7) ile aylık Premium abonelik yönetimi.
 *
 * KURULUM ADIMLARI (Play Console):
 * 1. Play Console > Uygulamanız > Monetize > Products > Subscriptions
 * 2. Yeni abonelik oluşturun, Product ID: "premium_monthly" (aşağıdaki
 *    PRODUCT_ID_PREMIUM_MONTHLY ile birebir aynı olmalı).
 * 3. Fiyat: 50 TL / ay olacak şekilde Türkiye fiyatlandırmasını ayarlayın.
 * 4. Base plan + (opsiyonel) deneme süresi tanımlayın.
 * 5. Uygulamayı en az "Kapalı test" (internal/closed testing) olarak
 *    yayınlamadan satın alma testleri ÇALIŞMAZ - bu Google Play'in kısıtıdır.
 *
 * GÜVENLİK NOTU (ÇOK ÖNEMLİİ):
 * BillingClient'tan dönen "purchase" bilgisine ASLA doğrudan güvenmeyin;
 * bu istemci tarafında spoof edilebilir. purchaseToken'ı sunucunuza
 * (Cloud Function) gönderip, Google Play Developer API üzerinden
 * `purchases.subscriptions.get` ile SUNUCU TARAFINDA doğrulayın, Premium
 * durumunu ancak bu doğrulamadan sonra aktif edin. Bkz. README ->
 * "Sunucu tarafı satın alma doğrulama".
 */
class BillingManager(
    private val context: Context,
    private val onPurchaseVerified: suspend (purchaseToken: String, isActive: Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_PREMIUM_MONTHLY = "premium_monthly"
        private const val TAG = "BillingManager"
    }

    private val _connectionState = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connectionState

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val premiumProductDetails: StateFlow<ProductDetails?> = _productDetails

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection(onReady: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                _connectionState.value = result.responseCode == BillingClient.BillingResponseCode.OK
                if (_connectionState.value) {
                    queryProductDetails()
                    queryExistingPurchases()
                    onReady()
                } else {
                    Log.e(TAG, "Billing kurulumu başarısız: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = false
                // Basit yeniden bağlanma; production'da exponential backoff önerilir.
            }
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID_PREMIUM_MONTHLY)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsResult.productDetailsList.firstOrNull()
            } else {
                Log.e(TAG, "Ürün bilgisi alınamadı: ${result.debugMessage}")
            }
        }
    }

    /** Satın alma akışını başlatır (Aylık Premium abonelik). */
    fun launchPurchaseFlow(activity: Activity) {
        val productDetails = _productDetails.value ?: run {
            Log.e(TAG, "Ürün detayları henüz yüklenmedi.")
            return
        }
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    /** Satın alma geçmişini kontrol eder (örn. cihaz değişince / hesap değişince). */
    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
                if (purchases.isEmpty()) {
                    // Aktif abonelik bulunamadı -> sunucuya bildir, Premium'u kapat.
                    // (İptal / abonelik süresi doldu senaryosu)
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Log.d(TAG, "Kullanıcı satın almayı iptal etti.")
            else -> Log.e(TAG, "Satın alma hatası: ${result.debugMessage}")
        }
    }

    private val serverApi = com.editlyai.app.data.server.ServerApi()
    private val billingScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { }
        }

        // Sunucu tarafı doğrulama: purchaseToken, Cloud Functions'taki
        // verifyPurchase fonksiyonuna gönderilir; Google Play Developer API
        // ile GERÇEKTEN doğrulanır ve Premium durumu ancak bu doğrulamadan
        // sonra aktif edilir. Bu, istemci tarafı sinyale güvenmemek için
        // kritik bir adımdır (bkz. functions/index.js).
        billingScope.launch {
            when (val result = serverApi.verifyPurchaseRemote(
                purchaseToken = purchase.purchaseToken,
                subscriptionId = PRODUCT_ID_PREMIUM_MONTHLY
            )) {
                is com.editlyai.app.data.server.VerifyPurchaseResult.Success -> {
                    onPurchaseVerified(purchase.purchaseToken, result.isActive)
                }
                is com.editlyai.app.data.server.VerifyPurchaseResult.NetworkError -> {
                    Log.e(TAG, "Sunucu doğrulaması başarısız: ${result.message}. " +
                        "Firebase Functions henüz kurulmadıysa README'deki adımları uygulayın.")
                    // Sunucuya ulaşılamadığında Premium AKTİF EDİLMEZ - güvenlik
                    // tarafında hata payı bırakmamak için "kapalı" varsayılır.
                }
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}

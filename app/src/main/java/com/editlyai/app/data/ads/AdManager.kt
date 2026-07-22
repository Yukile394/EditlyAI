package com.editlyai.app.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob entegrasyonu. Banner reklam Compose tarafında AndroidView ile
 * gösterilir (bkz. ui/components/BannerAdView.kt). Burada sadece SDK
 * başlatma ve ödüllü reklam yönetimi var.
 *
 * TEST ID'LERİ kullanılıyor (Google'ın resmi test reklam birim ID'leri).
 * Play Store'a yayınlamadan önce kendi AdMob hesabınızdaki GERÇEK reklam
 * birimi ID'leri ile değiştirin, aksi halde AdMob politika ihlali /
 * hesap askıya alma riski oluşur.
 */
object AdManager {

    // Google resmi TEST reklam birimi ID'leri - production'da değiştirin!
    const val BANNER_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/9214589741"
    const val REWARDED_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) { isInitialized = true }
    }

    fun loadRewardedAd(context: Context, onLoaded: (Boolean) -> Unit = {}) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID_TEST, request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                onLoaded(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("AdManager", "Ödüllü reklam yüklenemedi: ${error.message}")
                rewardedAd = null
                onLoaded(false)
            }
        })
    }

    /**
     * Ödüllü reklamı gösterir. [onRewardEarned] yalnızca kullanıcı reklamı
     * sonuna kadar izlerse tetiklenir (+1 hak burada verilmelidir).
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                onDismissed()
            }
        }
        ad.show(activity) { onRewardEarned() }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null
}

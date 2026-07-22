package com.editlyai.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.editlyai.app.data.model.UserState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset

private val Context.dataStore by preferencesDataStore(name = "editly_user_prefs")

/**
 * ÖNEMLİ - GÜVENLİK NOTU:
 * Bu repository, hızlı/çevrimdışı UI güncellemesi için yerel (DataStore) bir
 * önbellek tutar. ANCAK gerçek hak/abonelik kontrolü İSTEMCİDE YAPILMAMALI;
 * kötüye kullanımı (root ile değer değiştirme, saat geri alma vb.) önlemek için
 * her "düzenleme" isteğinden önce sunucu tarafında (Cloud Function / backend)
 * doğrulama yapılmalıdır. Bkz. functions/checkAndConsumeCredit (README'de anlatılmıştır).
 * Buradaki isPremium değeri de Google Play satın alma doğrulamasından (Play
 * Developer API, sunucu tarafında) senkronize edilmelidir; sadece
 * BillingClient'tan gelen yerel sinyale güvenilmemelidir.
 */
class UserRepository(private val context: Context) {

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val CREDITS_USED = intPreferencesKey("credits_used")
        val LAST_RESET_DAY = longPreferencesKey("last_reset_epoch_day")
        val PREMIUM_EXPIRY = longPreferencesKey("premium_expiry_millis")
    }

    val userStateFlow: Flow<UserState> = context.dataStore.data.map { prefs ->
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        val lastReset = prefs[Keys.LAST_RESET_DAY] ?: 0L
        val used = if (today != lastReset) 0 else (prefs[Keys.CREDITS_USED] ?: 0)
        UserState(
            isPremium = prefs[Keys.IS_PREMIUM] ?: false,
            dailyCreditsUsed = used,
            dailyCreditsLimit = if (prefs[Keys.IS_PREMIUM] == true) UserState.PREMIUM_DAILY_LIMIT else UserState.FREE_DAILY_LIMIT,
            lastResetEpochDay = lastReset,
            premiumExpiryEpochMillis = prefs[Keys.PREMIUM_EXPIRY] ?: 0L
        )
    }

    /** Yeni gün başladıysa hakları sıfırlar (24 saatlik otomatik yenileme). */
    suspend fun resetDailyCreditsIfNeeded() {
        val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        context.dataStore.edit { prefs ->
            val lastReset = prefs[Keys.LAST_RESET_DAY] ?: 0L
            if (today != lastReset) {
                prefs[Keys.LAST_RESET_DAY] = today
                prefs[Keys.CREDITS_USED] = 0
            }
        }
    }

    private val serverApi = com.editlyai.app.data.server.ServerApi()

    /**
     * Bir AI düzenleme hakkı tüketir. ÖNCE sunucudaki (Cloud Functions)
     * `consumeCredit` fonksiyonunu dener - asıl güvenlik sınırı budur.
     * Sunucuya ulaşılamazsa (çevrimdışı / henüz Firebase kurulmadıysa)
     * yerel DataStore'a düşer; bu, çevrimdışı kullanılabilirlik için bir
     * ödün olduğunu ve tek başına kötüye kullanımı önlemediğini unutmayın.
     */
    suspend fun consumeCredit(): Boolean {
        when (val result = serverApi.consumeCreditRemote()) {
            is com.editlyai.app.data.server.ConsumeCreditResult.Allowed -> {
                syncFromServer(remaining = result.remaining, limit = result.limit)
                return true
            }
            is com.editlyai.app.data.server.ConsumeCreditResult.Denied -> return false
            is com.editlyai.app.data.server.ConsumeCreditResult.NetworkError -> {
                // Sunucuya ulaşılamadı -> yerel (offline) moda düş.
                return consumeCreditLocalFallback()
            }
        }
    }

    private suspend fun syncFromServer(remaining: Int, limit: Int) {
        context.dataStore.edit { prefs ->
            val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
            prefs[Keys.LAST_RESET_DAY] = today
            prefs[Keys.CREDITS_USED] = (limit - remaining).coerceAtLeast(0)
        }
    }

    private suspend fun consumeCreditLocalFallback(): Boolean {
        resetDailyCreditsIfNeeded()
        val current = userStateFlow.first()
        if (!current.hasCreditsLeft) return false
        if (current.isPremium) return true
        context.dataStore.edit { prefs ->
            prefs[Keys.CREDITS_USED] = (prefs[Keys.CREDITS_USED] ?: 0) + 1
        }
        return true
    }

    /** Ödüllü reklam izlendikten sonra +1 bonus hak (günlük sınırın üzerine ekstra). */
    suspend fun addRewardedCredit() {
        val serverSucceeded = serverApi.claimRewardedCreditRemote()
        if (!serverSucceeded) {
            // Çevrimdışı / sunucu hatası -> yerel fallback
            context.dataStore.edit { prefs ->
                val used = (prefs[Keys.CREDITS_USED] ?: 0)
                prefs[Keys.CREDITS_USED] = (used - UserState.REWARD_BONUS_CREDIT).coerceAtLeast(0)
            }
        } else {
            resetDailyCreditsIfNeeded()
        }
    }

    /** Play Billing / sunucu doğrulamasından gelen abonelik durumunu yazar. */
    suspend fun setPremiumStatus(isPremium: Boolean, expiryEpochMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
            prefs[Keys.PREMIUM_EXPIRY] = expiryEpochMillis
        }
    }
}

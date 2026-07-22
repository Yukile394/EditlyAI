package com.editlyai.app.data.server

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

/**
 * Firebase Cloud Functions (functions/index.js) ile konuşan istemci katmanı.
 * Bu, uygulamanın "gerçek" güvenlik sınırıdır: hak tüketimi ve abonelik
 * doğrulaması burada, sunucu tarafında yapılır. Yerel DataStore
 * (UserRepository) sadece çevrimdışı/hızlı-UI önbelleği olarak kalır.
 *
 * Firebase kurulumu tamamlanmadan (gerçek google-services.json + deploy
 * edilmiş functions olmadan) bu çağrılar hata verir; UserRepository bu
 * durumda yerel moda düşer (bkz. UserRepository.consumeCredit).
 */
sealed class ConsumeCreditResult {
    data class Allowed(val remaining: Int, val limit: Int) : ConsumeCreditResult()
    data class Denied(val limit: Int) : ConsumeCreditResult()
    data class NetworkError(val message: String) : ConsumeCreditResult()
}

sealed class VerifyPurchaseResult {
    data class Success(val isActive: Boolean, val expiryTimeMillis: Long) : VerifyPurchaseResult()
    data class NetworkError(val message: String) : VerifyPurchaseResult()
}

class ServerApi {

    private val functions = FirebaseFunctions.getInstance()

    suspend fun consumeCreditRemote(): ConsumeCreditResult {
        return try {
            val result = functions.getHttpsCallable("consumeCredit").call().await()
            val data = result.data as? Map<*, *> ?: return ConsumeCreditResult.NetworkError("Beklenmeyen yanıt")
            val remaining = (data["remaining"] as? Number)?.toInt() ?: 0
            val limit = (data["limit"] as? Number)?.toInt() ?: 0
            ConsumeCreditResult.Allowed(remaining, limit)
        } catch (e: FirebaseFunctionsException) {
            if (e.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) {
                ConsumeCreditResult.Denied(limit = 0)
            } else {
                ConsumeCreditResult.NetworkError(e.message ?: "Sunucu hatası")
            }
        } catch (e: Exception) {
            ConsumeCreditResult.NetworkError(e.message ?: "Bağlantı hatası")
        }
    }

    suspend fun claimRewardedCreditRemote(): Boolean {
        return try {
            functions.getHttpsCallable("claimRewardedCredit").call().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun verifyPurchaseRemote(purchaseToken: String, subscriptionId: String): VerifyPurchaseResult {
        return try {
            val payload = mapOf("purchaseToken" to purchaseToken, "subscriptionId" to subscriptionId)
            val result = functions.getHttpsCallable("verifyPurchase").call(payload).await()
            val data = result.data as? Map<*, *> ?: return VerifyPurchaseResult.NetworkError("Beklenmeyen yanıt")
            val isActive = data["isActive"] as? Boolean ?: false
            val expiry = (data["expiryTimeMillis"] as? Number)?.toLong() ?: 0L
            VerifyPurchaseResult.Success(isActive, expiry)
        } catch (e: Exception) {
            VerifyPurchaseResult.NetworkError(e.message ?: "Doğrulama başarısız")
        }
    }
}

package com.editlyai.app.data.model

/**
 * Kullanıcının anlık hak / abonelik durumunu temsil eder.
 * Bu veri, [com.editlyai.app.data.repository.UserRepository] tarafından
 * DataStore (yerel) + Firestore (sunucu, doğrulama için) üzerinden yönetilir.
 */
data class UserState(
    val isPremium: Boolean = false,
    val dailyCreditsUsed: Int = 0,
    val dailyCreditsLimit: Int = FREE_DAILY_LIMIT,
    val lastResetEpochDay: Long = 0L,
    val premiumExpiryEpochMillis: Long = 0L
) {
    val creditsRemaining: Int
        get() = if (isPremium) Int.MAX_VALUE else (dailyCreditsLimit - dailyCreditsUsed).coerceAtLeast(0)

    val hasCreditsLeft: Boolean
        get() = isPremium || creditsRemaining > 0

    companion object {
        const val FREE_DAILY_LIMIT = 5
        const val PREMIUM_DAILY_LIMIT = 100 // Premium: "sınırsız" olarak sunulur, kötüye kullanım için üst sınır
        const val REWARD_BONUS_CREDIT = 1
    }
}

/** Uygulama içi tek bir düzenleme projesini temsil eder. */
data class EditProject(
    val id: String,
    val sourceUri: String,
    val isVideo: Boolean,
    val createdAtEpochMillis: Long,
    val detectedTextBlocks: List<DetectedTextBlock> = emptyList()
)

/** ML Kit OCR'dan gelen ve düzenlenebilir hale getirilen bir metin bloğu. */
data class DetectedTextBlock(
    val id: String,
    val originalText: String,
    var editedText: String,
    val boundingBox: BoxRect,
    var fontSizeSp: Float,
    var colorArgb: Int,
    var rotationDegrees: Float = 0f,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var fontFamily: String = "Default",
    var alpha: Float = 1f,
    var hasShadow: Boolean = false,
    var hasBorder: Boolean = false
)

data class BoxRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

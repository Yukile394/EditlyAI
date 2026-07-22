package com.editlyai.app.util

import android.os.Build
import java.io.File

/**
 * Basit root/emulator tespiti. Caydırıcıdır, kesin güvenlik garantisi vermez.
 * Gerçek koruma için Play Integrity API kullanılması önerilir (README'de anlatılmıştır).
 */
object SecurityChecks {

    private val ROOT_PATHS = listOf(
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
        "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
        "/su/bin/su"
    )

    fun isProbablyRooted(): Boolean {
        val hasRootFile = ROOT_PATHS.any { File(it).exists() }
        val hasTestKeys = Build.TAGS?.contains("test-keys") == true
        return hasRootFile || hasTestKeys
    }

    fun isProbablyEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")))
    }
}

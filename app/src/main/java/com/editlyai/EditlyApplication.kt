package com.editlyai.app

import android.app.Application
import com.editlyai.app.data.ads.AdManager
import com.editlyai.app.util.SecurityChecks

class EditlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)

        // Güvenlik kontrolleri: root / emulator tespiti sadece loglanır ve
        // (isteğe bağlı) analitik/uyarı amaçlı kullanılır. Bu kontroller tek
        // başına kesin bir engelleme mekanizması DEĞİLDİR (bypass edilebilir);
        // asıl güvenlik sunucu tarafı doğrulamadan gelmelidir.
        if (SecurityChecks.isProbablyRooted()) {
            // TODO: Analitik olayı gönder / isteğe bağlı olarak premium
            // özellikleri kısıtla. Kullanıcıyı uygulamadan tamamen atmak
            // Play politikalarıyla ve kullanıcı deneyimiyle çelişebilir.
        }
    }
}

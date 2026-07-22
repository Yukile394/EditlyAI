package com.editlyai.app.data.session

import android.graphics.Bitmap
import com.editlyai.app.data.model.DetectedTextBlock

/**
 * Düzenleme ekranından dışa aktarma ekranına, kaynak Bitmap + metin
 * katmanlarını taşımak için basit bellek-içi (process-scoped) tutucu.
 * Navigation argümanları ile büyük Bitmap taşımak yerine bu tercih edildi.
 * Not: Uygulama süreci sonlandırılırsa (process death) bu veri kaybolur;
 * production'da bir "taslak" (draft) mekanizmasıyla diske de yazılması
 * önerilir (bkz. README -> Yol Haritası).
 */
object EditSessionHolder {
    var sourceBitmap: Bitmap? = null
    var textBlocks: List<DetectedTextBlock> = emptyList()

    fun set(bitmap: Bitmap, blocks: List<DetectedTextBlock>) {
        sourceBitmap = bitmap
        textBlocks = blocks
    }

    fun clear() {
        sourceBitmap = null
        textBlocks = emptyList()
    }
}

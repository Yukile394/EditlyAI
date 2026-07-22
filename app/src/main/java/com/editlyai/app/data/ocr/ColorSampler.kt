package com.editlyai.app.data.ocr

import android.graphics.Bitmap
import android.graphics.Color
import com.editlyai.app.data.model.BoxRect
import kotlin.math.max
import kotlin.math.min

/**
 * OCR ile bulunan bir metin kutusu içindeki dominant (yazı) rengini
 * basit bir histogram yaklaşımıyla tahmin eder. Bu, "aynı renk korunsun"
 * gereksinimi için ilk yaklaşımdır; production kalitesinde daha gelişmiş
 * bir k-means/segmentasyon modeliyle değiştirilebilir (bkz. README -> Yol Haritası).
 */
object ColorSampler {

    fun estimateTextColor(bitmap: Bitmap, box: BoxRect): Int {
        val left = max(0, box.left.toInt())
        val top = max(0, box.top.toInt())
        val right = min(bitmap.width, box.right.toInt())
        val bottom = min(bitmap.height, box.bottom.toInt())
        if (right <= left || bottom <= top) return Color.WHITE

        val colorCounts = HashMap<Int, Int>()
        val stepX = max(1, (right - left) / 40)
        val stepY = max(1, (bottom - top) / 40)

        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val pixel = bitmap.getPixel(x, y)
                // Yazının genelde en koyu veya en açık uçta olduğu varsayımıyla
                // orta tonları (arkaplan olma ihtimali yüksek) az say.
                val luminance = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel))
                val bucketKey = pixel and 0xFCFCFC // hafif kuantalama
                if (luminance < 60 || luminance > 200) {
                    colorCounts[bucketKey] = (colorCounts[bucketKey] ?: 0) + 1
                }
                x += stepX
            }
            y += stepY
        }

        return colorCounts.maxByOrNull { it.value }?.key?.let { it or 0xFF000000.toInt() } ?: Color.WHITE
    }

    fun estimateBackgroundColor(bitmap: Bitmap, box: BoxRect, marginPx: Int = 12): Int {
        val left = max(0, box.left.toInt() - marginPx)
        val top = max(0, box.top.toInt() - marginPx)
        val right = min(bitmap.width - 1, box.right.toInt() + marginPx)
        val bottom = min(bitmap.height - 1, box.bottom.toInt() + marginPx)
        if (right <= left || bottom <= top) return Color.BLACK
        // Kutunun hemen dışındaki köşe piksellerinin ortalaması
        val samples = listOf(
            bitmap.getPixel(left, top),
            bitmap.getPixel(right, top),
            bitmap.getPixel(left, bottom),
            bitmap.getPixel(right, bottom)
        )
        val r = samples.map { Color.red(it) }.average().toInt()
        val g = samples.map { Color.green(it) }.average().toInt()
        val b = samples.map { Color.blue(it) }.average().toInt()
        return Color.rgb(r, g, b)
    }
}

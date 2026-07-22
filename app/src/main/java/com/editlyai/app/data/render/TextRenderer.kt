package com.editlyai.app.data.render

import android.graphics.*
import android.text.TextPaint
import com.editlyai.app.data.model.DetectedTextBlock

/**
 * OCR/manuel olarak eklenen metin kutularını, kaynak görselin üzerine
 * GERÇEKTEN çizip nihai (dışa aktarılabilir) bir Bitmap üreten render motoru.
 *
 * NOT: Bu, "orijinal yazıyı AI ile silip yerine doğal biçimde yenisini basma"
 * (inpainting) DEĞİLDİR. Burada eski yazının hemen dışındaki arka plan
 * rengine yakın bir dolgu sürülerek kutu temizlenir, üzerine yeni metin
 * çizilir. Fotoğraftaki mockup'takine benzer "görünmez düzenleme" kalitesi
 * için ileride bir inpainting modeli entegre edilmelidir (bkz. README).
 */
object TextRenderer {

    fun renderToBitmap(source: Bitmap, blocks: List<DetectedTextBlock>): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        blocks.forEach { block -> drawTextBlock(canvas, colorSampleSource = output, block = block) }
        return output
    }

    /**
     * VİDEO İÇİN: kaynak video karesiyle aynı boyutta, SAYDAM bir bitmap
     * üretir; içine sadece metin katmanları (+ eski yazıyı kapatma dolgusu)
     * çizilir. Bu bitmap, Media3 OverlayEffect ile videonun üstüne "yakılır"
     * (bkz. VideoExporter). Renk örneklemesi [colorSampleSource] (genelde
     * videodan alınan ilk kare) üzerinden yapılır çünkü saydam katmanın
     * kendisinde arka plan pikseli yoktur.
     */
    fun renderTransparentOverlay(
        width: Int,
        height: Int,
        colorSampleSource: Bitmap,
        blocks: List<DetectedTextBlock>
    ): Bitmap {
        val overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlay)
        blocks.forEach { block -> drawTextBlock(canvas, colorSampleSource = colorSampleSource, block = block) }
        return overlay
    }

    private fun drawTextBlock(canvas: Canvas, colorSampleSource: Bitmap, block: DetectedTextBlock) {
        val box = block.boundingBox
        val boxWidth = box.right - box.left
        val boxHeight = box.bottom - box.top
        if (boxWidth <= 0 || boxHeight <= 0) return

        canvas.save()
        val centerX = box.left + boxWidth / 2f
        val centerY = box.top + boxHeight / 2f
        canvas.rotate(block.rotationDegrees, centerX, centerY)

        // 1) Eski yazının üstünü, hemen dışındaki arka plan rengine yakın bir
        //    dolguyla kapat (basit "temizleme"; gerçek inpainting değildir).
        val bgColor = com.editlyai.app.data.ocr.ColorSampler.estimateBackgroundColor(colorSampleSource, box)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val paddedRect = RectF(box.left - 4, box.top - 4, box.right + 4, box.bottom + 4)
        canvas.drawRoundRect(paddedRect, 6f, 6f, bgPaint)

        // 2) Yeni metni, ayarlanan stil ile çiz.
        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = block.colorArgb
            alpha = (block.alpha * 255).toInt().coerceIn(0, 255)
            textSize = block.fontSizeSp.coerceAtLeast(8f)
            typeface = Typeface.create(
                Typeface.DEFAULT,
                when {
                    block.isBold && block.isItalic -> Typeface.BOLD_ITALIC
                    block.isBold -> Typeface.BOLD
                    block.isItalic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
            )
            if (block.hasShadow) {
                setShadowLayer(6f, 2f, 2f, Color.argb(160, 0, 0, 0))
            }
            textAlign = Paint.Align.LEFT
        }

        if (block.hasBorder) {
            val borderPaint = Paint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = textPaint.textSize * 0.08f
                color = Color.BLACK
                alpha = textPaint.alpha
            }
            drawFittedText(canvas, block.editedText, box.left, box.top, boxWidth, boxHeight, borderPaint)
        }

        drawFittedText(canvas, block.editedText, box.left, box.top, boxWidth, boxHeight, textPaint)
        canvas.restore()
    }

    /** Metni kutuya sığdırmak için gerekirse font boyutunu küçültüp dikey ortalar. */
    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        maxWidth: Float,
        maxHeight: Float,
        basePaint: Paint
    ) {
        if (text.isEmpty()) return
        val paint = Paint(basePaint)
        var textSize = paint.textSize
        val minTextSize = 8f

        while (textSize > minTextSize) {
            paint.textSize = textSize
            val width = paint.measureText(text)
            if (width <= maxWidth) break
            textSize -= 1f
        }
        paint.textSize = textSize.coerceAtLeast(minTextSize)

        val fm = paint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val baselineY = top + (maxHeight - textHeight) / 2f - fm.ascent
        canvas.drawText(text, left, baselineY, paint)
    }
}

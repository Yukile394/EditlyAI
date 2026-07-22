package com.editlyai.app.data.ocr

import android.graphics.Bitmap
import com.editlyai.app.data.model.BoxRect
import com.editlyai.app.data.model.DetectedTextBlock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * ML Kit Latin metin tanıma sarmalayıcısı. Bir görsel üzerindeki yazı
 * bloklarını, yaklaşık font boyutu (kutu yüksekliğinden tahmini) ve konum
 * bilgisiyle birlikte döndürür. Renk/font ailesi gibi görsel özellikler
 * OCR'dan gelmez; bunlar EditViewModel'de piksel örneklemesiyle (ortalama
 * renk) tahmin edilir - bkz. ColorSampler.
 */
class TextRecognizerHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): List<DetectedTextBlock> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()

        val blocks = mutableListOf<DetectedTextBlock>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                val estimatedFontSizeSp = (box.height() * 0.75f)
                blocks += DetectedTextBlock(
                    id = UUID.randomUUID().toString(),
                    originalText = line.text,
                    editedText = line.text,
                    boundingBox = BoxRect(
                        left = box.left.toFloat(),
                        top = box.top.toFloat(),
                        right = box.right.toFloat(),
                        bottom = box.bottom.toFloat()
                    ),
                    fontSizeSp = estimatedFontSizeSp,
                    colorArgb = android.graphics.Color.WHITE, // ColorSampler ile güncellenir
                    rotationDegrees = line.angle
                )
            }
        }
        return blocks
    }

    fun close() = recognizer.close()
}

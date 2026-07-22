package com.editlyai.app.data.render

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.editlyai.app.data.model.DetectedTextBlock
import com.google.common.collect.ImmutableList
import java.io.File

/**
 * Jetpack Media3 Transformer (Google'ın resmi video düzenleme API'si) ile
 * videoya metin katmanı "yakan" (burn-in) dışa aktarma motoru.
 *
 * KAPSAM VE DÜRÜST SINIRLAR:
 * - Metin overlay'i seçilen zaman aralığı boyunca SABİT konumda kalır.
 *   Kare içindeki nesne/yazı hareket ediyorsa yazı onu TAKİP ETMEZ - bu,
 *   gerçek bir video nesne takibi (object tracking) gerektirir ve henüz
 *   eklenmedi (bkz. README -> Yol Haritası).
 * - Eski yazının üstü, ilk kareden örneklenen arka plan rengine yakın bir
 *   dolguyla kapatılır (fotoğraftaki TextRenderer ile aynı basit yaklaşım);
 *   kamera veya nesne hareket ederse bu maskeleme kayabilir. Statik çekimler
 *   (sabit kamera, sabit tabela vb.) için makul sonuç verir.
 * - Ses akışı olduğu gibi (efektsiz) korunur.
 */
class VideoExporter(private val context: Context) {

    fun export(
        sourceUri: Uri,
        firstFrame: Bitmap,
        videoWidthPx: Int,
        videoHeightPx: Int,
        textBlocks: List<DetectedTextBlock>,
        clipStartMs: Long,
        clipEndMs: Long,
        onSuccess: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (videoWidthPx <= 0 || videoHeightPx <= 0) {
            onError(IllegalStateException("Geçersiz video boyutu"))
            return
        }

        val overlayBitmap = TextRenderer.renderTransparentOverlay(
            width = videoWidthPx,
            height = videoHeightPx,
            colorSampleSource = firstFrame,
            blocks = textBlocks
        )

        // Metin katmanı, klip boyunca sabit (statik) bir bitmap olarak
        // döndürülür - bkz. sınıf üstü açıklama.
        val staticTextOverlay = object : BitmapOverlay() {
            override fun getBitmap(presentationTimeUs: Long): Bitmap = overlayBitmap
        }

        val overlayEffect = OverlayEffect(ImmutableList.of<TextureOverlay>(staticTextOverlay))

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartMs)
                    .setEndPositionMs(clipEndMs.coerceAtLeast(clipStartMs + 100))
                    .build()
            )
            .build()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), listOf(overlayEffect)))
            .build()

        val outputFile = File(context.cacheDir, "editly_export_${System.currentTimeMillis()}.mp4")

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onSuccess(outputFile)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    onError(exportException)
                }
            })
            .build()

        // NOT: Transformer.start çağrısının, Transformer'ı oluşturan thread'in
        // Looper'ında (genelde ana thread) yapılması gerekir.
        transformer.start(editedMediaItem, outputFile.absolutePath)
    }
}

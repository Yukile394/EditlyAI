package com.editlyai.app.data.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.OutputStream

enum class ExportQualityPreset(val longEdgePx: Int, val label: String) {
    P720(1280, "720p"),
    P1080(1920, "1080p"),
    Q2K(2560, "2K"),
    Q4K(3840, "4K")
}

enum class ExportImageFormat(val mimeType: String, val extension: String) {
    PNG("image/png", "png"),
    JPG("image/jpeg", "jpg")
}

/**
 * Bitmap'i seçilen kalite/format ile MediaStore üzerinden cihazın
 * "Pictures/Editly" klasörüne kaydeder (Android 10+ scoped storage uyumlu,
 * ekstra depolama izni gerekmez). minSdk=29 olduğu için sadece MediaStore
 * API kullanılıyor, eski dosya-yolu tabanlı yöntemlere gerek yok.
 */
object MediaSaver {

    fun scaleToQuality(bitmap: Bitmap, preset: ExportQualityPreset): Bitmap {
        val longEdge = maxOf(bitmap.width, bitmap.height)
        if (longEdge == preset.longEdgePx) return bitmap
        val scale = preset.longEdgePx.toFloat() / longEdge
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /** @return kaydedilen dosyanın content:// URI'si, veya hata olursa null. */
    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        format: ExportImageFormat,
        displayNameWithoutExtension: String = "editly_${System.currentTimeMillis()}"
    ): Uri? {
        val resolver = context.contentResolver
        val fileName = "$displayNameWithoutExtension.${format.extension}"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Editly")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out: OutputStream ->
                val compressFormat = if (format == ExportImageFormat.PNG) {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, 95, out)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return uri
    }

    fun buildShareIntent(context: Context, uri: Uri, mimeType: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Paylaş")
    }

    /**
     * Media3 Transformer'ın ürettiği yerel (cache) MP4 dosyasını, MediaStore
     * üzerinden "Movies/Editly" klasörüne kopyalayarak galeriye kaydeder.
     */
    fun saveVideoToGallery(
        context: Context,
        sourceFile: java.io.File,
        displayNameWithoutExtension: String = "editly_${System.currentTimeMillis()}"
    ): Uri? {
        val resolver = context.contentResolver
        val fileName = "$displayNameWithoutExtension.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Editly")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }

        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return uri
    }
}

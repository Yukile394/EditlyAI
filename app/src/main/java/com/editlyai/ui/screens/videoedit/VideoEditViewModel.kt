package com.editlyai.app.ui.screens.videoedit

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editlyai.app.data.model.DetectedTextBlock
import com.editlyai.app.data.ocr.ColorSampler
import com.editlyai.app.data.ocr.TextRecognizerHelper
import com.editlyai.app.data.render.VideoExporter
import com.editlyai.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class VideoEditUiState(
    val isLoading: Boolean = true,
    val previewFrame: Bitmap? = null,
    val textBlocks: List<DetectedTextBlock> = emptyList(),
    val selectedBlockId: String? = null,
    val durationMs: Long = 0L,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val creditBlocked: Boolean = false,
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val exportedFile: File? = null
)

private data class VideoMeta(val frame: Bitmap, val durationMs: Long, val width: Int, val height: Int)

class VideoEditViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val textRecognizer = TextRecognizerHelper()
    private val videoExporter = VideoExporter(application)

    private val _uiState = MutableStateFlow(VideoEditUiState())
    val uiState: StateFlow<VideoEditUiState> = _uiState

    private var sourceUri: Uri? = null

    fun loadVideo(uriString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Video düzenleme de bir AI oturumu (OCR) tükettiği için hak kullanır.
            val allowed = userRepository.consumeCredit()
            if (!allowed) {
                _uiState.value = _uiState.value.copy(isLoading = false, creditBlocked = true)
                return@launch
            }

            try {
                val uri = Uri.parse(uriString)
                sourceUri = uri
                val meta = withContext(Dispatchers.IO) { extractFirstFrameAndMeta(uri) }
                val blocks = withContext(Dispatchers.Default) {
                    textRecognizer.recognize(meta.frame).map { block ->
                        block.copy(colorArgb = ColorSampler.estimateTextColor(meta.frame, block.boundingBox))
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    previewFrame = meta.frame,
                    textBlocks = blocks,
                    durationMs = meta.durationMs,
                    clipStartMs = 0L,
                    clipEndMs = meta.durationMs,
                    videoWidth = meta.width,
                    videoHeight = meta.height
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Video yüklenemedi")
            }
        }
    }

    private fun extractFirstFrameAndMeta(uri: Uri): VideoMeta {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(getApplication(), uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val frame = retriever.getFrameAtTime(0) ?: throw IllegalStateException("Video karesi okunamadı")
            // Bazı cihazlarda getFrameAtTime dönüş açısını otomatik uygular,
            // bazılarında uygulamaz; genişlik/yükseklik dönüş açısına göre
            // frame boyutuna göre düzeltilir (frame kaynağı esas alınır).
            val width = if (rotation == 90 || rotation == 270) rawHeight else rawWidth
            val height = if (rotation == 90 || rotation == 270) rawWidth else rawHeight
            val finalWidth = if (width > 0) width else frame.width
            val finalHeight = if (height > 0) height else frame.height
            return VideoMeta(frame, durationMs, finalWidth, finalHeight)
        } finally {
            retriever.release()
        }
    }

    fun selectBlock(id: String) {
        _uiState.value = _uiState.value.copy(selectedBlockId = id)
    }

    fun updateSelectedBlockText(newText: String) {
        val state = _uiState.value
        val updated = state.textBlocks.map {
            if (it.id == state.selectedBlockId) it.copy(editedText = newText) else it
        }
        _uiState.value = state.copy(textBlocks = updated)
    }

    fun updateClipRange(startMs: Long, endMs: Long) {
        _uiState.value = _uiState.value.copy(
            clipStartMs = startMs.coerceIn(0, _uiState.value.durationMs),
            clipEndMs = endMs.coerceIn(startMs + 100, _uiState.value.durationMs.coerceAtLeast(startMs + 100))
        )
    }

    /** Not: Transformer.start ana thread Looper'ında çağrılmalı - viewModelScope.launch KULLANILMAZ. */
    fun export() {
        val state = _uiState.value
        val frame = state.previewFrame
        val uri = sourceUri
        if (frame == null || uri == null) return

        _uiState.value = state.copy(isExporting = true, errorMessage = null, exportedFile = null)

        videoExporter.export(
            sourceUri = uri,
            firstFrame = frame,
            videoWidthPx = state.videoWidth,
            videoHeightPx = state.videoHeight,
            textBlocks = state.textBlocks,
            clipStartMs = state.clipStartMs,
            clipEndMs = state.clipEndMs,
            onSuccess = { file ->
                _uiState.value = _uiState.value.copy(isExporting = false, exportedFile = file)
            },
            onError = { e ->
                _uiState.value = _uiState.value.copy(isExporting = false, errorMessage = e.message ?: "Dışa aktarma başarısız oldu")
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}

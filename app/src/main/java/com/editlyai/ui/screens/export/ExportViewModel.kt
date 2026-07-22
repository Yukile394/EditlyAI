package com.editlyai.app.ui.screens.export

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editlyai.app.data.media.ExportImageFormat
import com.editlyai.app.data.media.ExportQualityPreset
import com.editlyai.app.data.media.MediaSaver
import com.editlyai.app.data.render.TextRenderer
import com.editlyai.app.data.session.EditSessionHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExportUiState(
    val previewBitmap: Bitmap? = null,
    val isExporting: Boolean = false,
    val savedUri: Uri? = null,
    val errorMessage: String? = null
)

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState

    init {
        // Edit ekranında "Kaydet" ile üretilen render'ı önizleme olarak göster.
        _uiState.value = _uiState.value.copy(previewBitmap = EditSessionHolder.sourceBitmap?.let {
            TextRenderer.renderToBitmap(it, EditSessionHolder.textBlocks)
        })
    }

    fun export(format: ExportImageFormat, quality: ExportQualityPreset, saveToGallery: Boolean) {
        val rendered = _uiState.value.previewBitmap
        if (rendered == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Kaydedilecek bir görsel bulunamadı.")
            return
        }
        if (!saveToGallery) {
            _uiState.value = _uiState.value.copy(errorMessage = null, savedUri = null)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            val uri = withContext(Dispatchers.Default) {
                val scaled = MediaSaver.scaleToQuality(rendered, quality)
                MediaSaver.saveToGallery(getApplication(), scaled, format)
            }
            _uiState.value = if (uri != null) {
                _uiState.value.copy(isExporting = false, savedUri = uri)
            } else {
                _uiState.value.copy(isExporting = false, errorMessage = "Kaydetme başarısız oldu.")
            }
        }
    }
}

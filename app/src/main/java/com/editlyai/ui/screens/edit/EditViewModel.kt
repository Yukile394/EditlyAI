package com.editlyai.app.ui.screens.edit

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editlyai.app.data.model.DetectedTextBlock
import com.editlyai.app.data.ocr.ColorSampler
import com.editlyai.app.data.ocr.TextRecognizerHelper
import com.editlyai.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditUiState(
    val isLoading: Boolean = true,
    val bitmap: Bitmap? = null,
    val textBlocks: List<DetectedTextBlock> = emptyList(),
    val selectedBlockId: String? = null,
    val errorMessage: String? = null,
    val creditBlocked: Boolean = false
)

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val textRecognizer = TextRecognizerHelper()

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState

    fun loadMedia(uriString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Her yeni AI düzenleme oturumu bir hak tüketir.
            val allowed = userRepository.consumeCredit()
            if (!allowed) {
                _uiState.value = _uiState.value.copy(isLoading = false, creditBlocked = true)
                return@launch
            }

            try {
                val bitmap = withContext(Dispatchers.IO) { loadBitmap(uriString) }
                val blocks = withContext(Dispatchers.Default) {
                    val detected = textRecognizer.recognize(bitmap)
                    detected.map { block ->
                        block.copy(colorArgb = ColorSampler.estimateTextColor(bitmap, block.boundingBox))
                    }
                }
                _uiState.value = _uiState.value.copy(isLoading = false, bitmap = bitmap, textBlocks = blocks)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Görsel yüklenemedi")
            }
        }
    }

    private fun loadBitmap(uriString: String): Bitmap {
        val uri = Uri.parse(uriString)
        val context = getApplication<Application>()
        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                // KRİTİK: ImageDecoder varsayılan olarak HARDWARE config bitmap
                // döndürür; bu tip üzerinde getPixel()/copy() ile piksel
                // erişimi ÇALIŞMAZ (ColorSampler ve TextRenderer'ın ihtiyacı
                // olan şey tam da bu). isMutableRequired = true, decoder'ı
                // yazılım (software) bellek ayırmaya zorlar.
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
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

    fun addManualTextBlock() {
        val bitmap = _uiState.value.bitmap ?: return
        val newBlock = DetectedTextBlock(
            id = java.util.UUID.randomUUID().toString(),
            originalText = "",
            editedText = "Yeni Metin",
            boundingBox = com.editlyai.app.data.model.BoxRect(
                left = bitmap.width * 0.25f,
                top = bitmap.height * 0.45f,
                right = bitmap.width * 0.75f,
                bottom = bitmap.height * 0.55f
            ),
            fontSizeSp = 24f,
            colorArgb = android.graphics.Color.WHITE
        )
        _uiState.value = _uiState.value.copy(
            textBlocks = _uiState.value.textBlocks + newBlock,
            selectedBlockId = newBlock.id
        )
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}

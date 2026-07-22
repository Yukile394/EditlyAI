package com.editlyai.app.ui.screens.videoedit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editlyai.app.data.media.MediaSaver
import com.editlyai.app.ui.theme.EditlyPurple

private enum class VideoEditTab { METIN, STIL, RENK, KONUM, EFEKT }

@Composable
fun VideoEditScreen(
    mediaUri: String,
    onBack: () -> Unit,
    onNeedPremium: () -> Unit,
    viewModel: VideoEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(VideoEditTab.METIN) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var savedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(mediaUri) { viewModel.loadVideo(mediaUri) }

    if (uiState.creditBlocked) {
        LaunchedEffect(Unit) { onNeedPremium() }
        return
    }

    // Export tamamlanınca otomatik galeriye kaydet (tek seferlik).
    LaunchedEffect(uiState.exportedFile) {
        val file = uiState.exportedFile ?: return@LaunchedEffect
        val uri = MediaSaver.saveVideoToGallery(context, file)
        savedVideoUri = uri
        savedMessage = if (uri != null) "Video galeriye kaydedildi" else "Kaydetme başarısız oldu"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Video Düzenle") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            },
            actions = {
                Button(
                    onClick = { viewModel.export() },
                    enabled = !uiState.isExporting && uiState.previewFrame != null,
                    colors = ButtonDefaults.buttonColors(containerColor = EditlyPurple),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Dışa Aktar")
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EditlyPurple)
                        Spacer(Modifier.height(8.dp))
                        Text("Video karesi analiz ediliyor…")
                    }
                }
                uiState.errorMessage != null -> Text(uiState.errorMessage ?: "")
                uiState.previewFrame != null -> {
                    val frame = uiState.previewFrame!!
                    val imageBitmap = remember(frame) { frame.asImageBitmap() }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scale = minOf(size.width / frame.width, size.height / frame.height)
                        val drawWidth = frame.width * scale
                        val drawHeight = frame.height * scale
                        val offsetX = (size.width - drawWidth) / 2
                        val offsetY = (size.height - drawHeight) / 2

                        drawImage(
                            image = imageBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt())
                        )

                        uiState.textBlocks.forEach { block ->
                            val left = offsetX + block.boundingBox.left * scale
                            val top = offsetY + block.boundingBox.top * scale
                            val right = offsetX + block.boundingBox.right * scale
                            val bottom = offsetY + block.boundingBox.bottom * scale
                            val isSelected = block.id == uiState.selectedBlockId
                            drawRect(
                                color = if (isSelected) EditlyPurple else Color.White.copy(alpha = 0.6f),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = Stroke(width = if (isSelected) 3f else 1.5f)
                            )
                        }
                    }
                }
            }
        }

        savedMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        // Basitleştirilmiş zaman aralığı (klip) seçici - mockup'taki
        // timeline'ın yerini tutar. Tam kare-kare scrubbing/thumbnail şeridi
        // bir sonraki adımda eklenebilir.
        if (uiState.durationMs > 0) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Klip aralığı: ${formatMs(uiState.clipStartMs)} - ${formatMs(uiState.clipEndMs)} / ${formatMs(uiState.durationMs)}",
                    style = MaterialTheme.typography.labelMedium
                )
                RangeSlider(
                    value = uiState.clipStartMs.toFloat()..uiState.clipEndMs.toFloat(),
                    onValueChange = { range ->
                        viewModel.updateClipRange(range.start.toLong(), range.endInclusive.toLong())
                    },
                    valueRange = 0f..uiState.durationMs.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = EditlyPurple, activeTrackColor = EditlyPurple)
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(selected = selectedTab == VideoEditTab.METIN, onClick = { selectedTab = VideoEditTab.METIN }, text = { Text("Metin") })
            Tab(selected = selectedTab == VideoEditTab.STIL, onClick = { selectedTab = VideoEditTab.STIL }, text = { Text("Stil") })
            Tab(selected = selectedTab == VideoEditTab.RENK, onClick = { selectedTab = VideoEditTab.RENK }, text = { Text("Renk") })
            Tab(selected = selectedTab == VideoEditTab.KONUM, onClick = { selectedTab = VideoEditTab.KONUM }, text = { Text("Konum") })
            Tab(selected = selectedTab == VideoEditTab.EFEKT, onClick = { selectedTab = VideoEditTab.EFEKT }, text = { Text("Efekt") })
        }

        val selectedBlock = uiState.textBlocks.find { it.id == uiState.selectedBlockId }
            ?: uiState.textBlocks.firstOrNull()

        Column(modifier = Modifier.padding(16.dp)) {
            when (selectedTab) {
                VideoEditTab.METIN -> {
                    if (selectedBlock != null) {
                        LaunchedEffect(selectedBlock.id) {
                            if (uiState.selectedBlockId == null) viewModel.selectBlock(selectedBlock.id)
                        }
                        var text by remember(selectedBlock.id) { mutableStateOf(selectedBlock.editedText) }
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                viewModel.updateSelectedBlockText(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Metin") }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Not: Yazı, seçilen klip aralığı boyunca sabit konumda kalır (hareket takibi yol haritasında).",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Bu karede yazı bulunamadı.")
                    }
                }
                VideoEditTab.STIL -> Text("Font, kalınlık ve italik ayarları burada gösterilecek. (Yol haritası)")
                VideoEditTab.RENK -> Text("Renk seçici burada gösterilecek. (Yol haritası)")
                VideoEditTab.KONUM -> Text("Sürükle-bırak konum kontrolleri burada gösterilecek. (Yol haritası)")
                VideoEditTab.EFEKT -> Text("Gölge, kenarlık ve saydamlık kontrolleri burada gösterilecek. (Yol haritası)")
            }
        }

        if (savedVideoUri != null) {
            OutlinedButton(
                onClick = {
                    context.startActivity(MediaSaver.buildShareIntent(context, savedVideoUri!!, "video/mp4"))
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Paylaş")
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

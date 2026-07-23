package com.editlyai.app.ui.screens.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editlyai.app.ui.theme.EditlyPurple

private enum class EditTab { METIN, STIL, RENK, KONUM, EFEKT }

@Composable
fun EditScreen(
    mediaUri: String,
    isVideo: Boolean,
    onBack: () -> Unit,
    onNeedPremium: () -> Unit,
    onExport: (String) -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(EditTab.METIN) }

    LaunchedEffect(mediaUri) { viewModel.loadMedia(mediaUri) }

    if (uiState.creditBlocked) {
        LaunchedEffect(Unit) { onNeedPremium() }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Düzenle") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            },
            actions = {
                IconButton(onClick = { /* TODO: undo */ }) { Icon(Icons.Default.Undo, contentDescription = null) }
                IconButton(onClick = { /* TODO: redo */ }) { Icon(Icons.Default.Redo, contentDescription = null) }
                Button(
                    onClick = {
                        val bmp = uiState.bitmap
                        if (bmp != null) {
                            com.editlyai.app.data.session.EditSessionHolder.set(bmp, uiState.textBlocks)
                            onExport(mediaUri)
                        }
                    },
                    enabled = uiState.bitmap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = EditlyPurple),
                    modifier = Modifier.padding(end = 8.dp)
                ) { Text("Kaydet") }
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
                        Text("Yazılar algılanıyor…")
                    }
                }
                uiState.errorMessage != null -> Text(uiState.errorMessage ?: "")
                uiState.bitmap != null -> {
                    val bmp = uiState.bitmap!!
                    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(bmp, uiState.textBlocks) {
                                detectTapGestures { tapOffset ->
                                    if (canvasSize.width == 0 || canvasSize.height == 0) return@detectTapGestures
                                    val scale = minOf(
                                        canvasSize.width / bmp.width.toFloat(),
                                        canvasSize.height / bmp.height.toFloat()
                                    )
                                    val drawWidth = bmp.width * scale
                                    val drawHeight = bmp.height * scale
                                    val offsetX = (canvasSize.width - drawWidth) / 2
                                    val offsetY = (canvasSize.height - drawHeight) / 2
                                    // Üstteki (son çizilen) kutu önce eşleşsin diye tersten ara.
                                    val tapped = uiState.textBlocks.lastOrNull { block ->
                                        val left = offsetX + block.boundingBox.left * scale
                                        val top = offsetY + block.boundingBox.top * scale
                                        val right = offsetX + block.boundingBox.right * scale
                                        val bottom = offsetY + block.boundingBox.bottom * scale
                                        tapOffset.x in left..right && tapOffset.y in top..bottom
                                    }
                                    if (tapped != null) {
                                        viewModel.selectBlock(tapped.id)
                                        selectedTab = EditTab.METIN
                                    }
                                }
                            }
                    ) {
                        val scale = minOf(size.width / bmp.width, size.height / bmp.height)
                        val drawWidth = bmp.width * scale
                        val drawHeight = bmp.height * scale
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

        if (uiState.textBlocks.isEmpty() && !uiState.isLoading && uiState.bitmap != null) {
            TextButton(onClick = { viewModel.addManualTextBlock() }, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)) {
                Text("Görselde yazı bulunamadı — Metin Kutusu Ekle")
            }
        }

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(selected = selectedTab == EditTab.METIN, onClick = { selectedTab = EditTab.METIN }, text = { Text("Metin") })
            Tab(selected = selectedTab == EditTab.STIL, onClick = { selectedTab = EditTab.STIL }, text = { Text("Stil") })
            Tab(selected = selectedTab == EditTab.RENK, onClick = { selectedTab = EditTab.RENK }, text = { Text("Renk") })
            Tab(selected = selectedTab == EditTab.KONUM, onClick = { selectedTab = EditTab.KONUM }, text = { Text("Konum") })
            Tab(selected = selectedTab == EditTab.EFEKT, onClick = { selectedTab = EditTab.EFEKT }, text = { Text("Efekt") })
        }

        val selectedBlock = uiState.textBlocks.find { it.id == uiState.selectedBlockId }
            ?: uiState.textBlocks.firstOrNull()

        Column(modifier = Modifier.padding(16.dp)) {
            when (selectedTab) {
                EditTab.METIN -> {
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
                    } else {
                        Text("Düzenlemek için bir metin kutusu seçin veya ekleyin.")
                    }
                }
                EditTab.STIL -> Text("Font, kalınlık ve italik ayarları burada gösterilecek. (Yol haritası)")
                EditTab.RENK -> Text("Renk seçici burada gösterilecek. Algılanan renk otomatik önerilir. (Yol haritası)")
                EditTab.KONUM -> Text("Sürükle-bırak konum ve döndürme kontrolleri burada gösterilecek. (Yol haritası)")
                EditTab.EFEKT -> Text("Gölge, kenarlık ve saydamlık kontrolleri burada gösterilecek. (Yol haritası)")
            }
        }
    }
}

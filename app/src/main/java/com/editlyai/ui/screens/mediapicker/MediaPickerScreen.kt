package com.editlyai.app.ui.screens.mediapicker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private enum class MediaTab { ALL, PHOTO, VIDEO }

/**
 * Basitleştirilmiş medya seçici: galeriden çoklu seçim (PickMultipleMedia)
 * ve kameradan tekli çekim aksiyonlarını sunar. Gerçek "cihazdaki tüm
 * medyaları liste halinde gösteren galeri ızgarası" MediaStore sorgusu
 * gerektirir (bkz. README -> Yol Haritası, sonraki adım olarak eklenebilir).
 */
@Composable
fun MediaPickerScreen(
    onMediaSelected: (uri: String, isVideo: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MediaTab.ALL) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onMediaSelected(it.toString(), false) } }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onMediaSelected(it.toString(), true) } }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Medya Seç") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }
        )

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(selected = selectedTab == MediaTab.ALL, onClick = { selectedTab = MediaTab.ALL }, text = { Text("Tümü") })
            Tab(selected = selectedTab == MediaTab.PHOTO, onClick = { selectedTab = MediaTab.PHOTO }, text = { Text("Fotoğraf") })
            Tab(selected = selectedTab == MediaTab.VIDEO, onClick = { selectedTab = MediaTab.VIDEO }, text = { Text("Video") })
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    photoPicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Galeriden Fotoğraf Seç") }

            Button(
                onClick = {
                    videoPicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Galeriden Video Seç") }

            Text(
                "İpucu: Kamera ile çekim için Ana Sayfa'daki hızlı işlem kartlarını kullanabilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

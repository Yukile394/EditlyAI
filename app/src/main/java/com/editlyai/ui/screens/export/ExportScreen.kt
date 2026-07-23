package com.editlyai.app.ui.screens.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editlyai.app.data.media.ExportImageFormat
import com.editlyai.app.data.media.ExportQualityPreset
import com.editlyai.app.data.media.MediaSaver
import com.editlyai.app.ui.theme.EditlyPurple

@Composable
fun ExportScreen(
    mediaUri: String,
    onBack: () -> Unit,
    viewModel: ExportViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var format by remember { mutableStateOf(ExportImageFormat.PNG) }
    var quality by remember { mutableStateOf(ExportQualityPreset.P1080) }
    var saveToGallery by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Dışa Aktar") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }
        )

        Column(modifier = Modifier.padding(20.dp).weight(1f)) {
            uiState.previewBitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, EditlyPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Önizleme",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Format", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportImageFormat.entries.forEach { f ->
                    FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.name) })
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Kalite", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportQualityPreset.entries.forEach { q ->
                    FilterChip(selected = quality == q, onClick = { quality = q }, label = { Text(q.label) })
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Galeriye Kaydet")
                Switch(checked = saveToGallery, onCheckedChange = { saveToGallery = it })
            }

            uiState.errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            uiState.savedUri?.let {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EditlyPurple)
                    Spacer(Modifier.width(8.dp))
                    Text("Başarıyla kaydedildi", color = EditlyPurple, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Button(
                onClick = { viewModel.export(format, quality, saveToGallery) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EditlyPurple),
                enabled = !uiState.isExporting && uiState.previewBitmap != null
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
                } else {
                    Text("Dışa Aktar")
                }
            }

            if (uiState.savedUri != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val uri = uiState.savedUri ?: return@OutlinedButton
                        context.startActivity(MediaSaver.buildShareIntent(context, uri, format.mimeType))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Paylaş")
                }
            }
        }
    }
}

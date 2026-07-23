package com.editlyai.app.ui.screens.projects

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.editlyai.app.ui.theme.EditlyPurple

/**
 * NOT: Bu ekran şu an sadece bir yer tutucudur (dürüstçe belirtilmeli).
 * Geçmiş düzenlemelerin diske kaydedilip burada listelenmesi henüz
 * eklenmedi (bkz. README -> Yol Haritası: "taslak/proje" mekanizması).
 */
@Composable
fun ProjectsScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Projelerim") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.FolderOff, contentDescription = null, tint = EditlyPurple, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Henüz kaydedilmiş proje yok", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Bu özellik yakında eklenecek: düzenlemelerinizi taslak olarak kaydedip buradan devam edebileceksiniz.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

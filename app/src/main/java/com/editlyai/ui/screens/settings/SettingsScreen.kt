package com.editlyai.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editlyai.app.data.repository.UserRepository
import com.editlyai.app.ui.theme.EditlyPurple

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = androidx.compose.runtime.remember { UserRepository(context) }
    val userState by userRepository.userStateFlow.collectAsState(initial = com.editlyai.app.data.model.UserState())

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ayarlar") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            if (!userState.isPremium) {
                SettingsRow(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Reklamları Kaldır / Premium'a Geç",
                    subtitle = "Sınırsız düzenleme, reklamsız kullanım",
                    onClick = onNavigateToPaywall
                )
            } else {
                Text(
                    "Premium aktif — reklamsız kullanıyorsunuz",
                    color = EditlyPurple,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            SettingsRow(title = "Google ile Giriş", subtitle = "Yakında", onClick = { })
            SettingsRow(title = "Gizlilik Politikası", subtitle = "Yakında", onClick = { })
            SettingsRow(title = "Kullanım Koşulları", subtitle = "Yakında", onClick = { })
            SettingsRow(title = "Sürüm", subtitle = "1.0.0", onClick = null)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = EditlyPurple)
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

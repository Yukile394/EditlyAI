package com.editlyai.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editlyai.app.data.model.UserState
import com.editlyai.app.ui.components.BannerAdView
import com.editlyai.app.ui.theme.EditlyPurple

@Composable
fun HomeScreen(
    onNavigateToPicker: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val userState by viewModel.userState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ana Sayfa") },
            actions = {
                IconButton(onClick = { /* TODO: Ayarlar ekranı */ }) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            CreditsBadge(userState = userState)

            Spacer(Modifier.height(16.dp))
            UploadCard(onClick = onNavigateToPicker)

            Spacer(Modifier.height(24.dp))
            Text("Hızlı İşlemler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(Icons.Default.PhotoLibrary, "Galeriden Seç", Modifier.weight(1f), onNavigateToPicker)
                QuickActionCard(Icons.Default.PhotoCamera, "Kameradan Fotoğraf", Modifier.weight(1f), onNavigateToPicker)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(Icons.Default.VideoLibrary, "Galeriden Video", Modifier.weight(1f), onNavigateToPicker)
                QuickActionCard(Icons.Default.Videocam, "Kameradan Video", Modifier.weight(1f), onNavigateToPicker)
            }

            if (!userState.isPremium && !userState.hasCreditsLeft) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onNavigateToPaywall,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EditlyPurple)
                ) {
                    Text("Premium'a Geç")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Ücretsiz kullanıcılarda banner reklam (Premium'da gösterilmez)
        if (!userState.isPremium) {
            BannerAdView(modifier = Modifier.padding(bottom = 4.dp))
        }

        BottomNavBar()
    }
}

@Composable
private fun CreditsBadge(userState: UserState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        if (userState.isPremium) {
            Text("Sınırsız (Premium)", color = EditlyPurple, fontWeight = FontWeight.Bold)
        } else {
            Text(
                "Kalan hakkın: ${userState.creditsRemaining} / ${userState.dailyCreditsLimit}",
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Haklar her gün yenilenir",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UploadCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, EditlyPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.FileUpload, contentDescription = null, tint = EditlyPurple, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text("Fotoğraf veya video yükle", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = EditlyPurple)) {
            Text("Seç")
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = EditlyPurple)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BottomNavBar() {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Ana Sayfa") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Projelerim") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Ayarlar") })
    }
}

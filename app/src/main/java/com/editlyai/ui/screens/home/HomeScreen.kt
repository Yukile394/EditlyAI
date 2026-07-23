package com.editlyai.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editlyai.app.data.model.UserState
import com.editlyai.app.ui.components.BannerAdView
import com.editlyai.app.ui.components.SnowBackground
import com.editlyai.app.ui.theme.EditlyPurple
import java.io.File

@Composable
fun HomeScreen(
    onNavigateToPicker: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onMediaCaptured: (uri: String, isVideo: Boolean) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val userState by viewModel.userState.collectAsState()

    // Kameradan fotoğraf çekimi için: FileProvider üzerinden geçici bir
    // dosya URI'si oluşturup TakePicture contract'ına veriyoruz.
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) pendingPhotoUri?.let { onMediaCaptured(it.toString(), false) } }

    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success -> if (success) pendingVideoUri?.let { onMediaCaptured(it.toString(), true) } }

    fun launchCameraPhoto() {
        val file = File(context.cacheDir, "editly_camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    fun launchCameraVideo() {
        val file = File(context.cacheDir, "editly_camera_${System.currentTimeMillis()}.mp4")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingVideoUri = uri
        captureVideoLauncher.launch(uri)
    }

    SnowBackground(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ana Sayfa") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
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
                QuickActionCard(Icons.Default.PhotoCamera, "Kameradan Fotoğraf", Modifier.weight(1f)) { launchCameraPhoto() }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(Icons.Default.VideoLibrary, "Galeriden Video", Modifier.weight(1f), onNavigateToPicker)
                QuickActionCard(Icons.Default.Videocam, "Kameradan Video", Modifier.weight(1f)) { launchCameraVideo() }
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

        BottomNavBar(
            onHomeClick = {},
            onProjectsClick = onNavigateToProjects,
            onSettingsClick = onNavigateToSettings
        )
    }
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
            .clickable(onClick = onClick)
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
    // NOT: Önceki sürümde sadece içteki küçük IconButton tıklanabilirdi,
    // bu yüzden kartın geri kalanına dokunmak hiçbir şey yapmıyordu. Artık
    // kartın TAMAMI tıklanabilir (Modifier.clickable).
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = EditlyPurple)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BottomNavBar(
    onHomeClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(selected = true, onClick = onHomeClick, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Ana Sayfa") })
        NavigationBarItem(selected = false, onClick = onProjectsClick, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Projelerim") })
        NavigationBarItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Ayarlar") })
    }
}

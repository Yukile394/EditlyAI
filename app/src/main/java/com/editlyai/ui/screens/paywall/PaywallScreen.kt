package com.editlyai.app.ui.screens.paywall

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.editlyai.app.data.ads.AdManager
import com.editlyai.app.data.billing.BillingManager
import com.editlyai.app.data.repository.UserRepository
import com.editlyai.app.ui.theme.EditlyPurple
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository(context) }

    val billingManager = remember {
        BillingManager(context) { _, isActive ->
            userRepository.setPremiumStatus(isActive, 0L)
        }
    }

    DisposableEffect(Unit) {
        billingManager.startConnection()
        onDispose { billingManager.endConnection() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Premium'a Geç") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            }
        )

        Column(modifier = Modifier.padding(24.dp).weight(1f)) {
            Text(
                "Sınırsız düzenleme, reklamsız deneyim ve en yüksek kalite",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(24.dp))

            listOf(
                "Günlük sınırsız yapay zekâ düzenleme",
                "Reklamsız kullanım",
                "Daha hızlı yapay zekâ işleme",
                "En yüksek kalite dışa aktarma (4K)",
                "Öncelikli sunucu erişimi"
            ).forEach { feature ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EditlyPurple)
                    Spacer(Modifier.width(8.dp))
                    Text(feature)
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))
            Text("50 TL / ay", style = MaterialTheme.typography.headlineMedium, color = EditlyPurple)
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Button(
                onClick = { activity?.let { billingManager.launchPurchaseFlow(it) } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EditlyPurple)
            ) { Text("Premium'a Abone Ol") }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { billingManager.queryExistingPurchases() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Satın Alımları Geri Yükle") }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    activity?.let {
                        AdManager.loadRewardedAd(context) { loaded ->
                            if (loaded) {
                                AdManager.showRewardedAd(it, onRewardEarned = {
                                    scope.launch { userRepository.addRewardedCredit() }
                                })
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Reklam İzle, +1 Hak Kazan") }
        }
    }
}

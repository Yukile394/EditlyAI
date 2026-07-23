package com.editlyai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.editlyai.app.ui.screens.edit.EditScreen
import com.editlyai.app.ui.screens.export.ExportScreen
import com.editlyai.app.ui.screens.home.HomeScreen
import com.editlyai.app.ui.screens.mediapicker.MediaPickerScreen
import com.editlyai.app.ui.screens.paywall.PaywallScreen
import com.editlyai.app.ui.screens.projects.ProjectsScreen
import com.editlyai.app.ui.screens.settings.SettingsScreen
import com.editlyai.app.ui.screens.videoedit.VideoEditScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val MEDIA_PICKER = "media_picker"
    const val EDIT = "edit/{mediaUri}/{isVideo}"
    const val VIDEO_EDIT = "video_edit/{mediaUri}"
    const val EXPORT = "export/{mediaUri}"
    const val PAYWALL = "paywall"
    const val SETTINGS = "settings"
    const val PROJECTS = "projects"

    fun edit(uri: String, isVideo: Boolean) =
        "edit/${URLEncoder.encode(uri, "UTF-8")}/$isVideo"

    fun videoEdit(uri: String) = "video_edit/${URLEncoder.encode(uri, "UTF-8")}"

    fun export(uri: String) = "export/${URLEncoder.encode(uri, "UTF-8")}"
}

@Composable
fun EditlyNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToPicker = { navController.navigate(Routes.MEDIA_PICKER) },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToProjects = { navController.navigate(Routes.PROJECTS) },
                onMediaCaptured = { uri, isVideo ->
                    if (isVideo) {
                        navController.navigate(Routes.videoEdit(uri))
                    } else {
                        navController.navigate(Routes.edit(uri, isVideo))
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) }
            )
        }
        composable(Routes.PROJECTS) {
            ProjectsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MEDIA_PICKER) {
            MediaPickerScreen(
                onMediaSelected = { uri, isVideo ->
                    if (isVideo) {
                        navController.navigate(Routes.videoEdit(uri))
                    } else {
                        navController.navigate(Routes.edit(uri, isVideo))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.VIDEO_EDIT,
            arguments = listOf(navArgument("mediaUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("mediaUri").orEmpty()
            val uri = URLDecoder.decode(encodedUri, "UTF-8")
            VideoEditScreen(
                mediaUri = uri,
                onBack = { navController.popBackStack() },
                onNeedPremium = { navController.navigate(Routes.PAYWALL) }
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("mediaUri") { type = NavType.StringType },
                navArgument("isVideo") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("mediaUri").orEmpty()
            val uri = URLDecoder.decode(encodedUri, "UTF-8")
            val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
            EditScreen(
                mediaUri = uri,
                isVideo = isVideo,
                onBack = { navController.popBackStack() },
                onNeedPremium = { navController.navigate(Routes.PAYWALL) },
                onExport = { finalUri -> navController.navigate(Routes.export(finalUri)) }
            )
        }
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("mediaUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("mediaUri").orEmpty()
            val uri = URLDecoder.decode(encodedUri, "UTF-8")
            ExportScreen(mediaUri = uri, onBack = { navController.popBackStack() })
        }
        composable(Routes.PAYWALL) {
            PaywallScreen(onBack = { navController.popBackStack() })
        }
    }
}

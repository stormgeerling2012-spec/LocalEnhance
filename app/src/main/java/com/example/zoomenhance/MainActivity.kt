package com.example.zoomenhance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zoomenhance.ui.screens.DownloadScreen
import com.example.zoomenhance.ui.screens.EnhanceScreen
import com.example.zoomenhance.ui.screens.SettingsScreen
import com.example.zoomenhance.ui.theme.ZoomEnhanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZoomEnhanceTheme { ZoomEnhanceApp() } }
    }
    override fun onDestroy() {
        super.onDestroy()
        NativeLib.releaseModel()
    }
}

@Composable
fun ZoomEnhanceApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "download") {
        composable("download") {
            DownloadScreen(onModelsReady = {
                navController.navigate("enhance") { popUpTo("download") { inclusive = true } }
            })
        }
        composable("enhance") {
            EnhanceScreen(onNavigateToSettings = { navController.navigate("settings") })
        }
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

package com.example.zoomenhance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zoomenhance.data.ModelManager
import kotlinx.coroutines.launch

@Composable
fun DownloadScreen(onModelsReady: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val progress by ModelManager.progress.collectAsState(initial = ModelManager.DownloadProgress())
    var hasChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasChecked) {
            hasChecked = true
            if (ModelManager.areModelsReady(context)) onModelsReady()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ZoomEnhance", fontSize = 32.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Local AI Image Enhancement", fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))

        if (progress.isDownloading) {
            LinearProgressIndicator(
                progress = { progress.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Downloading ${progress.currentModel}...", fontSize = 16.sp)
            Text("${progress.progressPercent}%", fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("One-time download (~1.8 GB total)", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (progress.error != null) {
            Text("Download Failed", color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(progress.error!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                ModelManager.resetProgress()
                scope.launch {
                    ModelManager.downloadModels(context)
                    if (ModelManager.areModelsReady(context)) onModelsReady()
                }
            }) { Text("Retry Download") }
        } else {
            Text("AI models required for local processing", fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("SD-Turbo (Q4_0): ~1.8 GB\nTAESD (Tiny VAE): ~4 MB", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        ModelManager.downloadModels(context)
                        if (ModelManager.areModelsReady(context)) onModelsReady()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("Download Models", fontSize = 18.sp) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Models stored locally after download", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

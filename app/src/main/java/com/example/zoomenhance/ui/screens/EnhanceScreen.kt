package com.example.zoomenhance.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zoomenhance.NativeLib
import com.example.zoomenhance.data.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhanceScreen(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isModelReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (ModelManager.areModelsReady(context)) {
                isModelReady = NativeLib.initModel(
                    ModelManager.getModelFile(context).absolutePath,
                    ModelManager.getTaesdFile(context).absolutePath
                )
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedBitmap = uriToBitmap(context, it); enhancedBitmap = null }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ZoomEnhance") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(320.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0F0F0)).clickable(enabled = !isLoading) {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    enhancedBitmap != null -> Image(
                        bitmap = enhancedBitmap!!.asImageBitmap(),
                        contentDescription = "Enhanced", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    selectedBitmap != null -> Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    else -> Text("Tap to select a photo", color = Color.Gray, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Local Enhance", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val input = selectedBitmap ?: return@Button
                    if (!isModelReady) return@Button
                    scope.launch(Dispatchers.Default) {
                        isLoading = true
                        try {
                            val scaled = resizeBitmap(input, 512)
                            val result = NativeLib.enhanceImage(scaled)
                            withContext(Dispatchers.Main) { enhancedBitmap = result }
                        } finally {
                            withContext(Dispatchers.Main) { isLoading = false }
                        }
                    }
                },
                enabled = !isLoading && selectedBitmap != null && isModelReady,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB), contentColor = Color.White,
                    disabledContainerColor = Color(0xFF93C5FD), disabledContentColor = Color.White
                )
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Enhance Image", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = maxDim.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
}

private fun uriToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
                d.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)?.copy(Bitmap.Config.ARGB_8888, true)
        }
    } catch (e: Exception) { e.printStackTrace(); null }
}

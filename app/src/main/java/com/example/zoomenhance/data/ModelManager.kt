package com.example.zoomenhance.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object ModelManager {

    data class DownloadProgress(
        val isDownloading: Boolean = false,
        val progressPercent: Int = 0,
        val currentModel: String = "",
        val error: String? = null
    )

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: Flow<DownloadProgress> = _progress.asStateFlow()

    private const val SD_TURBO_URL = "https://huggingface.co/gpustack/stable-diffusion-v2-1-turbo-GGUF/resolve/main/sd-turbo-Q4_0.gguf"
    private const val TAESD_URL = "https://huggingface.co/madebyollin/taesd/resolve/main/taesd.gguf"

    fun getModelFile(context: Context): File = File(context.filesDir, "sd-turbo-q4_0.gguf")
    fun getTaesdFile(context: Context): File = File(context.filesDir, "taesd.gguf")

    fun areModelsReady(context: Context): Boolean {
        return getModelFile(context).exists() && getTaesdFile(context).exists()
    }

    fun getModelSize(context: Context): String {
        val model = getModelFile(context)
        val taesd = getTaesdFile(context)
        val totalBytes = (if (model.exists()) model.length() else 0) +
                        (if (taesd.exists()) taesd.length() else 0)
        return "%.1f MB".format(totalBytes / (1024.0 * 1024.0))
    }

    suspend fun downloadModels(context: Context) = withContext(Dispatchers.IO) {
        _progress.value = DownloadProgress(isDownloading = true, currentModel = "SD-Turbo")
        try {
            downloadFile(SD_TURBO_URL, getModelFile(context), "SD-Turbo") { pct ->
                _progress.value = _progress.value.copy(progressPercent = pct / 2)
            }
            _progress.value = _progress.value.copy(currentModel = "TAESD")
            downloadFile(TAESD_URL, getTaesdFile(context), "TAESD") { pct ->
                _progress.value = _progress.value.copy(progressPercent = 50 + pct / 2)
            }
            _progress.value = DownloadProgress(isDownloading = false, progressPercent = 100)
        } catch (e: Exception) {
            _progress.value = DownloadProgress(isDownloading = false, error = e.message ?: "Failed")
        }
    }

    private fun downloadFile(url: String, destFile: File, name: String, onProgress: (Int) -> Unit) {
        val connection = URL(url).openConnection().apply {
            connectTimeout = 30000
            readTimeout = 30000
            setRequestProperty("User-Agent", "ZoomEnhance/1.0")
        }
        val totalBytes = connection.contentLengthLong
        connection.getInputStream().use { input ->
            destFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalBytes > 0) {
                        onProgress(((downloaded * 100) / totalBytes).toInt())
                    }
                }
                output.flush()
            }
        }
    }

    fun deleteModels(context: Context): Boolean {
        val m = getModelFile(context).delete()
        val t = getTaesdFile(context).delete()
        return m || t
    }

    fun resetProgress() { _progress.value = DownloadProgress() }
}

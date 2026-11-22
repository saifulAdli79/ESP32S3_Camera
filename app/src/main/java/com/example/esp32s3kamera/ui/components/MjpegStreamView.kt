package com.example.esp32s3kamera.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Composable
fun MjpegStreamView(
    ipAddress: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    var frameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val cleanIp = remember(ipAddress) {
        ipAddress.trim().removePrefix("http://").removePrefix("https://").removeSuffix("/")
    }

    LaunchedEffect(cleanIp, isStreaming) {
        if (!isStreaming || cleanIp.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val streamUrl = "http://$cleanIp/stream"

        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(streamUrl)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val inputStream = response.body?.byteStream() ?: return@use

                    val buffer = ByteArray(8192)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    var read = 0
                    var inJpeg = false
                    var prevByte = -1

                    while (isActive && inputStream.read(buffer).also { read = it } != -1) {
                        for (i in 0 until read) {
                            val currentByte = buffer[i].toInt() and 0xFF
                            if (!inJpeg) {
                                if (prevByte == 0xFF && currentByte == 0xD8) {
                                    inJpeg = true
                                    byteArrayOutputStream.reset()
                                    byteArrayOutputStream.write(0xFF)
                                    byteArrayOutputStream.write(0xD8)
                                }
                            } else {
                                byteArrayOutputStream.write(currentByte)
                                if (prevByte == 0xFF && currentByte == 0xD9) {
                                    inJpeg = false
                                    val bytes = byteArrayOutputStream.toByteArray()
                                    val options = BitmapFactory.Options().apply {
                                        inPreferredConfig = Bitmap.Config.RGB_565
                                    }
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                    if (bitmap != null) {
                                        withContext(Dispatchers.Main) {
                                            frameBitmap = bitmap
                                            isLoading = false
                                        }
                                    }
                                    byteArrayOutputStream.reset()
                                }
                            }
                            prevByte = currentByte
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = frameBitmap
        if (isStreaming && bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Live Camera Feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (isStreaming && isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Text(
                text = if (!isStreaming) "⏸️ Penstriman Dijeda" else "Sila masukkan Alamat IP ESP32",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

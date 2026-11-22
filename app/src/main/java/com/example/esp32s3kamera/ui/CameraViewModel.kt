package com.example.esp32s3kamera.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.esp32s3kamera.data.Esp32CameraRepository
import com.example.esp32s3kamera.data.SdFile
import com.example.esp32s3kamera.data.SdStatus
import com.example.esp32s3kamera.utils.WifiConnectionManager
import com.example.esp32s3kamera.utils.WifiDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.OutputStream

data class CameraUiState(
    val ipAddress: String = "192.168.4.1",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val sdStatus: SdStatus = SdStatus(),
    val isStreaming: Boolean = true,
    val flashMode: Int = 0, // 0=OFF, 1=AUTO, 2=TORCH
    val flashBrightness: Int = 200,
    val resolution: Int = 6, // 6=VGA, 7=SVGA, 9=SXGA, 10=UXGA
    val specialEffect: Int = 0, // 0=Normal, 1=Negative, 2=B&W, 4=Green, 5=Blue, 6=Sepia
    val vflip: Boolean = false,
    val hmirror: Boolean = false,
    val isTimelapseRunning: Boolean = false,
    val statusMessage: String? = null,
    val capturedPhotos: List<Bitmap> = emptyList(),
    val scannedWifiList: List<WifiDevice> = emptyList(),
    val isScanningWifi: Boolean = false,
    val selectedTab: Int = 0, // 0: Studio, 1: Galeri Kad SD
    val sdFileList: List<SdFile> = emptyList(),
    val isLoadingSdFiles: Boolean = false,
    val selectedSdPhotoBitmap: Bitmap? = null,
    val selectedSdFileName: String? = null,
    val isLoadingSdPhoto: Boolean = false
)

class CameraViewModel(
    private val repository: Esp32CameraRepository = Esp32CameraRepository(),
    private val wifiManager: WifiConnectionManager = WifiConnectionManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var timelapseJob: Job? = null

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        if (tabIndex == 1) {
            loadSdFileList()
        }
    }

    fun loadSdFileList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSdFiles = true) }
            val baseUrl = _uiState.value.ipAddress
            val result = repository.fetchSdFileList(baseUrl)
            result.onSuccess { files ->
                _uiState.update {
                    it.copy(
                        isLoadingSdFiles = false,
                        sdFileList = files,
                        statusMessage = "Kad SD: ${files.size} foto dijumpai"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingSdFiles = false,
                        statusMessage = "Gagal memuatkan senarai Kad SD: ${err.message}"
                    )
                }
            }
        }
    }

    fun openSdPhoto(filename: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSdPhoto = true, selectedSdFileName = filename) }
            val baseUrl = _uiState.value.ipAddress
            val result = repository.fetchSdFileBytes(baseUrl, filename)
            result.onSuccess { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                _uiState.update {
                    it.copy(
                        isLoadingSdPhoto = false,
                        selectedSdPhotoBitmap = bitmap
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingSdPhoto = false,
                        selectedSdFileName = null,
                        statusMessage = "Gagal memuat foto: ${err.message}"
                    )
                }
            }
        }
    }

    fun closeSdPhotoPreview() {
        _uiState.update {
            it.copy(selectedSdPhotoBitmap = null, selectedSdFileName = null)
        }
    }

    fun downloadSelectedSdPhoto(context: Context) {
        val bitmap = _uiState.value.selectedSdPhotoBitmap
        val name = _uiState.value.selectedSdFileName
        if (bitmap != null && name != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            saveToGallery(context, stream.toByteArray())
            _uiState.update { it.copy(statusMessage = "✅ Gambar '$name' disimpan ke Galeri HP!") }
        }
    }

    fun deleteSdPhoto(filename: String) {
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            val result = repository.deleteSdFile(baseUrl, filename)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        selectedSdPhotoBitmap = null,
                        selectedSdFileName = null,
                        statusMessage = "✅ Gambar '$filename' telah dipadam dari Kad SD"
                    )
                }
                loadSdFileList()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusMessage = "Gagal memadam fail: ${err.message}")
                }
            }
        }
    }

    fun updateIpAddress(ip: String) {
        _uiState.update { it.copy(ipAddress = ip.trim()) }
    }

    fun scanWifiNetworks(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningWifi = true) }
            val devices = wifiManager.scanWifiNetworks(context)
            _uiState.update {
                it.copy(
                    scannedWifiList = devices,
                    isScanningWifi = false,
                    statusMessage = if (devices.isNotEmpty()) "Pengesanan Wi-Fi: ${devices.size} Rangkaian Ditemui" else "Tiada Wi-Fi Ditemui"
                )
            }
        }
    }

    fun connectToEsp32Wifi(context: Context, ssid: String = "ESP32S3-Kamera", pass: String = "12345678") {
        _uiState.update { it.copy(statusMessage = "Menyambung ke Wi-Fi '$ssid'...") }
        wifiManager.connectToWifi(
            context = context,
            ssid = ssid,
            passphrase = pass,
            onConnected = {
                _uiState.update {
                    it.copy(
                        ipAddress = "192.168.4.1",
                        statusMessage = "✅ Berjaya menyambung ke Wi-Fi '$ssid'!"
                    )
                }
                checkStatus(context)
            },
            onError = { errMsg ->
                _uiState.update { it.copy(statusMessage = errMsg) }
            }
        )
    }

    fun bindToWifiNetwork(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }
                if (wifiNetwork != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        connectivityManager.bindProcessToNetwork(wifiNetwork)
                    } else {
                        @Suppress("DEPRECATION")
                        ConnectivityManager.setProcessDefaultNetwork(wifiNetwork)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkStatus(context: Context? = null) {
        viewModelScope.launch {
            context?.let { bindToWifiNetwork(it) }
            _uiState.update { it.copy(isConnecting = true) }
            val baseUrl = _uiState.value.ipAddress
            val result = repository.fetchSdStatus(baseUrl)
            result.onSuccess { status ->
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        sdStatus = status,
                        statusMessage = if (status.ready) "Kad SD Aktif (${status.totalMb} MB)" else "Terhubung (Tanpa Kad SD)"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        statusMessage = "Gagal terhubung (${baseUrl}): ${err.localizedMessage ?: err.message}"
                    )
                }
            }
        }
    }

    fun toggleStream() {
        _uiState.update { it.copy(isStreaming = !it.isStreaming) }
    }

    fun capturePhoto(context: Context) {
        viewModelScope.launch {
            val wasStreaming = _uiState.value.isStreaming

            // Temporarily pause stream so ESP32 camera framebuffer is freed
            if (wasStreaming) {
                _uiState.update { it.copy(isStreaming = false) }
                delay(300) // Give 300ms for HTTP stream socket on ESP32 to close
            }

            val baseUrl = _uiState.value.ipAddress
            _uiState.update { it.copy(statusMessage = "Merakam gambar ke SD & HP...") }

            val result = repository.capturePhoto(baseUrl)
            result.onSuccess { capture ->
                val bitmap = BitmapFactory.decodeByteArray(capture.bytes, 0, capture.bytes.size)
                if (bitmap != null) {
                    saveToGallery(context, capture.bytes)

                    _uiState.update {
                        val updatedList = listOf(bitmap) + it.capturedPhotos
                        val message = if (capture.sdSavedPath != null) {
                            "✅ Disimpan ke Kad SD (${capture.sdSavedPath}) & Galeri HP!"
                        } else {
                            "📥 Disimpan ke Galeri HP!"
                        }
                        it.copy(capturedPhotos = updatedList, statusMessage = message)
                    }
                    // Refresh SD status
                    checkStatus()
                } else {
                    _uiState.update { it.copy(statusMessage = "Gagal memproses gambar") }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = "Ralat tangkap gambar: ${err.message}") }
            }

            // Automatically resume stream after capture is complete
            if (wasStreaming) {
                delay(200)
                _uiState.update { it.copy(isStreaming = true) }
            }
        }
    }

    private fun saveToGallery(context: Context, imageBytes: ByteArray) {
        try {
            val timestamp = System.currentTimeMillis()
            val filename = "ESP32_AI_Foto_$timestamp.jpg"
            val contentResolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ESP32S3_Kamera")
                }
            }

            val imageUri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            imageUri?.let { uri ->
                val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
                outputStream?.use { it.write(imageBytes) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setFlashMode(mode: Int) {
        _uiState.update { it.copy(flashMode = mode) }
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            val duty = _uiState.value.flashBrightness
            repository.setFlash(baseUrl, mode, duty)
        }
    }

    fun setFlashBrightness(brightness: Int) {
        _uiState.update { it.copy(flashBrightness = brightness) }
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            val mode = _uiState.value.flashMode
            repository.setFlash(baseUrl, mode, brightness)
        }
    }

    fun setResolution(res: Int) {
        _uiState.update { it.copy(resolution = res) }
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            repository.setControl(baseUrl, "framesize", res)
        }
    }

    fun setSpecialEffect(effect: Int) {
        _uiState.update { it.copy(specialEffect = effect) }
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            repository.setControl(baseUrl, "special_effect", effect)
        }
    }

    fun toggleVflip() {
        val newVal = !_uiState.value.vflip
        _uiState.update { it.copy(vflip = newVal) }
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            repository.setControl(baseUrl, "vflip", if (newVal) 1 else 0)
        }
    }

    fun toggleHmirror() {
        val newVal = !_uiState.value.hmirror
        _uiState.update { it.copy(hmirror = newVal) }
        viewModelScope.launch {
            val baseUrl = _uiState.value.ipAddress
            repository.setControl(baseUrl, "hmirror", if (newVal) 1 else 0)
        }
    }

    fun toggleTimelapse(context: Context) {
        val isRunning = _uiState.value.isTimelapseRunning
        if (!isRunning) {
            _uiState.update { it.copy(isTimelapseRunning = true, statusMessage = "Timelapse Aktif (Setiap 5s)") }
            timelapseJob = viewModelScope.launch {
                while (true) {
                    capturePhoto(context)
                    delay(5000)
                }
            }
        } else {
            timelapseJob?.cancel()
            timelapseJob = null
            _uiState.update { it.copy(isTimelapseRunning = false, statusMessage = "Timelapse Dihentikan") }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

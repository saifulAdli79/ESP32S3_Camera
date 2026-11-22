package com.example.esp32s3kamera.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32s3kamera.data.SdStatus
import com.example.esp32s3kamera.ui.components.MjpegStreamView
import com.example.esp32s3kamera.ui.theme.ESP32S3KameraTheme

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkStatus(context)
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    CameraScreenContent(
        uiState = uiState,
        onCloseSdPhotoPreview = { viewModel.closeSdPhotoPreview() },
        onDownloadSelectedSdPhoto = { viewModel.downloadSelectedSdPhoto(context) },
        onDeleteSdPhoto = { viewModel.deleteSdPhoto(it) },
        onSetSelectedTab = { viewModel.setSelectedTab(it) },
        onCheckStatus = { viewModel.checkStatus(context) },
        onCapturePhoto = { viewModel.capturePhoto(context) },
        onToggleStream = { viewModel.toggleStream() },
        onToggleTimelapse = { viewModel.toggleTimelapse(context) },
        onSetFlashMode = { viewModel.setFlashMode(it) },
        onSetFlashBrightness = { viewModel.setFlashBrightness(it) },
        onSetResolution = { viewModel.setResolution(it) },
        onSetSpecialEffect = { viewModel.setSpecialEffect(it) },
        onToggleVflip = { viewModel.toggleVflip() },
        onToggleHmirror = { viewModel.toggleHmirror() },
        onLoadSdFileList = { viewModel.loadSdFileList() },
        onOpenSdPhoto = { viewModel.openSdPhoto(it) },
        onConnectToEsp32Wifi = { ssid, pass -> viewModel.connectToEsp32Wifi(context, ssid, pass) },
        onScanWifiNetworks = { viewModel.scanWifiNetworks(context) },
        onUpdateIpAddress = { viewModel.updateIpAddress(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CameraScreenContent(
    uiState: CameraUiState,
    onCloseSdPhotoPreview: () -> Unit = {},
    onDownloadSelectedSdPhoto: () -> Unit = {},
    onDeleteSdPhoto: (String) -> Unit = {},
    onSetSelectedTab: (Int) -> Unit = {},
    onCheckStatus: () -> Unit = {},
    onCapturePhoto: () -> Unit = {},
    onToggleStream: () -> Unit = {},
    onToggleTimelapse: () -> Unit = {},
    onSetFlashMode: (Int) -> Unit = {},
    onSetFlashBrightness: (Int) -> Unit = {},
    onSetResolution: (Int) -> Unit = {},
    onSetSpecialEffect: (Int) -> Unit = {},
    onToggleVflip: () -> Unit = {},
    onToggleHmirror: () -> Unit = {},
    onLoadSdFileList: () -> Unit = {},
    onOpenSdPhoto: (String) -> Unit = {},
    onConnectToEsp32Wifi: (String, String) -> Unit = { _, _ -> },
    onScanWifiNetworks: () -> Unit = {},
    onUpdateIpAddress: (String) -> Unit = {}
) {
    // Modal Dialog Preview Gambar Kad SD
    if (uiState.selectedSdFileName != null) {
        AlertDialog(
            onDismissRequest = { onCloseSdPhotoPreview() },
            title = {
                Text(
                    text = uiState.selectedSdFileName ?: "Foto Kad SD",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoadingSdPhoto) {
                        CircularProgressIndicator(color = Color.White)
                    } else if (uiState.selectedSdPhotoBitmap != null) {
                        Image(
                            bitmap = uiState.selectedSdPhotoBitmap!!.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Gagal memuatkan gambar", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onDownloadSelectedSdPhoto() },
                    enabled = uiState.selectedSdPhotoBitmap != null
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan ke HP")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            uiState.selectedSdFileName?.let { onDeleteSdPhoto(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Padam")
                    }

                    TextButton(onClick = { onCloseSdPhotoPreview() }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "📷 ESP32-S3 Kamera Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Navigation Tabs (Studio Kamera, Galeri Kad SD, Tetapan ⚙️)
                TabRow(
                    selectedTabIndex = uiState.selectedTab
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { onSetSelectedTab(0) },
                        text = { Text("📷 Studio") },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = null) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { onSetSelectedTab(1) },
                        text = { Text("💾 Kad SD") },
                        icon = { Icon(Icons.Default.SdCard, contentDescription = null) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 2,
                        onClick = { onSetSelectedTab(2) },
                        text = { Text("⚙️ Tetapan") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> {
                    // ==================== TAB 0: STUDIO KAMERA ====================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Ringkasan Status Ringkas (Status Badges)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val (connBg, connText) = when {
                                uiState.isConnected -> Pair(Color(0xFF10B981), "Terhubung")
                                uiState.isConnecting -> Pair(Color(0xFFF59E0B), "Menyambung...")
                                else -> Pair(Color(0xFFEF4444), "Terputus")
                            }
                            AssistChip(
                                onClick = { onCheckStatus() },
                                label = { Text(connText, color = Color.White, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = connBg)
                            )

                            val sdText = if (uiState.sdStatus.ready) {
                                "💾 Kad SD: Ready"
                            } else {
                                "💾 Kad SD: Tiada Kad"
                            }
                            AssistChip(
                                onClick = { onCheckStatus() },
                                label = { Text(sdText) },
                                leadingIcon = { Icon(Icons.Default.SdCard, contentDescription = null) }
                            )

                            if (uiState.isConnected) {
                                val batPct = uiState.sdStatus.batteryPct
                                val batMv = uiState.sdStatus.batteryMv
                                val batColor = when {
                                    batPct > 50 -> Color(0xFF10B981)
                                    batPct > 20 -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                                val batVolts = "%.2f".format(batMv / 1000f)
                                val batText = if (batMv > 0) "🔋 Bateri: ${batPct}% (${batVolts}V)" else "🔋 Bateri: Sedia"
                                AssistChip(
                                    onClick = { onCheckStatus() },
                                    label = { Text(batText, fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = batColor.copy(alpha = 0.2f))
                                )
                            }
                        }

                        // ==================== PANEL STREAM VIDEO & SNAPSHOT ====================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .background(Color.Black)
                                ) {
                                    MjpegStreamView(
                                        ipAddress = uiState.ipAddress,
                                        isStreaming = uiState.isStreaming,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .align(Alignment.TopStart)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (uiState.isStreaming) Color.Red else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (uiState.isStreaming) "LIVE STREAM" else "PAUSED",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onCapturePhoto() },
                                        modifier = Modifier.weight(2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Icon(Icons.Default.Camera, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("📸 Tangkap Foto", fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(onClick = { onToggleStream() }) {
                                        Icon(
                                            if (uiState.isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Toggle Stream"
                                        )
                                    }

                                    Button(
                                        onClick = { onToggleTimelapse() },
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (uiState.isTimelapseRunning) Color(0xFFEF4444) else Color(0xFF3B82F6)
                                        )
                                    ) {
                                        Icon(Icons.Default.MotionPhotosAuto, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (uiState.isTimelapseRunning) "Stop" else "Timelapse")
                                    }
                                }
                            }
                        }

                        // ==================== CARD: KAWALAN LAMPU FLASH ====================
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("💡 Pilihan Lampu Flash LED", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = uiState.flashMode == 0,
                                        onClick = { onSetFlashMode(0) },
                                        label = { Text("OFF") },
                                        leadingIcon = { Icon(Icons.Default.FlashOff, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = uiState.flashMode == 1,
                                        onClick = { onSetFlashMode(1) },
                                        label = { Text("Auto") },
                                        leadingIcon = { Icon(Icons.Default.FlashAuto, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = uiState.flashMode == 2,
                                        onClick = { onSetFlashMode(2) },
                                        label = { Text("Torch") },
                                        leadingIcon = { Icon(Icons.Default.FlashOn, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Kecerahan:", fontSize = 13.sp)
                                    Slider(
                                        value = uiState.flashBrightness.toFloat(),
                                        onValueChange = { onSetFlashBrightness(it.toInt()) },
                                        valueRange = 10f..255f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(uiState.flashBrightness / 255f * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // ==================== CARD: RESOLUSI KAMERA ====================
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("📐 Resolusi Kamera", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val resolutions = listOf(
                                        Pair(10, "UXGA (1600x1200)"),
                                        Pair(9, "SXGA (1280x1024)"),
                                        Pair(7, "SVGA (800x600)"),
                                        Pair(6, "VGA (640x480)")
                                    )
                                    resolutions.forEach { (resVal, label) ->
                                        FilterChip(
                                            selected = uiState.resolution == resVal,
                                            onClick = { onSetResolution(resVal) },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }

                        // ==================== CARD: EFEK WARNA & ORIENTASI ====================
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🎨 Efek Warna & Orientasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val effects = listOf(
                                        Pair(0, "Normal"),
                                        Pair(2, "Hitam Putih"),
                                        Pair(6, "Sepia Retro"),
                                        Pair(1, "Negatif"),
                                        Pair(4, "Tint Hijau"),
                                        Pair(5, "Tint Biru")
                                    )
                                    effects.forEach { (effVal, label) ->
                                        FilterChip(
                                            selected = uiState.specialEffect == effVal,
                                            onClick = { onSetSpecialEffect(effVal) },
                                            label = { Text(label) }
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = uiState.vflip,
                                        onClick = { onToggleVflip() },
                                        label = { Text("Flip Vertikal") },
                                        leadingIcon = { Icon(Icons.Default.Flip, contentDescription = null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = uiState.hmirror,
                                        onClick = { onToggleHmirror() },
                                        label = { Text("Cermin Horizontal") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ==================== CARD: GALERI FOTO SESI INI ====================
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🖼️ Tangkapan Sesi Ini (${uiState.capturedPhotos.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                if (uiState.capturedPhotos.isEmpty()) {
                                    Text("Belum ada gambar ditangkap dalam sesi ini.", color = Color.Gray, fontSize = 13.sp)
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(uiState.capturedPhotos) { bitmap ->
                                            Box(
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            ) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Captured Photo",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ==================== TAB 1: GALERI KAD SD ====================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "💾 Senarai Foto Dalam Kad SD",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Jumlah Fail: ${uiState.sdFileList.size}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            Button(
                                onClick = { onLoadSdFileList() },
                                enabled = !uiState.isLoadingSdFiles
                            ) {
                                if (uiState.isLoadingSdFiles) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh SD")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Muat Semula")
                                }
                            }
                        }

                        if (uiState.isLoadingSdFiles) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.sdFileList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Tiada gambar dijumpai dalam Kad SD ESP32", color = Color.Gray)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.sdFileList) { sdFile ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenSdPhoto(sdFile.name) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.SdCard,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )

                                            Text(
                                                sdFile.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )

                                            Text(
                                                "${sdFile.size / 1024} KB",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )

                                            Button(
                                                onClick = { onOpenSdPhoto(sdFile.name) },
                                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Pratinjau / Buka", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ==================== TAB 2: TETAPAN ⚙️ ====================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ==================== KARD SAMBUNGAN UTAMA & AUTO WI-FI (1-TAP) ====================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "📶 Sambungan Automatik Wi-Fi ESP32-S3",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Button(
                                    onClick = { onConnectToEsp32Wifi("ESP32S3-Kamera", "12345678") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ Sambung Wi-Fi ESP32S3-Kamera (1-Tekan)", fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Senarai Wi-Fi Berhampiran",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    IconButton(
                                        onClick = { onScanWifiNetworks() },
                                        enabled = !uiState.isScanningWifi
                                    ) {
                                        if (uiState.isScanningWifi) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.WifiFind, contentDescription = "Imbas Wi-Fi")
                                        }
                                    }
                                }

                                if (uiState.scannedWifiList.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        uiState.scannedWifiList.take(5).forEach { device ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (device.isEsp32) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(device.ssid, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            if (device.isEsp32) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("[ESP32 KAMERA]", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        Text("Isyarat: ${device.signalLevel}/5", fontSize = 11.sp, color = Color.Gray)
                                                    }

                                                    Button(
                                                        onClick = { onConnectToEsp32Wifi(device.ssid, "12345678") },
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text("Sambung", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ==================== KARTU SAMBUNGAN & IP ====================
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "🌐 Alamat IP ESP32-S3",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.ipAddress,
                                        onValueChange = { onUpdateIpAddress(it) },
                                        label = { Text("Contoh: 192.168.4.1 atau 192.168.1.100") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = { onCheckStatus() },
                                        enabled = !uiState.isConnecting
                                    ) {
                                        if (uiState.isConnecting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = "Semak")
                                        }
                                    }
                                }

                                // Status Badges Lengkap
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val (connBg, connText) = when {
                                        uiState.isConnected -> Pair(Color(0xFF10B981), "Terhubung")
                                        uiState.isConnecting -> Pair(Color(0xFFF59E0B), "Menyambung...")
                                        else -> Pair(Color(0xFFEF4444), "Terputus")
                                    }
                                    AssistChip(
                                        onClick = { onCheckStatus() },
                                        label = { Text(connText, color = Color.White, fontWeight = FontWeight.Bold) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = connBg)
                                    )

                                    val sdText = if (uiState.sdStatus.ready) {
                                        "💾 Kad SD: ${uiState.sdStatus.usedMb}/${uiState.sdStatus.totalMb} MB"
                                    } else {
                                        "💾 Kad SD: Tiada Kad"
                                    }
                                    AssistChip(
                                        onClick = { onCheckStatus() },
                                        label = { Text(sdText) },
                                        leadingIcon = { Icon(Icons.Default.SdCard, contentDescription = null) }
                                    )

                                    if (uiState.isConnected) {
                                        val batPct = uiState.sdStatus.batteryPct
                                        val batMv = uiState.sdStatus.batteryMv
                                        val batColor = when {
                                            batPct > 50 -> Color(0xFF10B981)
                                            batPct > 20 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        }
                                        val batVolts = "%.2f".format(batMv / 1000f)
                                        val batText = if (batMv > 0) "🔋 Bateri: ${batPct}% (${batVolts}V)" else "🔋 Bateri: Sedia"
                                        AssistChip(
                                            onClick = { onCheckStatus() },
                                            label = { Text(batText, fontWeight = FontWeight.Bold) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = batColor.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    ESP32S3KameraTheme {
        CameraScreenContent(
            uiState = CameraUiState(
                ipAddress = "192.168.4.1",
                isConnected = true,
                sdStatus = SdStatus(
                    ready = true,
                    totalMb = 31200,
                    usedMb = 4500,
                    batteryMv = 4100,
                    batteryPct = 85
                ),
                isStreaming = false,
                flashMode = 1,
                flashBrightness = 200,
                resolution = 6,
                specialEffect = 0,
                selectedTab = 0
            )
        )
    }
}

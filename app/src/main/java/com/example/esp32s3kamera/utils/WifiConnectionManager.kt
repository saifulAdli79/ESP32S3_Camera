package com.example.esp32s3kamera.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings

data class WifiDevice(
    val ssid: String,
    val signalLevel: Int,
    val isEsp32: Boolean
)

class WifiConnectionManager {

    @SuppressLint("MissingPermission")
    fun scanWifiNetworks(context: Context): List<WifiDevice> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return emptyList()

        try {
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val results: List<ScanResult> = try {
            wifiManager.scanResults ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return results
            .mapNotNull { result ->
                val ssid: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.wifiSsid?.toString()?.replace("\"", "") ?: result.SSID
                } else {
                    @Suppress("DEPRECATION")
                    result.SSID?.replace("\"", "")
                }

                if (ssid.isNull_or_blank()) {
                    null
                } else {
                    val validSsid = ssid!!
                    val level = WifiManager.calculateSignalLevel(result.level, 5)
                    val isEsp32 = validSsid.contains("ESP32", ignoreCase = true) || 
                                  validSsid.contains("Kamera", ignoreCase = true) ||
                                  validSsid.contains("Camera", ignoreCase = true)
                    WifiDevice(ssid = validSsid, signalLevel = level, isEsp32 = isEsp32)
                }
            }
            .distinctBy { it.ssid }
            .sortedWith(compareByDescending<WifiDevice> { it.isEsp32 }.thenByDescending { it.signalLevel })
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty() || this == "<unknown ssid>"
    }

    fun openWifiPanel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun connectToWifi(
        context: Context,
        ssid: String = "ESP32S3-Kamera",
        passphrase: String = "12345678",
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            onError("ConnectivityManager tidak ditemui")
            openWifiPanel(context)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(passphrase)
                    .build()

                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        connectivityManager.bindProcessToNetwork(network)
                        onConnected()
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        openWifiPanel(context)
                    }
                }

                connectivityManager.requestNetwork(request, callback)
            } catch (e: Exception) {
                openWifiPanel(context)
            }
        } else {
            openWifiPanel(context)
        }
    }
}

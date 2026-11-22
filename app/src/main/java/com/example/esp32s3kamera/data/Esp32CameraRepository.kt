package com.example.esp32s3kamera.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SdStatus(
    val ready: Boolean = false,
    val totalMb: Long = 0,
    val usedMb: Long = 0,
    val nextPhoto: Long = 0,
    val batteryMv: Int = 0,
    val batteryPct: Int = 0
)

data class SdFile(
    val name: String,
    val size: Long
)

data class CaptureResult(
    val bytes: ByteArray,
    val sdSavedPath: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CaptureResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (sdSavedPath != other.sdSavedPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (sdSavedPath?.hashCode() ?: 0)
        return result
    }
}

class Esp32CameraRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun normalizeUrl(baseUrl: String, path: String): String {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val formattedBase = if (!cleanBase.startsWith("http://") && !cleanBase.startsWith("https://")) {
            "http://$cleanBase"
        } else {
            cleanBase
        }
        return "$formattedBase$path"
    }

    suspend fun fetchSdStatus(baseUrl: String): Result<SdStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalizeUrl(baseUrl, "/sd_status")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
                val bodyStr = response.body?.string() ?: throw Exception("Empty body")
                val json = JSONObject(bodyStr)
                SdStatus(
                    ready = json.optBoolean("ready", false),
                    totalMb = json.optLong("total_mb", 0),
                    usedMb = json.optLong("used_mb", 0),
                    nextPhoto = json.optLong("next_photo", 0),
                    batteryMv = json.optInt("battery_mv", 0),
                    batteryPct = json.optInt("battery_pct", 0)
                )
            }
        }
    }

    suspend fun fetchSdFileList(baseUrl: String): Result<List<SdFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalizeUrl(baseUrl, "/sd_list")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyStr)
                val list = mutableListOf<SdFile>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        SdFile(
                            name = obj.optString("name", ""),
                            size = obj.optLong("size", 0)
                        )
                    )
                }
                list.reversed()
            }
        }
    }

    suspend fun fetchSdFileBytes(baseUrl: String, filename: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = filename.removePrefix("/")
            val url = normalizeUrl(baseUrl, "/sd_get?file=$cleanName")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
                response.body?.bytes() ?: throw Exception("Empty image data")
            }
        }
    }

    suspend fun deleteSdFile(baseUrl: String, filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = filename.removePrefix("/")
            val url = normalizeUrl(baseUrl, "/sd_delete?file=$cleanName")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Delete failed")
            }
        }
    }

    suspend fun capturePhoto(baseUrl: String): Result<CaptureResult> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val url = normalizeUrl(baseUrl, "/capture?t=$timestamp")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Capture failed with code ${response.code}")
                val bytes = response.body?.bytes() ?: throw Exception("No image data received")
                val sdSavedPath = response.header("X-SD-Saved")
                CaptureResult(bytes = bytes, sdSavedPath = sdSavedPath)
            }
        }
    }

    suspend fun setFlash(baseUrl: String, mode: Int, brightness: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalizeUrl(baseUrl, "/flash?mode=$mode&val=$brightness")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Flash set failed")
            }
        }
    }

    suspend fun setControl(baseUrl: String, variable: String, value: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalizeUrl(baseUrl, "/control?var=$variable&val=$value")
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Control set failed for $variable")
            }
        }
    }
}

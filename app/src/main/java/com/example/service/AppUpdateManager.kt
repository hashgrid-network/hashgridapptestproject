package com.example.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val minSupportedVersionCode: Int = 1,
    val isMandatory: Boolean = false,
    val publishedAt: String = ""
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val info: AppUpdateInfo, val progressPercent: Int) : UpdateStatus()
    data class ReadyToInstall(val info: AppUpdateInfo, val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object AppUpdateManager {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadProgressJob: Job? = null
    private var currentDownloadId: Long = -1L
    private var downloadedApkFile: File? = null
    private var receiverRegistered = false

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // Configurable endpoint for remote version manifest
    var remoteVersionUrl: String = "https://raw.githubusercontent.com/hashgrid/hashgrid-mobile/main/version.json"
    var githubReleasesApiUrl: String = "https://api.github.com/repos/hashgrid/hashgrid-mobile/releases/latest"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == currentDownloadId && context != null) {
                    handleDownloadComplete(context, id)
                }
            }
        }
    }

    fun initialize(context: Context) {
        if (!receiverRegistered) {
            try {
                val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.applicationContext.registerReceiver(
                        downloadCompleteReceiver,
                        filter,
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    context.applicationContext.registerReceiver(downloadCompleteReceiver, filter)
                }
                receiverRegistered = true
            } catch (_: Exception) {
                // Ignore receiver registration issues
            }
        }
    }

    /**
     * Check remote version on app launch:
     * 1. Query raw GitHub version.json or GitHub Latest Release API
     * 2. Compare remote versionCode against local BuildConfig.VERSION_CODE
     */
    fun checkForUpdates(context: Context? = null, isManualCheck: Boolean = false) {
        serviceScope.launch {
            _updateStatus.value = UpdateStatus.Checking

            try {
                val updateInfo = fetchRemoteVersionInfo()

                if (updateInfo != null) {
                    val currentVersionCode = BuildConfig.VERSION_CODE
                    if (updateInfo.versionCode > currentVersionCode) {
                        _updateStatus.value = UpdateStatus.UpdateAvailable(updateInfo)
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                        if (isManualCheck && context != null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "HashGrid is up to date (v${BuildConfig.VERSION_NAME}).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    // If remote is not reachable or empty, return UpToDate or Error
                    if (isManualCheck) {
                        _updateStatus.value = UpdateStatus.Error("Unable to reach update server.")
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                    }
                }
            } catch (e: Exception) {
                if (isManualCheck) {
                    _updateStatus.value = UpdateStatus.Error(e.localizedMessage ?: "Update check failed.")
                } else {
                    _updateStatus.value = UpdateStatus.UpToDate
                }
            }
        }
    }

    /**
     * Attempts to query version.json first, then falls back to GitHub Releases API
     */
    private fun fetchRemoteVersionInfo(): AppUpdateInfo? {
        // Try raw version.json
        try {
            val request = Request.Builder()
                .url(remoteVersionUrl)
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "HashGrid-Android-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = parseVersionJson(body)
                        if (parsed != null) return parsed
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: Try GitHub Releases API
        try {
            val request = Request.Builder()
                .url(githubReleasesApiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "HashGrid-Android-Updater")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = parseGitHubReleaseJson(body)
                        if (parsed != null) return parsed
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun parseVersionJson(jsonStr: String): AppUpdateInfo? {
        return try {
            val obj = JSONObject(jsonStr)
            val versionCode = obj.optInt("versionCode", 0)
            val versionName = obj.optString("versionName", "1.0.0")
            val downloadUrl = obj.optString("downloadUrl", "")
            val releaseNotes = obj.optString("releaseNotes", "")
            val minSupported = obj.optInt("minSupportedVersionCode", 1)
            val isMandatory = obj.optBoolean("isMandatory", false)

            if (versionCode > 0 && downloadUrl.isNotBlank()) {
                AppUpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    minSupportedVersionCode = minSupported,
                    isMandatory = isMandatory
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseGitHubReleaseJson(jsonStr: String): AppUpdateInfo? {
        return try {
            val obj = JSONObject(jsonStr)
            val tagName = obj.optString("tag_name", "") // e.g. "v2.0" or "v2"
            val releaseNotes = obj.optString("body", "")
            val publishedAt = obj.optString("published_at", "")

            // Parse version code from tag or assets
            val cleanTag = tagName.removePrefix("v").trim()
            val versionNumPart = cleanTag.split(".").firstOrNull()?.toIntOrNull() ?: 2

            // Look for .apk asset
            var apkDownloadUrl = ""
            val assets = obj.optJSONArray("assets") ?: JSONArray()
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }

            if (apkDownloadUrl.isNotBlank()) {
                AppUpdateInfo(
                    versionCode = versionNumPart,
                    versionName = cleanTag,
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Download the latest APK file in background using Android DownloadManager
     */
    fun startDownload(context: Context, info: AppUpdateInfo) {
        val appContext = context.applicationContext
        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Target file in app-specific external files dir (Scoped Storage friendly)
        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadsDir, "hashgrid-v${info.versionName}-build${info.versionCode}.apk")
        downloadedApkFile = apkFile

        if (apkFile.exists()) {
            apkFile.delete()
        }

        try {
            val uri = Uri.parse(info.downloadUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle("HashGrid v${info.versionName}")
                setDescription("Downloading HashGrid update...")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(apkFile))
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            currentDownloadId = downloadManager.enqueue(request)
            _updateStatus.value = UpdateStatus.Downloading(info, 0)

            // Start polling progress
            startProgressTracker(downloadManager, currentDownloadId, info, apkFile)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Failed to initiate download: ${e.localizedMessage}")
        }
    }

    private fun startProgressTracker(
        downloadManager: DownloadManager,
        downloadId: Long,
        info: AppUpdateInfo,
        apkFile: File
    ) {
        downloadProgressJob?.cancel()
        downloadProgressJob = serviceScope.launch {
            var isFinished = false
            while (isActive && !isFinished) {
                delay(600L)
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    val bytesDownloaded = if (bytesIndex != -1) cursor.getLong(bytesIndex) else 0L
                    val totalBytes = if (totalIndex != -1) cursor.getLong(totalIndex) else 0L
                    val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val percent = if (totalBytes > 0) {
                                ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
                            } else {
                                15
                            }
                            _updateStatus.value = UpdateStatus.Downloading(info, percent)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isFinished = true
                            _updateStatus.value = UpdateStatus.ReadyToInstall(info, apkFile)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isFinished = true
                            _updateStatus.value = UpdateStatus.Error("Download failed. Please check network connection.")
                        }
                    }
                    cursor.close()
                }
            }
        }
    }

    private fun handleDownloadComplete(context: Context, downloadId: Long) {
        val file = downloadedApkFile
        val status = _updateStatus.value
        val info = when (status) {
            is UpdateStatus.Downloading -> status.info
            is UpdateStatus.UpdateAvailable -> status.info
            else -> null
        }

        if (file != null && file.exists() && info != null) {
            _updateStatus.value = UpdateStatus.ReadyToInstall(info, file)
            // Trigger install directly
            triggerInstall(context, file)
        }
    }

    /**
     * Trigger system Package Installer using FileProvider
     */
    fun triggerInstall(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "Update file not found. Please re-download.", Toast.LENGTH_SHORT).show()
            return
        }

        // On Android 8.0+ (API 26+), check if unknown app installs are allowed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(
                        context,
                        "Please allow HashGrid to install updates, then tap Install.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                } catch (_: Exception) {}
            }
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Install failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * For testing/demo verification: simulate finding an update
     */
    fun simulateUpdateAvailable(
        versionCode: Int = BuildConfig.VERSION_CODE + 1,
        versionName: String = "2.1.0",
        notes: String = "• Verified Arctic Node PPA telemetry\n• Enhanced Dual-Engine WebSocket stream\n• Institutional cold-storage security patches\n• Instant push notification engine"
    ) {
        _updateStatus.value = UpdateStatus.UpdateAvailable(
            AppUpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                downloadUrl = "https://github.com/hashgrid/hashgrid-mobile/releases/latest/download/hashgrid-update.apk",
                releaseNotes = notes,
                isMandatory = false
            )
        )
    }

    fun dismissUpdate() {
        _updateStatus.value = UpdateStatus.Idle
    }
}

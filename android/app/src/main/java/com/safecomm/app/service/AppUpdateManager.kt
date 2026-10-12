package com.safecomm.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.safecomm.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val newVersion: String,
        val releaseNotes: String,
        val downloadUrl: String
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progressPercent: Int) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * In-App Auto-Updater for Varta Radio PTT directly from GitHub Releases.
 */
class AppUpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("varta_update_prefs", Context.MODE_PRIVATE)

    var githubRepo: String
        get() {
            val saved = prefs.getString("github_repo", null)
            return if (saved.isNullOrBlank() || saved.equals("your-repo/varta-radio", ignoreCase = true)) {
                "danilpromsiz-byte/varta-radio"
            } else {
                saved
            }
        }
        set(value) = prefs.edit().putString("github_repo", value.trim()).apply()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    suspend fun checkForUpdates(): Result<Boolean> = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val repo = githubRepo
            if (repo.isBlank() || !repo.contains("/")) {
                val err = "Укажите GitHub репозиторий в формате owner/repo"
                _updateStatus.value = UpdateStatus.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            if (repo.equals("your-repo/varta-radio", ignoreCase = true) || repo.startsWith("your-repo/")) {
                val err = "Замените «your-repo/varta-radio» на ваш реальный репозиторий на GitHub"
                _updateStatus.value = UpdateStatus.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "VartaRadio-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    val err = "Репозиторий «$repo» или релизы в нём не найдены (404). Опубликуйте релиз на GitHub!"
                    _updateStatus.value = UpdateStatus.Error(err)
                    return@withContext Result.failure(Exception(err))
                }
                val err = "Ошибка GitHub API: HTTP ${response.code}"
                _updateStatus.value = UpdateStatus.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val body = response.body?.string() ?: ""
            val release = json.decodeFromString<GitHubRelease>(body)

            val remoteTag = release.tagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            Log.d("AppUpdater", "Current: $currentVersion, Latest on GitHub: $remoteTag")

            val isNewer = isVersionNewer(remoteTag, currentVersion)
            if (isNewer) {
                val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    _updateStatus.value = UpdateStatus.UpdateAvailable(
                        newVersion = release.tagName,
                        releaseNotes = release.body ?: "",
                        downloadUrl = apkAsset.downloadUrl
                    )
                    Result.success(true)
                } else {
                    _updateStatus.value = UpdateStatus.Error("В релизе $remoteTag нет APK файла")
                    Result.success(false)
                }
            } else {
                _updateStatus.value = UpdateStatus.UpToDate
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("AppUpdater", "Update check failed", e)
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Ошибка проверки обновлений")
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstallApk(downloadUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0)

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "VartaRadio-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Сбой скачивания APK: HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Пустой ответ от сервера")
            val totalBytes = body.contentLength()
            val apkFile = File(context.cacheDir, "varta_update_${System.currentTimeMillis()}.apk")

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            _updateStatus.value = UpdateStatus.Downloading(percent)
                        }
                    }
                    output.flush()
                }
            }

            _updateStatus.value = UpdateStatus.ReadyToInstall(apkFile)
            triggerApkInstall(apkFile)
            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Download failed", e)
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Сбой скачивания")
            Result.failure(e)
        }
    }

    fun triggerApkInstall(apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Launch installer error", e)
            _updateStatus.value = UpdateStatus.Error("Не удалось запустить установку: ${e.message}")
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        try {
            val rParts = remote.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val cParts = current.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (_: Exception) {
            return remote != current
        }
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }
}

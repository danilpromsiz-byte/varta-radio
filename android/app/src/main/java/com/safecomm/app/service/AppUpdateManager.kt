package com.safecomm.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val newVersion: String,
        val releaseNotes: String
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Manages Google Play Store updates for Varta GO.
 * Fully compliant with Google Play Device and Network Abuse policies:
 * redirects all updates directly to the official Google Play Store page.
 */
class AppUpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    /**
     * Redirects user directly to Google Play Store page for Varta GO.
     */
    fun openGooglePlay(customContext: Context? = null) {
        val ctx = customContext ?: context
        val packageName = ctx.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(webIntent)
        }
    }

    /**
     * Checks if a newer version of the app is available.
     */
    suspend fun checkForUpdates(force: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val repo = githubRepo
            if (repo.isBlank() || !repo.contains("/")) {
                val err = "Укажите GitHub репозиторий в формате owner/repo"
                _updateStatus.value = UpdateStatus.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "VartaRadio-App")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    val err = "Репозиторий «$repo» или релизы в нём не найдены (404)."
                    _updateStatus.value = if (force) UpdateStatus.Error(err) else UpdateStatus.Idle
                    return@withContext Result.failure(Exception(err))
                }
                val err = "Ошибка проверки обновлений: HTTP ${response.code}"
                _updateStatus.value = if (force) UpdateStatus.Error(err) else UpdateStatus.Idle
                return@withContext Result.failure(Exception(err))
            }

            val body = response.body?.string() ?: ""
            val release = json.decodeFromString<GitHubRelease>(body)

            val remoteTag = release.tagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            Log.d("AppUpdater", "Current: $currentVersion, Latest: $remoteTag, Force=$force")

            val isNewer = isVersionNewer(remoteTag, currentVersion)
            if (isNewer || force) {
                _updateStatus.value = UpdateStatus.UpdateAvailable(
                    newVersion = release.tagName,
                    releaseNotes = release.body ?: ""
                )
                Result.success(true)
            } else {
                _updateStatus.value = if (force) UpdateStatus.UpToDate else UpdateStatus.Idle
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("AppUpdater", "Update check failed", e)
            _updateStatus.value = if (force) UpdateStatus.Error(e.message ?: "Ошибка проверки обновлений") else UpdateStatus.Idle
            Result.failure(e)
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
}

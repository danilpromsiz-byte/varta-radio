package com.safecomm.app.data.api

import android.content.Context
import android.util.Log
import com.safecomm.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val prefs = context.getSharedPreferences("safecomm_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
        set(value) = prefs.edit().putString("base_url", value).apply()

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var currentUserId: Long
        get() = prefs.getLong("current_user_id", 0)
        set(value) = prefs.edit().putLong("current_user_id", value).apply()

    var currentGroupId: Long
        get() = prefs.getLong("current_group_id", 0)
        set(value) = prefs.edit().putLong("current_group_id", value).apply()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            authToken?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // --- Authentication ---

    suspend fun loginWithTelegramOrDev(tgId: Long, nickname: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            // For dev/standalone android: we send init_data or direct fields
            val payload = """{"init_data":"","nickname":"$nickname","telegram_id":$tgId}"""
            val body = payload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/auth/init")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Auth failed: ${response.code} $respBody"))
            }

            val authResp = json.decodeFromString<AuthResponse>(respBody)
            authToken = authResp.token
            currentUserId = authResp.user.id
            Result.success(authResp)
        } catch (e: Exception) {
            Log.e("ApiClient", "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/profile").get().build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Get profile: ${response.code}"))

            val user = json.decodeFromString<User>(respBody)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(nickname: String, avatarUrl: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(ProfileRequest(nickname, avatarUrl)).toRequestBody(jsonMediaType)
            val request = Request.Builder().url("$baseUrl/api/profile").post(body).build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Update profile: ${response.code}"))

            val user = json.decodeFromString<User>(respBody)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Groups ---

    suspend fun getGroups(): Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/groups").get().build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Get groups: ${response.code}"))

            val groups = json.decodeFromString<List<Group>>(respBody)
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, pass: String): Result<Group> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(CreateGroupRequest(name, pass)).toRequestBody(jsonMediaType)
            val request = Request.Builder().url("$baseUrl/api/groups").post(body).build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Create group: ${response.code} $respBody"))

            val group = json.decodeFromString<Group>(respBody)
            currentGroupId = group.id
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGroup(inviteCode: String, pass: String): Result<Group> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(JoinGroupRequest(inviteCode, pass)).toRequestBody(jsonMediaType)
            val request = Request.Builder().url("$baseUrl/api/groups/join").post(body).build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Join group: ${response.code} $respBody"))

            val group = json.decodeFromString<Group>(respBody)
            currentGroupId = group.id
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGroupMembers(groupId: Long): Result<List<GroupMember>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/groups/$groupId/members").get().build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Get members: ${response.code}"))

            val members = json.decodeFromString<List<GroupMember>>(respBody)
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun kickMember(groupId: Long, userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/groups/$groupId/members/$userId").delete().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Kick: ${response.code}"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Voice Messages ---

    suspend fun uploadVoice(groupId: Long, audioFile: File, durationSec: Int): Result<VoiceMessage> = withContext(Dispatchers.IO) {
        try {
            val fileBody = audioFile.asRequestBody("audio/mp4".toMediaType())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("voice", audioFile.name, fileBody)
                .addFormDataPart("duration_sec", durationSec.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/groups/$groupId/voice")
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Upload voice: ${response.code}"))

            val voice = json.decodeFromString<VoiceMessage>(respBody)
            Result.success(voice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVoiceHistory(groupId: Long, limit: Int = 50): Result<List<VoiceMessage>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/groups/$groupId/voice?limit=$limit").get().build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Get voice history: ${response.code}"))

            val history = json.decodeFromString<List<VoiceMessage>>(respBody)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Alerts ---

    suspend fun createAlert(
        groupId: Long,
        type: AlertType,
        lat: Double,
        lng: Double,
        description: String,
        photoFile: File? = null
    ): Result<Alert> = withContext(Dispatchers.IO) {
        try {
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("alert_type", type.rawValue)
                .addFormDataPart("latitude", lat.toString())
                .addFormDataPart("longitude", lng.toString())
                .addFormDataPart("description", description)

            photoFile?.let { photo ->
                if (photo.exists()) {
                    val photoBody = photo.asRequestBody("image/jpeg".toMediaType())
                    multipartBuilder.addFormDataPart("photo", photo.name, photoBody)
                }
            }

            val request = Request.Builder()
                .url("$baseUrl/api/groups/$groupId/alerts")
                .post(multipartBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Create alert: ${response.code} $respBody"))

            val alert = json.decodeFromString<Alert>(respBody)
            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveAlerts(groupId: Long): Result<List<Alert>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/groups/$groupId/alerts").get().build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Get alerts: ${response.code}"))

            val alerts = json.decodeFromString<List<Alert>>(respBody)
            Result.success(alerts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveAlert(groupId: Long, alertId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "".toRequestBody(jsonMediaType)
            val request = Request.Builder().url("$baseUrl/api/groups/$groupId/alerts/$alertId/resolve").put(emptyBody).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Resolve alert: ${response.code}"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Settings ---

    suspend fun getSettings(): Result<UserSettings> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/api/profile").get().build()
            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Get settings: ${response.code}"))

            // Backend returns settings in profile or we use default
            Result.success(UserSettings(alertRadiusKm = 2.0, soundEnabled = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSettings(radiusKm: Double, soundEnabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(UpdateSettingsRequest(radiusKm, soundEnabled)).toRequestBody(jsonMediaType)
            val request = Request.Builder().url("$baseUrl/api/settings").put(body).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(IOException("Update settings: ${response.code}"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

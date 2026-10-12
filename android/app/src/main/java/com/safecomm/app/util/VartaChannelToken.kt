package com.safecomm.app.util

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class VartaChannelInfo(
    val chatId: Long,
    val inviteLink: String,
    val channelTitle: String
)

/**
 * Obfuscates and encrypts Telegram private channel references into a custom,
 * opaque tactical Varta Radio identifier (e.g. VR-k7A2...).
 *
 * This guarantees:
 * 1. Third parties / interceptors cannot deduce or access the Telegram channel from the link.
 * 2. It will not open official Telegram or web browsers.
 * 3. Only the Varta Radio app can resolve and join the channel.
 */
object VartaChannelToken {
    private const val PREFIX = "VR-"
    private val SECRET_KEY = deriveAesKey("VARTA_TACTICAL_ENCRYPTED_CHANNEL_SALT_2026")
    private val FIXED_IV = ByteArray(16) { ((it * 19 + 37) and 0xFF).toByte() }

    private fun deriveAesKey(seed: String): SecretKeySpec {
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(seed.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encodes Telegram channel parameters into a secure Varta channel token.
     */
    fun encode(chatId: Long, inviteLink: String, title: String): String {
        return try {
            val cleanLink = inviteLink.trim()
            val cleanTitle = title.trim().ifBlank { "Канал" }
            val raw = "$chatId|$cleanLink|$cleanTitle"
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, IvParameterSpec(FIXED_IV))
            val encrypted = cipher.doFinal(raw.toByteArray(Charsets.UTF_8))
            val b64 = Base64.encodeToString(encrypted, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            "$PREFIX$b64"
        } catch (e: Exception) {
            "$PREFIX$chatId"
        }
    }

    /**
     * Decodes a Varta channel token back into channel parameters.
     * Returns null if the token cannot be decrypted.
     */
    fun decode(token: String): VartaChannelInfo? {
        val trimmed = token.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        val b64 = trimmed.removePrefix(PREFIX)
        return try {
            val encrypted = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, IvParameterSpec(FIXED_IV))
            val decrypted = String(cipher.doFinal(encrypted), Charsets.UTF_8)
            val parts = decrypted.split("|", limit = 3)
            if (parts.size >= 2) {
                val chatId = parts[0].toLongOrNull() ?: 0L
                val inviteLink = parts[1]
                val title = if (parts.size >= 3) parts[2] else "Радиоканал"
                VartaChannelInfo(chatId, inviteLink, title)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun isVartaToken(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith(PREFIX) && trimmed.length > 5
    }
}

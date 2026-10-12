package com.safecomm.app.ui.i18n

import android.content.Context
import com.safecomm.app.BuildConfig

enum class AppLanguage(val code: String, val title: String, val flag: String) {
    EN("en", "English", "🇬🇧"),
    UK("uk", "Українська", "🇺🇦"),
    RU("ru", "Русский", "🇷🇺");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
        }
    }
}

object LanguageManager {
    private const val PREFS_NAME = "varta_lang_prefs"
    private const val KEY_LANG = "app_language"
    private const val KEY_PROMPT_VERSION = "lang_prompt_version"

    fun getLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANG, null)
        return if (code != null) {
            AppLanguage.fromCode(code)
        } else {
            val sys = java.util.Locale.getDefault().language.lowercase()
            when {
                sys.startsWith("uk") -> AppLanguage.UK
                sys.startsWith("ru") -> AppLanguage.RU
                else -> AppLanguage.EN
            }
        }
    }

    fun setLanguage(context: Context, lang: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, lang.code)
            .putInt(KEY_PROMPT_VERSION, 23)
            .apply()
    }

    fun shouldPromptLanguage(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastVer = prefs.getInt(KEY_PROMPT_VERSION, 0)
        return lastVer < 23
    }

    fun markPromptCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PROMPT_VERSION, 23)
            .apply()
    }
}

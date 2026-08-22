package com.git.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "git_settings")

object SettingsRepository {
    fun getSettings(context: Context): Flow<UiSettings> = context.settingsStore.data.map { prefs ->
        UiSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.raw == prefs[Keys.THEME_MODE] } ?: ThemeMode.SYSTEM,
            bgPreset = BgPreset.entries.firstOrNull { it.raw == prefs[Keys.BG_PRESET] } ?: BgPreset.DEFAULT,
            accentPreset = AccentPreset.entries.firstOrNull { it.raw == prefs[Keys.ACCENT_PRESET] } ?: AccentPreset.INDIGO,
            language = AppLanguage.entries.firstOrNull { it.raw == prefs[Keys.LANGUAGE] } ?: AppLanguage.ZH,
            customBgPath = prefs[Keys.CUSTOM_BG],
            bgAlpha = prefs[Keys.BG_ALPHA] ?: 0.3f,
            gitUserName = prefs[Keys.GIT_USER_NAME] ?: "",
            gitUserEmail = prefs[Keys.GIT_USER_EMAIL] ?: "",
            logMaxBytes = prefs[Keys.LOG_MAX_BYTES] ?: 0L,
            logMaxFiles = prefs[Keys.LOG_MAX_FILES] ?: 0
        )
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.THEME_MODE] = mode.raw }
    }

    suspend fun setBgPreset(context: Context, preset: BgPreset) {
        context.settingsStore.edit { it[Keys.BG_PRESET] = preset.raw }
    }

    suspend fun setAccentPreset(context: Context, preset: AccentPreset) {
        context.settingsStore.edit { it[Keys.ACCENT_PRESET] = preset.raw }
    }

    suspend fun setLanguage(context: Context, language: AppLanguage) {
        context.settingsStore.edit { it[Keys.LANGUAGE] = language.raw }
        applyLanguageSync(context, language)
    }

    suspend fun setCustomBg(context: Context, path: String?) {
        context.settingsStore.edit { it[Keys.CUSTOM_BG] = path ?: "" }
    }

    suspend fun setBgAlpha(context: Context, alpha: Float) {
        context.settingsStore.edit { it[Keys.BG_ALPHA] = alpha }
    }

    suspend fun setGlobalGitUser(context: Context, name: String, email: String) {
        context.settingsStore.edit {
            it[Keys.GIT_USER_NAME] = name
            it[Keys.GIT_USER_EMAIL] = email
        }
    }

    suspend fun setLogLimits(context: Context, maxBytes: Long, maxFiles: Int) {
        context.settingsStore.edit {
            it[Keys.LOG_MAX_BYTES] = maxBytes
            it[Keys.LOG_MAX_FILES] = maxFiles
        }
    }

    private fun applyLanguageSync(context: Context, language: AppLanguage) {
        context.getSharedPreferences("settings_locale", Context.MODE_PRIVATE)
            .edit()
            .putString("tag", language.tag)
            .commit()
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BG_PRESET = stringPreferencesKey("bg_preset")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val LANGUAGE = stringPreferencesKey("language")
        val CUSTOM_BG = stringPreferencesKey("custom_bg")
        val BG_ALPHA = floatPreferencesKey("bg_alpha")
        val GIT_USER_NAME = stringPreferencesKey("git_user_name")
        val GIT_USER_EMAIL = stringPreferencesKey("git_user_email")
        val LOG_MAX_BYTES = longPreferencesKey("log_max_bytes")
        val LOG_MAX_FILES = intPreferencesKey("log_max_files")
    }
}

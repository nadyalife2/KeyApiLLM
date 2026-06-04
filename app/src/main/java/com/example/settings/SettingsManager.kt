package com.example.settings

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "FreeLlmHubSettings"
    
    // Keys
    private const val KEY_GITHUB_PAT = "github_pat"
    private const val KEY_CLIPBOARD_TIMEOUT = "clipboard_timeout"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ENABLE_SCREENSHOTS = "enable_screenshots"
    private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getGitHubPat(context: Context): String {
        return getPrefs(context).getString(KEY_GITHUB_PAT, "") ?: ""
    }

    fun setGitHubPat(context: Context, pat: String) {
        getPrefs(context).edit().putString(KEY_GITHUB_PAT, pat).apply()
    }

    fun getClipboardTimeout(context: Context): Int {
        return getPrefs(context).getInt(KEY_CLIPBOARD_TIMEOUT, 60)
    }

    fun setClipboardTimeout(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_CLIPBOARD_TIMEOUT, seconds).apply()
    }

    fun getThemeMode(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_MODE, "System") ?: "System"
    }

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getEnableScreenshots(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLE_SCREENSHOTS, true)
    }

    fun setEnableScreenshots(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_SCREENSHOTS, enabled).apply()
    }

    fun getBiometricsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRICS_ENABLED, false)
    }

    fun setBiometricsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRICS_ENABLED, enabled).apply()
    }
}

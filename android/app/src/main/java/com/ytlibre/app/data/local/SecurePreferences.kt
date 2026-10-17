package com.ytlibre.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Préférences de l'application.
 *
 * - La clé API est stockée dans un [EncryptedSharedPreferences] adossé au
 *   keystore Android (AES-256-GCM). Elle ne quitte jamais l'appareil en mode proxy.
 * - Aucune donnée personnelle, aucun identifiant utilisateur, aucune télémétrie.
 */
class SecurePreferences(context: Context) {

    private val appContext = context.applicationContext

    private val plainPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val secretPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            SECRET_PREFS_NAME,
            MASTER_KEY_ALIAS,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Clé API YouTube Data API v3 (saisie par l'utilisateur) ou `null`. */
    var apiKey: String?
        get() = secretPrefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            secretPrefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, value.trim())
            }.apply()
        }

    /** URL du proxy auto-hébergé (vide = accès direct à l'API YouTube). */
    var proxyBaseUrl: String?
        get() = plainPrefs.getString(KEY_PROXY_URL, null)?.takeIf { it.isNotBlank() }
        set(value) {
            plainPrefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_PROXY_URL) else putString(KEY_PROXY_URL, value.trim())
            }.apply()
        }

    /** Région utilisée pour les tendances (ex. « FR », « US »). */
    var regionCode: String
        get() = plainPrefs.getString(KEY_REGION, DEFAULT_REGION) ?: DEFAULT_REGION
        set(value) = plainPrefs.edit().putString(KEY_REGION, value.uppercase()).apply()

    /** Thème : system / light / dark. */
    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(plainPrefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        set(value) = plainPrefs.edit().putString(KEY_THEME, value.name).apply()

    /** Nombre maximum d'entrées conservées dans l'historique. */
    var historyLimit: Int
        get() = plainPrefs.getInt(KEY_HISTORY_LIMIT, DEFAULT_HISTORY_LIMIT)
        set(value) = plainPrefs.edit().putInt(KEY_HISTORY_LIMIT, value).apply()

    /** Dernières recherches (stockage local uniquement). */
    var recentQueries: List<String>
        get() = plainPrefs.getString(KEY_RECENT, "")
            .orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }
        set(value) {
            plainPrefs.edit().putString(KEY_RECENT, value.take(MAX_RECENT).joinToString("\n")).apply()
        }

    fun addRecentQuery(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        val updated = (listOf(cleaned) + recentQueries.filterNot { it.equals(cleaned, true) })
            .take(MAX_RECENT)
        recentQueries = updated
    }

    fun clearRecentQueries() {
        plainPrefs.edit().remove(KEY_RECENT).apply()
    }

    fun clearAll() {
        plainPrefs.edit().clear().apply()
        runCatching { secretPrefs.edit().clear().apply() }
    }

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    companion object {
        private const val PREFS_NAME = "ytlibre_prefs"
        private const val SECRET_PREFS_NAME = "ytlibre_secrets"
        private const val MASTER_KEY_ALIAS = "ytlibre_master_key"
        private const val KEY_API_KEY = "youtube_api_key"
        private const val KEY_PROXY_URL = "proxy_base_url"
        private const val KEY_REGION = "region_code"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_HISTORY_LIMIT = "history_limit"
        private const val KEY_RECENT = "recent_queries"
        private const val DEFAULT_REGION = "FR"
        private const val DEFAULT_HISTORY_LIMIT = 200
        private const val MAX_RECENT = 8
    }
}

package com.ytlibre.app.data.remote

import com.google.gson.GsonBuilder
import com.ytlibre.app.BuildConfig
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Configuration réseau.
 *
 * - Cache HTTP disque (10 Mo) + cache mémoire TTL côté repository : les
 *   requêtes répétées ne consomment pas de quota.
 * - Un client Retrofit par URL de base (API directe ou proxy configuré).
 *
 * Les dépendances sont injectées (répertoire de cache, fournisseurs de clé et
 * de proxy) afin de rendre la classe testable unitairement.
 */
class NetworkModule(
    private val cacheDirectory: File,
    private val apiKeyProvider: () -> String?,
    private val proxyUrlProvider: () -> String?
) {

    private val gson = GsonBuilder().setLenient().create()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(cacheDirectory, "http"), CACHE_SIZE_BYTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(userAgentInterceptor())
            .addInterceptor(cacheControlInterceptor())
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    private val apis = ConcurrentHashMap<String, YouTubeDataApi>()

    /** URL de base effective : API officielle ou proxy auto-hébergé. */
    fun baseUrl(): String {
        val proxy = proxyUrlProvider()
        return if (proxy.isNullOrBlank()) BuildConfig.DEFAULT_API_BASE_URL else normalizeBaseUrl(proxy)
    }

    /** Clé API utilisée côté appareil. `null` en mode proxy (la clé reste côté serveur). */
    fun apiKey(): String? = apiKeyProvider()?.takeIf { it.isNotBlank() }

    fun isProxyMode(): Boolean = !proxyUrlProvider().isNullOrBlank()

    fun api(): YouTubeDataApi = apis.getOrPut(baseUrl()) { buildApi(baseUrl()) }

    private fun buildApi(baseUrl: String): YouTubeDataApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(YouTubeDataApi::class.java)

    private fun userAgentInterceptor() = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", BuildConfig.USER_AGENT)
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    /**
     * Mise en cache courte des réponses GET (5 minutes) : évite de consommer
     * du quota sur des rafraîchissements répétés sans servir de contenu obsolète.
     */
    private fun cacheControlInterceptor() = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.cacheResponse != null) return@Interceptor response
        response.newBuilder()
            .removeHeader("Pragma")
            .header(
                "Cache-Control",
                CacheControl.Builder().maxAge(5, TimeUnit.MINUTES).build().toString()
            )
            .build()
    }

    companion object {
        private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024

        fun normalizeBaseUrl(url: String): String {
            val trimmed = url.trim()
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }
}

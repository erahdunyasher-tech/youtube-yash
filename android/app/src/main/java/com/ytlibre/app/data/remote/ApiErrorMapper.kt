package com.ytlibre.app.data.remote

import com.google.gson.Gson
import com.ytlibre.app.data.remote.dto.ApiErrorEnvelope
import com.ytlibre.app.ui.common.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Traduit les exceptions Retrofit/OkHttp en [AppError] exploitables par l'UI.
 *
 * Points pris en charge :
 *  - quota quotidien épuisé (403 quotaExceeded / dailyLimitExceeded) ;
 *  - quota par seconde (429 + en-tête Retry-After) ;
 *  - clé invalide ou API non activée (400 / 403 keyInvalid, accessNotConfigured) ;
 *  - perte de connectivité et timeouts ;
 *  - erreur serveur YouTube (5xx).
 */
object ApiErrorMapper {

    private val gson = Gson()

    fun map(throwable: Throwable): AppError = when (throwable) {
        is HttpException -> mapHttpException(throwable)
        is SocketTimeoutException -> AppError.Timeout
        is UnknownHostException -> AppError.Network("Serveur YouTube injoignable. Vérifiez votre connexion.")
        is IOException -> AppError.Network()
        else -> AppError.Unknown(throwable.message)
    }

    fun mapHttpException(exception: HttpException): AppError {
        val code = exception.code()
        val body = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
        val envelope = body?.let {
            runCatching { gson.fromJson(it, ApiErrorEnvelope::class.java) }.getOrNull()
        }
        val reason = envelope?.error?.errors?.firstOrNull()?.reason
        val message = envelope?.error?.message

        return when {
            code == 429 -> AppError.RateLimited(retryAfterSeconds(exception))

            code == 403 && (reason == "quotaExceeded" || reason == "dailyLimitExceeded" ||
                reason == "rateLimitExceeded") ->
                AppError.QuotaExceeded(resetHint = quotaResetHint(message))

            code == 403 && (reason == "keyInvalid" || reason == "badRequest" ||
                reason == "accessNotConfigured" || reason == "forbidden") ->
                AppError.InvalidApiKey(message)

            code == 400 && reason == "keyInvalid" -> AppError.InvalidApiKey(message)

            code == 401 -> AppError.InvalidApiKey(message)

            code in 500..599 -> AppError.ApiFailure(code, "Service YouTube momentanément indisponible.")

            code == 404 -> AppError.ApiFailure(code, "Ressource introuvable.")

            else -> AppError.ApiFailure(code, message)
        }
    }

    private fun retryAfterSeconds(exception: HttpException): Int? =
        exception.response()?.headers()?.get("Retry-After")?.toIntOrNull()

    private fun quotaResetHint(message: String?): String? =
        message?.takeIf { it.contains("quota", ignoreCase = true) || it.contains("limite", ignoreCase = true) }
}

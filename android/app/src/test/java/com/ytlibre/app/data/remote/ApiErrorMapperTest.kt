package com.ytlibre.app.data.remote

import com.ytlibre.app.ui.common.AppError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Tests de la traduction des erreurs HTTP en messages actionnables
 * (quota, clé invalide, indisponibilité, limitation de débit).
 */
class ApiErrorMapperTest {

    /**
     * Construit une réponse d'erreur rétographique.
     * Retrofit 2.11 n'expose plus `Response.Builder` : on assemble la réponse
     * HTTP brute (okhttp) puis on la passe à `Response.error(body, rawResponse)`,
     * seule fabrique qui accepte des en-têtes personnalisés (Retry-After, etc.).
     */
    private fun httpException(code: Int, body: String, headers: Map<String, String> = emptyMap()): HttpException {
        val rawResponse = okhttp3.Response.Builder()
            .request(
                okhttp3.Request.Builder()
                    .url("https://www.googleapis.com/youtube/v3/search")
                    .build()
            )
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()

        val errorBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(errorBody, rawResponse))
    }

    @Test
    fun quotaExceeded_isDetected() {
        val body = """
            {"error":{"code":403,"message":"The request cannot be completed because you have exceeded your quota.",
            "errors":[{"reason":"quotaExceeded","domain":"youtube.quota"}]}}
        """.trimIndent()
        val error = ApiErrorMapper.mapHttpException(httpException(403, body))
        assertTrue(error is AppError.QuotaExceeded)
    }

    @Test
    fun dailyLimitExceeded_isDetected() {
        val body = """
            {"error":{"code":403,"message":"Daily Limit Exceeded","errors":[{"reason":"dailyLimitExceeded"}]}}
        """.trimIndent()
        val error = ApiErrorMapper.mapHttpException(httpException(403, body))
        assertTrue(error is AppError.QuotaExceeded)
    }

    @Test
    fun invalidKey_isDetected() {
        val body = """
            {"error":{"code":403,"message":"The API key is invalid.","errors":[{"reason":"keyInvalid"}]}}
        """.trimIndent()
        val error = ApiErrorMapper.mapHttpException(httpException(403, body))
        assertTrue(error is AppError.InvalidApiKey)
    }

    @Test
    fun tooManyRequests_keepsRetryAfter() {
        val body = """{"error":{"code":429,"message":"Too many requests"}}"""
        val error = ApiErrorMapper.mapHttpException(
            httpException(429, body, mapOf("Retry-After" to "42"))
        )
        assertTrue(error is AppError.RateLimited)
        assertEquals(42, (error as AppError.RateLimited).retryAfterSeconds)
    }

    @Test
    fun serverError_isMappedToApiFailure() {
        val error = ApiErrorMapper.mapHttpException(httpException(503, "{}"))
        assertTrue(error is AppError.ApiFailure)
        assertEquals(503, (error as AppError.ApiFailure).code)
    }

    @Test
    fun networkExceptions_areMapped() {
        assertTrue(ApiErrorMapper.map(java.net.UnknownHostException("dns")) is AppError.Network)
        assertTrue(ApiErrorMapper.map(java.net.SocketTimeoutException("timeout")) is AppError.Timeout)
    }
}

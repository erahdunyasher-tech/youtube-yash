package com.ytlibre.app.ui.common

/**
 * Erreurs applicatives normalisées, exposées à l'interface.
 * Le message utilisateur est toujours en français et actionnable.
 */
sealed class AppError(open val userMessage: String) {

    /** Aucune clé API fournie : l'utilisateur doit en saisir une ou configurer un proxy. */
    data object MissingApiKey : AppError(
        "Aucune clé API YouTube n'est configurée. Ajoutez-la dans Réglages ou " +
            "activez un proxy serveur pour limiter la consommation de quota."
    )

    data class Network(
        override val userMessage: String = "Connexion impossible. Vérifiez votre réseau."
    ) : AppError(userMessage)

    data object Timeout : AppError("Le réseau est trop lent. Réessayez dans un instant.")

    /** Quota quotidien de l'API YouTube épuisé (403 quotaExceeded / dailyLimitExceeded). */
    data class QuotaExceeded(val resetHint: String? = null) : AppError(
        resetHint?.let { "Quota de l'API YouTube épuisé. $it" }
            ?: "Quota de l'API YouTube épuisé. Le quota quotidien se renouvelle chaque jour " +
                "(minuit, heure de Pacifique). Vos favoris, votre historique et vos recherches " +
                "récentes restent consultables."
    )

    /** Quota dépassé par seconde (429). */
    data class RateLimited(val retryAfterSeconds: Int?) : AppError(
        retryAfterSeconds?.let { "Trop de requêtes. Nouvelle tentative possible dans ${it}s." }
            ?: "Trop de requêtes envoyées à l'API YouTube. Patientez quelques instants."
    )

    /** Clé refusée (400 badRequest / 403 keyInvalid / 403 accessNotConfigured). */
    data class InvalidApiKey(val detail: String? = null) : AppError(
        detail?.let { "Clé API refusée : $it" }
            ?: "Clé API refusée par YouTube. Vérifiez qu'elle est valide et que l'API " +
            "« YouTube Data API v3 » est activée."
    )

    data class ApiFailure(val code: Int, val detail: String? = null) : AppError(
        detail?.let { "Erreur de l'API YouTube ($code) : $it" }
            ?: "Erreur de l'API YouTube ($code)."
    )

    data class Unknown(val detail: String? = null) : AppError(
        detail?.let { "Une erreur inattendue est survenue : $it" }
            ?: "Une erreur inattendue est survenue."
    )
}

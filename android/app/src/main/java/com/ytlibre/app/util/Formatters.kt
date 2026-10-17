package com.ytlibre.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Formatage des données YouTube (durée ISO-8601, nombres de vues, dates).
 * Toutes les fonctions sont pures et testables unitairement.
 */
object Formatters {

    private val french = Locale.FRENCH
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", french)

    /** Convertit une durée ISO-8601 YouTube (« PT1H2M3S ») en secondes. */
    fun parseDurationSeconds(isoDuration: String?): Int {
        if (isoDuration.isNullOrBlank()) return 0
        return runCatching {
            Duration.parse(isoDuration).seconds.coerceAtLeast(0L).toInt()
        }.getOrDefault(0)
    }

    /** « 1:02:03 » ou « 2:03 » ; « LIVE » si la durée est nulle. */
    fun formatDuration(totalSeconds: Int, liveLabel: String = "LIVE"): String {
        if (totalSeconds <= 0) return liveLabel
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(french, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(french, "%d:%02d", minutes, seconds)
        }
    }

    /** « 1 234 567 » → « 1,2 M de vues ». */
    fun formatViews(count: Long): String {
        if (count <= 0) return "0 vue"
        return when {
            count >= 1_000_000_000 -> String.format(french, "%.1f Md de vues", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format(french, "%.1f M de vues", count / 1_000_000.0)
            count >= 1_000 -> String.format(french, "%.1f k de vues", count / 1_000.0)
            else -> "$count vue${if (count > 1) "s" else ""}"
        }
    }

    /** « 12,3 k abonnés ». */
    fun formatSubscribers(count: Long): String = when {
        count <= 0 -> "Abonnés masqués"
        count >= 1_000_000 -> String.format(french, "%.1f M d'abonnés", count / 1_000_000.0)
        count >= 1_000 -> String.format(french, "%.1f k d'abonnés", count / 1_000.0)
        else -> "$count abonné${if (count > 1) "s" else ""}"
    }

    /** Date relative (« il y a 3 jours ») à partir d'un timestamp ISO-8601. */
    fun formatRelativeDate(isoDate: String?, now: Instant = Instant.now()): String {
        val instant = parseInstant(isoDate) ?: return ""
        val seconds = (now.epochSecond - instant.epochSecond).coerceAtLeast(0)
        val days = seconds / 86_400
        return when {
            days >= 365 -> "il y a ${days / 365} an${if (days / 365 > 1) "s" else ""}"
            days >= 30 -> "il y a ${days / 30} mois"
            days >= 7 -> "il y a ${days / 7} semaine${if (days / 7 > 1) "s" else ""}"
            days >= 1 -> "il y a $days jour${if (days > 1) "s" else ""}"
            seconds >= 3600 -> "il y a ${seconds / 3600} h"
            seconds >= 60 -> "il y a ${seconds / 60} min"
            else -> "à l'instant"
        }
    }

    /** « 12 mars 2024 ». */
    fun formatDate(isoDate: String?): String {
        val instant = parseInstant(isoDate) ?: return ""
        return runCatching {
            LocalDate.ofInstant(instant, ZoneId.systemDefault()).format(dateFormatter)
        }.getOrDefault("")
    }

    fun parseInstant(isoDate: String?): Instant? {
        if (isoDate.isNullOrBlank()) return null
        return try {
            Instant.parse(isoDate)
        } catch (e: DateTimeParseException) {
            try {
                LocalDate.parse(isoDate).atStartOfDay(ZoneId.of("UTC")).toInstant()
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }

    /** Supprime les accents / caractères spéciaux d'un texte de recherche. */
    fun normalizeQuery(query: String): String = query.trim().replace(Regex("\\s+"), " ")
}

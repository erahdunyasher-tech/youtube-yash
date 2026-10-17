package com.ytlibre.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Tests unitaires des formateurs (durée, vues, dates).
 */
class FormattersTest {

    @Test
    fun parseDurationSeconds_handlesIso8601() {
        assertEquals(3723, Formatters.parseDurationSeconds("PT1H2M3S"))
        assertEquals(123, Formatters.parseDurationSeconds("PT2M3S"))
        assertEquals(45, Formatters.parseDurationSeconds("PT45S"))
    }

    @Test
    fun parseDurationSeconds_returnsZeroForInvalidInput() {
        assertEquals(0, Formatters.parseDurationSeconds(null))
        assertEquals(0, Formatters.parseDurationSeconds(""))
        assertEquals(0, Formatters.parseDurationSeconds("pas une durée"))
    }

    @Test
    fun formatDuration_formatsHoursAndMinutes() {
        assertEquals("1:02:03", Formatters.formatDuration(3723))
        assertEquals("2:03", Formatters.formatDuration(123))
        assertEquals("LIVE", Formatters.formatDuration(0))
    }

    @Test
    fun formatViews_usesFrenchCompactNotation() {
        assertTrue(Formatters.formatViews(1_500_000).startsWith("1,5 M"))
        assertTrue(Formatters.formatViews(2_400).startsWith("2,4 k"))
        assertEquals("0 vue", Formatters.formatViews(0))
    }

    @Test
    fun formatRelativeDate_computesElapsedTime() {
        val now = Instant.parse("2024-05-10T12:00:00Z")
        assertEquals("il y a 3 jours", Formatters.formatRelativeDate("2024-05-07T12:00:00Z", now))
        assertEquals("il y a 2 mois", Formatters.formatRelativeDate("2024-03-01T12:00:00Z", now))
        assertEquals("", Formatters.formatRelativeDate(null, now))
    }

    @Test
    fun normalizeQuery_trimsAndCollapsesSpaces() {
        assertEquals("test kotlin", Formatters.normalizeQuery("  test   kotlin  "))
    }
}

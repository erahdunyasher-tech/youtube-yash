package com.ytlibre.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentés de la base locale (favoris et historique).
 */
@RunWith(AndroidJUnit4::class)
class LibraryDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun favorite_isStoredAndRemoved() = runBlocking {
        val dao = database.favoriteDao()
        dao.upsert(
            FavoriteEntity(
                videoId = "v1",
                title = "Vidéo",
                channelId = "c1",
                channelTitle = "Chaîne",
                thumbnailUrl = "https://img.test/t.jpg",
                durationSeconds = 120,
                publishedAt = "2024-01-01T00:00:00Z",
                addedAt = 1_000L
            )
        )

        assertEquals("Vidéo", dao.findById("v1")?.title)
        assertTrue(dao.observeIsFavorite("v1").first())

        dao.deleteById("v1")
        assertNull(dao.findById("v1"))
        assertFalse(dao.observeIsFavorite("v1").first())
    }

    @Test
    fun favorites_areOrderedByMostRecent() = runBlocking {
        val dao = database.favoriteDao()
        dao.upsert(entity("v1", addedAt = 1_000L))
        dao.upsert(entity("v2", addedAt = 5_000L))

        val all = dao.observeAll().first()
        assertEquals(listOf("v2", "v1"), all.map { it.videoId })
    }

    @Test
    fun history_isTrimmedToConfiguredLimit() = runBlocking {
        val dao = database.historyDao()
        repeat(5) { index ->
            dao.upsert(
                HistoryEntity(
                    videoId = "h$index",
                    title = "Vidéo $index",
                    channelId = "c",
                    channelTitle = "Chaîne",
                    thumbnailUrl = "",
                    durationSeconds = 10,
                    publishedAt = "",
                    watchedAt = index.toLong()
                )
            )
        }

        dao.trimTo(3)

        val all = dao.observeRecent(10).first()
        assertEquals(3, all.size)
        assertEquals("h4", all.first().videoId)
    }

    private fun entity(videoId: String, addedAt: Long) = FavoriteEntity(
        videoId = videoId,
        title = "Vidéo",
        channelId = "c1",
        channelTitle = "Chaîne",
        thumbnailUrl = "",
        durationSeconds = 60,
        publishedAt = "",
        addedAt = addedAt
    )
}

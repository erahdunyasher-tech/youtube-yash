package com.ytlibre.app.data.repository

import com.ytlibre.app.data.local.SearchCacheDao
import com.ytlibre.app.data.local.SearchCacheEntity
import com.ytlibre.app.data.model.SearchFilter
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.data.remote.NetworkModule
import com.ytlibre.app.ui.common.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests du repository : requêtes envoyées à la Data API, parsing des
 * résultats et gestion des erreurs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YouTubeRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var network: NetworkModule
    private lateinit var repository: YouTubeRepository
    private lateinit var testDispatcher: UnconfinedTestDispatcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        network = NetworkModule(
            cacheDirectory = temporaryFolder.newFolder("cache"),
            apiKeyProvider = { "TEST_API_KEY" },
            proxyUrlProvider = { server.url("/youtube/v3/").toString() }
        )
        testDispatcher = UnconfinedTestDispatcher()
        repository = YouTubeRepository(network, testDispatcher)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun search_sendsApiKeyAndMapsVideos() = runTest {
        server.enqueue(jsonResponse(SEARCH_PAYLOAD))
        server.enqueue(jsonResponse(VIDEOS_PAYLOAD))

        val result = repository.search("kotlin", SearchFilter.VIDEOS)

        assertTrue(result is AppResult.Success)
        val page = (result as AppResult.Success).data
        assertEquals(1, page.items.size)
        val video = (page.items.first() as SearchResultItem.VideoItem).video
        assertEquals("vid123", video.id)
        assertEquals("Chaîne Test", video.channelTitle)
        assertEquals(125, video.durationSeconds)
        assertEquals(1500L, video.viewCount)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("key=TEST_API_KEY"))
        assertTrue(request.path!!.contains("type=video"))
        assertTrue(request.path!!.contains("q=kotlin"))
    }

    @Test
    fun search_mapsApiErrorToQuotaError() = runTest {
        server.enqueue(
            jsonResponse(
                """{"error":{"code":403,"message":"quota","errors":[{"reason":"quotaExceeded"}]}}""",
                code = 403
            )
        )

        val result = repository.search("kotlin", SearchFilter.ALL)

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error.toString().contains("Quota"))
    }

    @Test
    fun secondIdenticalSearch_isServedFromMemoryCache() = runTest {
        server.enqueue(jsonResponse(SEARCH_PAYLOAD))
        server.enqueue(jsonResponse(VIDEOS_PAYLOAD))

        repository.search("cache-test", SearchFilter.VIDEOS)
        val requestCountAfterFirst = server.requestCount
        repository.search("cache-test", SearchFilter.VIDEOS)

        assertEquals(requestCountAfterFirst, server.requestCount)
    }

    @Test
    fun search_result_isPersistedAndServedWithoutNewRequest() = runTest {
        val dao = FakeSearchCacheDao()
        repository = YouTubeRepository(network, testDispatcher, searchCacheDao = dao)
        server.enqueue(jsonResponse(SEARCH_PAYLOAD))
        server.enqueue(jsonResponse(VIDEOS_PAYLOAD))

        val first = repository.search("persist-test", SearchFilter.VIDEOS)
        assertTrue(first is AppResult.Success)
        assertEquals(null, (first as AppResult.Success).data.cachedAt)
        assertEquals(1, dao.store.size)

        // Nouvelle instance du repository = cache mémoire vide (redémarrage),
        // même cache persistant (Room) partagé.
        val restarted = YouTubeRepository(network, testDispatcher, searchCacheDao = dao)
        val requestCount = server.requestCount

        val second = restarted.search("persist-test", SearchFilter.VIDEOS)

        assertTrue(second is AppResult.Success)
        assertEquals(requestCount, server.requestCount)
        assertNotNull((second as AppResult.Success).data.cachedAt)
        assertNotNull(restarted.cachedSearchPage("persist-test", SearchFilter.VIDEOS))
    }

    /** Double en mémoire de [SearchCacheDao] (Room) pour les tests. */
    private class FakeSearchCacheDao : SearchCacheDao {
        val store = linkedMapOf<String, SearchCacheEntity>()

        override suspend fun get(cacheKey: String): SearchCacheEntity? = store[cacheKey]

        override suspend fun put(entity: SearchCacheEntity) {
            store[entity.cacheKey] = entity
        }

        override suspend fun prune(keep: Int) {
            while (store.size > keep) store.remove(store.keys.first())
        }

        override suspend fun clear() {
            store.clear()
        }
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    companion object {
        private val SEARCH_PAYLOAD = """
            {
              "nextPageToken": "TOKEN2",
              "pageInfo": {"totalResults": 1, "resultsPerPage": 1},
              "items": [
                {
                  "kind": "youtube#searchResult",
                  "id": {"kind": "youtube#video", "videoId": "vid123"},
                  "snippet": {
                    "title": "Vidéo de test",
                    "description": "Description",
                    "channelTitle": "Chaîne Test",
                    "channelId": "chan123",
                    "publishedAt": "2024-04-01T10:00:00Z",
                    "thumbnails": {"high": {"url": "https://img.test/high.jpg"}}
                  }
                }
              ]
            }
        """.trimIndent()

        private val VIDEOS_PAYLOAD = """
            {
              "items": [
                {
                  "id": "vid123",
                  "contentDetails": {"duration": "PT2M5S"},
                  "statistics": {"viewCount": "1500", "likeCount": "42"}
                }
              ]
            }
        """.trimIndent()
    }
}

package com.ytlibre.app.data.repository

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
    private lateinit var repository: YouTubeRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val network = NetworkModule(
            cacheDirectory = temporaryFolder.newFolder("cache"),
            apiKeyProvider = { "TEST_API_KEY" },
            proxyUrlProvider = { server.url("/youtube/v3/").toString() }
        )
        repository = YouTubeRepository(network, UnconfinedTestDispatcher())
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

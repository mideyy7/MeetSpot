package com.meetspot.app.data.repository

import com.meetspot.app.data.remote.ApiClient
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises [RecommendRepositoryImpl] against a real HTTP server (MockWebServer),
 * verifying the Retrofit/kotlinx.serialization wiring against response shapes
 * that mirror `buildRecommendations`'s actual output in lib/recommend.js.
 */
class RecommendRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RecommendRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiService = ApiClient.buildApiService(server.url("/").toString(), OkHttpClient())
        repository = RecommendRepositoryImpl(apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `recommendForPair parses a full response with weather and AI decision`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "origins": [{"address":"UCL","location":{"lat":51.5,"lng":-0.13}}],
                  "weather": {"condition":"Cloudy","temperatureC":14.2,"rainChance":20,"windKph":10,"difficult":false,"advice":"Fine outside"},
                  "recommendations": [
                    {
                      "id":"place1","name":"Blue Bottle","address":"1 Main St","rating":4.5,"ratingCount":120,
                      "journeys":[{"minutes":12,"distanceKm":1.2},{"minutes":15,"distanceKm":1.8}],
                      "longest":15,"difference":3,"score":21.5,"explanation":"A fair option"
                    }
                  ],
                  "aiDecision": {"selectedPlaceId":"place1","selectedPlace":"Blue Bottle","headline":"Great pick","reason":"Fits everyone","tradeoff":"Slightly pricier","model":"gemini-3.6-flash"},
                  "maxMinutes": 30,
                  "travelMode": "TRANSIT"
                }
                """.trimIndent()
            )
        )

        val result = repository.recommendForPair("UCL", "London Bridge", "coffee", "2026-01-01T10:00:00Z", "TRANSIT", 30)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(1, response.recommendations.size)
        assertEquals("Blue Bottle", response.recommendations.first().name)
        assertEquals("place1", response.aiDecision?.selectedPlaceId)
        assertEquals(20, response.weather?.rainChance)
    }

    @Test
    fun `recommendForPair handles the empty-results shape when no places match`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"origins":[],"weather":null,"recommendations":[]}"""
            )
        )

        val result = repository.recommendForPair("A", "B", "sushi", "2026-01-01T10:00:00Z", "WALK", 20)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().recommendations.isEmpty())
    }

    @Test
    fun `a 400 response surfaces the server error field`() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"Choose a meeting time within the next 9 days"}"""))

        val result = repository.recommendForPair("A", "B", "sushi", "invalid", "WALK", 20)

        assertTrue(result.isFailure)
        assertEquals("Choose a meeting time within the next 9 days", result.exceptionOrNull()?.message)
    }

    @Test
    fun `fetchMeeting parses a waiting invite`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"id":"tok123","organizerName":"Abdul","guestName":null,"query":"coffee","travelMode":"TRANSIT","maxMinutes":30,"meetingTime":"2026-01-01T10:00:00Z","status":"waiting","results":null}"""
            )
        )

        val result = repository.fetchMeeting("tok123")

        assertTrue(result.isSuccess)
        assertEquals("waiting", result.getOrThrow().status)
        assertEquals("Abdul", result.getOrThrow().organizerName)
    }

    @Test
    fun `joinMeeting returns ready status with results`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"status":"ready","results":{"origins":[],"weather":null,"recommendations":[]}}"""
            )
        )

        val result = repository.joinMeeting("tok123", "Sarah", "London Bridge")

        assertTrue(result.isSuccess)
        assertEquals("ready", result.getOrThrow().status)
    }
}

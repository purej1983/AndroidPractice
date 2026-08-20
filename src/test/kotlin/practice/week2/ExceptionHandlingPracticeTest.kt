package practice.week2

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExceptionHandlingPracticeTest {

    private val thomas = HomeUser("u1", "Thomas")
    private val inbox = listOf(
        InboxMessage("m1", "Standup at 10"),
        InboxMessage("m2", "PR ready")
    )
    private val forecast = WeatherForecast("Hong Kong", "Cloudy")

    @Test
    fun `loadUser returns the user`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))

        assertEquals(thomas, ExceptionHandlingPractice.loadUser(userApi, "u1"))
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(listOf("u1"), userApi.completedIds)
    }

    @Test
    fun `loadUser throws HomeUserLoadException when the user is missing`() = runTest {
        val userApi = FakeHomeUserApi()

        val error = assertFailsWith<HomeUserLoadException> {
            ExceptionHandlingPractice.loadUser(userApi, "u1")
        }

        assertEquals("u1", error.userId)
    }

    @Test
    fun `loadUser suspends for the given duration`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)

        assertEquals(thomas, ExceptionHandlingPractice.loadUser(userApi, "u1"))
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadHomeSequential returns user messages and weather`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox))
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        assertEquals(
            HomeScreen(thomas, inbox, forecast),
            ExceptionHandlingPractice.loadHomeSequential(userApi, messagesApi, weatherApi, "u1")
        )
    }

    @Test
    fun `loadHomeSequential returns empty messages when none exist`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))
        val messagesApi = FakeMessagesApi()
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        assertEquals(
            HomeScreen(thomas, emptyList(), forecast),
            ExceptionHandlingPractice.loadHomeSequential(userApi, messagesApi, weatherApi, "u1")
        )
    }

    @Test
    fun `loadHomeSequential fetches one after another so delays add up`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val screen = ExceptionHandlingPractice.loadHomeSequential(
            userApi,
            messagesApi,
            weatherApi,
            "u1"
        )

        assertEquals(HomeScreen(thomas, inbox, forecast), screen)
        assertEquals(3_000, currentTime)
    }

    @Test
    fun `loadHomeSequential does not fetch messages or weather when the user fails`() = runTest {
        val userApi = FakeHomeUserApi()
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox))
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        val error = assertFailsWith<HomeUserLoadException> {
            ExceptionHandlingPractice.loadHomeSequential(userApi, messagesApi, weatherApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(emptyList(), messagesApi.fetchedIds)
        assertEquals(emptyList(), weatherApi.fetchedIds)
    }

    @Test
    fun `loadHomeSequential does not fetch weather when messages fail`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))
        val messagesApi = FakeMessagesApi(
            failures = mapOf("u1" to MessagesLoadException("u1"))
        )
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        val error = assertFailsWith<MessagesLoadException> {
            ExceptionHandlingPractice.loadHomeSequential(userApi, messagesApi, weatherApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(emptyList(), weatherApi.fetchedIds)
    }

    @Test
    fun `loadHomeStrict returns user messages and weather`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox))
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        assertEquals(
            HomeScreen(thomas, inbox, forecast),
            ExceptionHandlingPractice.loadHomeStrict(userApi, messagesApi, weatherApi, "u1")
        )
    }

    @Test
    fun `loadHomeStrict overlaps independent work`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val screen = ExceptionHandlingPractice.loadHomeStrict(userApi, messagesApi, weatherApi, "u1")

        assertEquals(HomeScreen(thomas, inbox, forecast), screen)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadHomeStrict total time is the slowest call`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 500)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 2_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val screen = ExceptionHandlingPractice.loadHomeStrict(userApi, messagesApi, weatherApi, "u1")

        assertEquals(HomeScreen(thomas, inbox, forecast), screen)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `loadHomeStrict cancels the user load when weather fails`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 5_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 5_000)
        val weatherApi = FakeWeatherApi(
            delayMillis = 200,
            failures = mapOf("u1" to WeatherLoadException("u1"))
        )

        val error = assertFailsWith<WeatherLoadException> {
            ExceptionHandlingPractice.loadHomeStrict(userApi, messagesApi, weatherApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(emptyList(), userApi.completedIds)
        assertEquals(listOf("u1"), messagesApi.fetchedIds)
        assertEquals(emptyList(), messagesApi.completedIds)
        assertEquals(200, currentTime)
    }

    @Test
    fun `loadHomeStrict cancels weather when the user fails`() = runTest {
        val userApi = FakeHomeUserApi(
            delayMillis = 200,
            failures = mapOf("u1" to HomeUserLoadException("u1"))
        )
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 5_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 5_000)

        val error = assertFailsWith<HomeUserLoadException> {
            ExceptionHandlingPractice.loadHomeStrict(userApi, messagesApi, weatherApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(listOf("u1"), weatherApi.fetchedIds)
        assertEquals(emptyList(), weatherApi.completedIds)
        assertEquals(200, currentTime)
    }

    @Test
    fun `loadHomeOrNull returns the screen when every child succeeds`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox))
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        assertEquals(
            HomeScreen(thomas, inbox, forecast),
            ExceptionHandlingPractice.loadHomeOrNull(userApi, messagesApi, weatherApi, "u1")
        )
    }

    @Test
    fun `loadHomeOrNull returns null when weather fails`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 5_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 5_000)
        val weatherApi = FakeWeatherApi(
            delayMillis = 200,
            failures = mapOf("u1" to WeatherLoadException("u1"))
        )

        assertNull(
            ExceptionHandlingPractice.loadHomeOrNull(userApi, messagesApi, weatherApi, "u1")
        )
        assertEquals(emptyList(), userApi.completedIds)
        assertEquals(emptyList(), messagesApi.completedIds)
        assertEquals(200, currentTime)
    }

    @Test
    fun `loadHomeOrNull returns null when the user fails`() = runTest {
        val userApi = FakeHomeUserApi(
            delayMillis = 200,
            failures = mapOf("u1" to HomeUserLoadException("u1"))
        )
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 5_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 5_000)

        assertNull(
            ExceptionHandlingPractice.loadHomeOrNull(userApi, messagesApi, weatherApi, "u1")
        )
        assertEquals(emptyList(), weatherApi.completedIds)
    }

    @Test
    fun `loadHomeOrNull does not return null when cancelled`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 5_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 5_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 5_000)
        var result: HomeScreen? = null
        var returned = false

        val job = launch {
            result = ExceptionHandlingPractice.loadHomeOrNull(
                userApi,
                messagesApi,
                weatherApi,
                "u1"
            )
            returned = true
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertNull(result)
        assertFalse(returned)
        assertEquals(listOf("u1"), userApi.fetchedIds)
    }

    @Test
    fun `loadHomeAllowWeatherFailure returns the screen when every child succeeds`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas))
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox))
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))

        assertEquals(
            HomeScreen(thomas, inbox, forecast),
            ExceptionHandlingPractice.loadHomeAllowWeatherFailure(
                userApi,
                messagesApi,
                weatherApi,
                "u1"
            )
        )
    }

    @Test
    fun `loadHomeAllowWeatherFailure overlaps independent work`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val screen = ExceptionHandlingPractice.loadHomeAllowWeatherFailure(
            userApi,
            messagesApi,
            weatherApi,
            "u1"
        )

        assertEquals(HomeScreen(thomas, inbox, forecast), screen)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadHomeAllowWeatherFailure keeps user and messages when weather fails`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(
            delayMillis = 200,
            failures = mapOf("u1" to WeatherLoadException("u1"))
        )

        val screen = ExceptionHandlingPractice.loadHomeAllowWeatherFailure(
            userApi,
            messagesApi,
            weatherApi,
            "u1"
        )

        assertEquals(HomeScreen(thomas, inbox, null), screen)
        assertEquals(listOf("u1"), userApi.completedIds)
        assertEquals(listOf("u1"), messagesApi.completedIds)
        assertEquals(emptyList(), weatherApi.completedIds)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadHomeAllowWeatherFailure still fails when the user fails`() = runTest {
        val userApi = FakeHomeUserApi(
            delayMillis = 200,
            failures = mapOf("u1" to HomeUserLoadException("u1"))
        )
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val error = assertFailsWith<HomeUserLoadException> {
            ExceptionHandlingPractice.loadHomeAllowWeatherFailure(
                userApi,
                messagesApi,
                weatherApi,
                "u1"
            )
        }

        assertEquals("u1", error.userId)
    }

    @Test
    fun `loadHomeAllowWeatherFailure still fails when messages fail`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val messagesApi = FakeMessagesApi(
            delayMillis = 200,
            failures = mapOf("u1" to MessagesLoadException("u1"))
        )
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val error = assertFailsWith<MessagesLoadException> {
            ExceptionHandlingPractice.loadHomeAllowWeatherFailure(
                userApi,
                messagesApi,
                weatherApi,
                "u1"
            )
        }

        assertEquals("u1", error.userId)
    }

    @Test
    fun `loadHomeAwaitWeatherFirst still overlaps both remaining calls`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 400)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 200)

        val screen = ExceptionHandlingPractice.loadHomeAwaitWeatherFirst(
            userApi,
            messagesApi,
            weatherApi,
            "u1"
        )

        assertEquals(HomeScreen(thomas, inbox, forecast), screen)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadHomeAwaitWeatherFirst cancels remaining children when weather fails`() = runTest {
        val userApi = FakeHomeUserApi(users = mapOf("u1" to thomas), delayMillis = 5_000)
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 5_000)
        val weatherApi = FakeWeatherApi(
            delayMillis = 200,
            failures = mapOf("u1" to WeatherLoadException("u1"))
        )

        val error = assertFailsWith<WeatherLoadException> {
            ExceptionHandlingPractice.loadHomeAwaitWeatherFirst(
                userApi,
                messagesApi,
                weatherApi,
                "u1"
            )
        }

        assertEquals("u1", error.userId)
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(emptyList(), userApi.completedIds)
        assertEquals(listOf("u1"), messagesApi.fetchedIds)
        assertEquals(emptyList(), messagesApi.completedIds)
        assertEquals(200, currentTime)
    }

    @Test
    fun `startIndependentLoads delivers messages and weather`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, error -> exceptions += error }
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox))
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast))
        var received: List<InboxMessage>? = null

        val loads = ExceptionHandlingPractice.startIndependentLoads(
            parent = this,
            handler = handler,
            messagesApi = messagesApi,
            weatherApi = weatherApi,
            userId = "u1"
        ) { received = it }
        loads.messagesJob.join()
        loads.weatherJob.join()

        assertEquals(inbox, received)
        assertEquals(listOf("u1"), messagesApi.completedIds)
        assertEquals(listOf("u1"), weatherApi.completedIds)
        assertEquals(emptyList(), exceptions)
        assertTrue(loads.messagesJob.isCompleted)
        assertTrue(loads.weatherJob.isCompleted)
    }

    @Test
    fun `startIndependentLoads keeps messages running when weather fails`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, error -> exceptions += error }
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(
            delayMillis = 200,
            failures = mapOf("u1" to WeatherLoadException("u1"))
        )
        var received: List<InboxMessage>? = null

        val loads = ExceptionHandlingPractice.startIndependentLoads(
            parent = this,
            handler = handler,
            messagesApi = messagesApi,
            weatherApi = weatherApi,
            userId = "u1"
        ) { received = it }
        loads.messagesJob.join()
        loads.weatherJob.join()

        assertEquals(inbox, received)
        assertEquals(listOf("u1"), messagesApi.completedIds)
        assertEquals(emptyList(), weatherApi.completedIds)
        assertTrue(exceptions.any { it is WeatherLoadException })
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `startIndependentLoads overlaps messages and weather`() = runTest {
        val handler = CoroutineExceptionHandler { _, _ -> }
        val messagesApi = FakeMessagesApi(messages = mapOf("u1" to inbox), delayMillis = 1_000)
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)
        var received: List<InboxMessage>? = null

        val loads = ExceptionHandlingPractice.startIndependentLoads(
            parent = this,
            handler = handler,
            messagesApi = messagesApi,
            weatherApi = weatherApi,
            userId = "u1"
        ) { received = it }
        loads.messagesJob.join()
        loads.weatherJob.join()

        assertEquals(inbox, received)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `weatherDeferred returns the forecast when awaited`() = runTest {
        val weatherApi = FakeWeatherApi(forecasts = mapOf("u1" to forecast), delayMillis = 1_000)

        val deferred = ExceptionHandlingPractice.weatherDeferred(this, weatherApi, "u1")

        assertEquals(forecast, deferred.await())
        assertEquals(1_000, currentTime)
        assertEquals(listOf("u1"), weatherApi.completedIds)
    }

    @Test
    fun `weatherDeferred does not throw until await`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, error -> exceptions += error }
        val scope = CoroutineScope(coroutineContext + SupervisorJob() + handler)
        val weatherApi = FakeWeatherApi(
            delayMillis = 1_000,
            failures = mapOf("u1" to WeatherLoadException("u1"))
        )

        val deferred = ExceptionHandlingPractice.weatherDeferred(scope, weatherApi, "u1")
        testScheduler.advanceUntilIdle()

        assertTrue(deferred.isCompleted)
        assertEquals(listOf("u1"), weatherApi.fetchedIds)
        assertEquals(emptyList(), weatherApi.completedIds)

        val error = assertFailsWith<WeatherLoadException> {
            deferred.await()
        }

        assertEquals("u1", error.userId)
        assertEquals(1_000, currentTime)
    }
}

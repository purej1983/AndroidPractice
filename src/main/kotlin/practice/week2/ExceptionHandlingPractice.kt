package practice.week2

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Day 9 — Exception handling and supervision.
 *
 * Key ideas:
 * - `coroutineScope` fails when a child fails, and cancels the siblings.
 * - Catching that exception does not keep the siblings running.
 * - `supervisorScope` lets siblings finish when one child fails.
 * - If the `supervisorScope` body itself fails (for example `await` without
 *   a catch), remaining children are still cancelled.
 * - `SupervisorJob` is the same rule for a long-lived `CoroutineScope`.
 * - `async` holds the exception until `await`. `launch` sends it to
 *   `CoroutineExceptionHandler`.
 */
data class HomeUser(
    val id: String,
    val name: String
)

data class InboxMessage(
    val id: String,
    val text: String
)

data class WeatherForecast(
    val city: String,
    val summary: String
)

data class HomeScreen(
    val user: HomeUser,
    val messages: List<InboxMessage>,
    val weather: WeatherForecast?
)

data class IndependentLoads(
    val messagesJob: Job,
    val weatherJob: Job
)

class HomeUserLoadException(val userId: String) : IllegalStateException("Failed to load user: $userId")

class MessagesLoadException(val userId: String) : IllegalStateException("Failed to load messages: $userId")

class WeatherLoadException(val userId: String) : IllegalStateException("Failed to load weather: $userId")

class FakeHomeUserApi(
    private val users: Map<String, HomeUser> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val delays: Map<String, Long> = emptyMap(),
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()

    suspend fun fetchUser(id: String): HomeUser {
        _fetchedIds += id
        delay(delays[id] ?: delayMillis)
        failures[id]?.let { throw it }
        val user = users[id] ?: throw HomeUserLoadException(id)
        _completedIds += id
        return user
    }
}

class FakeMessagesApi(
    private val messages: Map<String, List<InboxMessage>> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val delays: Map<String, Long> = emptyMap(),
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()

    suspend fun fetchMessages(userId: String): List<InboxMessage> {
        _fetchedIds += userId
        delay(delays[userId] ?: delayMillis)
        failures[userId]?.let { throw it }
        val result = messages[userId] ?: emptyList()
        _completedIds += userId
        return result
    }
}

class FakeWeatherApi(
    private val forecasts: Map<String, WeatherForecast> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val delays: Map<String, Long> = emptyMap(),
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()

    suspend fun fetchWeather(userId: String): WeatherForecast {
        _fetchedIds += userId
        delay(delays[userId] ?: delayMillis)
        failures[userId]?.let { throw it }
        val forecast = forecasts[userId] ?: throw WeatherLoadException(userId)
        _completedIds += userId
        return forecast
    }
}

object ExceptionHandlingPractice {

    /**
     * Exercise 1 — exception propagates
     *
     * Fetch [userId] from [userApi] and return the user.
     *
     * Requirement: do not catch. Let [HomeUserLoadException] propagate.
     */
    suspend fun loadUser(userApi: FakeHomeUserApi, userId: String): HomeUser {
        return userApi.fetchUser(userId)
    }

    /**
     * Exercise 2 — sequential load
     *
     * Load the user, then that user's messages, then weather.
     * Return a [HomeScreen] with all three.
     *
     * Requirement: fetch sequentially. Do not use `launch`, `async`,
     * `coroutineScope`, or `supervisorScope`.
     */
    suspend fun loadHomeSequential(
        userApi: FakeHomeUserApi,
        messagesApi: FakeMessagesApi,
        weatherApi: FakeWeatherApi,
        userId: String
    ): HomeScreen {
        val user = userApi.fetchUser(userId)
        val messages = messagesApi.fetchMessages(userId)
        val weather = weatherApi.fetchWeather(userId)
        return HomeScreen(user = user, messages = messages, weather = weather)
    }

    /**
     * Exercise 3 — coroutineScope cancels siblings
     *
     * Load user, messages, and weather at the same time, then return a
     * [HomeScreen].
     *
     * Requirement: use `async` inside `coroutineScope`. Do not use
     * `supervisorScope`.
     */
    suspend fun loadHomeStrict(
        userApi: FakeHomeUserApi,
        messagesApi: FakeMessagesApi,
        weatherApi: FakeWeatherApi,
        userId: String
    ): HomeScreen {
        TODO()
    }

    /**
     * Exercise 4 — catching does not supervise
     *
     * Same concurrent load as [loadHomeStrict].
     * Return the screen when every child succeeds.
     * Return null when any child fails. Do not throw that failure.
     *
     * Requirement: use `coroutineScope`. Catching the failure must not
     * keep cancelled siblings running.
     * Do not catch [CancellationException]. A cancelled coroutine must
     * not return null.
     */
    suspend fun loadHomeOrNull(
        userApi: FakeHomeUserApi,
        messagesApi: FakeMessagesApi,
        weatherApi: FakeWeatherApi,
        userId: String
    ): HomeScreen? {
        TODO()
    }

    /**
     * Exercise 5 — supervisorScope recovers one child
     *
     * Load user, messages, and weather at the same time.
     * If weather throws [WeatherLoadException], return the screen with
     * `weather = null`. User and messages must still finish.
     * If user or messages fail, that exception still propagates.
     *
     * Requirement: use `supervisorScope`. Catch only [WeatherLoadException]
     * around the weather await.
     */
    suspend fun loadHomeAllowWeatherFailure(
        userApi: FakeHomeUserApi,
        messagesApi: FakeMessagesApi,
        weatherApi: FakeWeatherApi,
        userId: String
    ): HomeScreen {
        TODO()
    }

    /**
     * Exercise 6 — failing await still cancels remaining children
     *
     * Load all three concurrently. Await weather first, then user, then
     * messages. Return a [HomeScreen].
     *
     * Requirement: use `supervisorScope` and `async`. Await weather
     * first. Do not catch the weather failure. If weather fails, the
     * remaining children must be cancelled.
     */
    suspend fun loadHomeAwaitWeatherFirst(
        userApi: FakeHomeUserApi,
        messagesApi: FakeMessagesApi,
        weatherApi: FakeWeatherApi,
        userId: String
    ): HomeScreen {
        TODO()
    }

    /**
     * Exercise 7 — SupervisorJob and CoroutineExceptionHandler
     *
     * Start messages and weather on a child scope and return both [Job]s.
     * When messages finish, call [onMessages].
     *
     * Requirement: create a scope with `SupervisorJob` and [handler].
     * Use `launch` for both children. A weather failure must not cancel
     * messages. Do not await either child.
     */
    fun startIndependentLoads(
        parent: CoroutineScope,
        handler: CoroutineExceptionHandler,
        messagesApi: FakeMessagesApi,
        weatherApi: FakeWeatherApi,
        userId: String,
        onMessages: (List<InboxMessage>) -> Unit
    ): IndependentLoads {
        TODO()
    }

    /**
     * Exercise 8 — async holds the exception until await
     *
     * Start a weather fetch on [scope] and return the [Deferred].
     *
     * Requirement: use `scope.async`. Return immediately. The caller
     * awaits. Do not use `coroutineScope`.
     */
    fun weatherDeferred(
        scope: CoroutineScope,
        weatherApi: FakeWeatherApi,
        userId: String
    ): Deferred<WeatherForecast> {
        TODO()
    }
}

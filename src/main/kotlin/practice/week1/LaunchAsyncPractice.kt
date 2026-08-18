package practice.week1

import kotlinx.coroutines.delay

/**
 * Day 7 — `launch` vs `async`.
 *
 * Key ideas:
 * - `launch` starts a coroutine for side effects and returns a `Job`.
 * - `async` starts a coroutine that produces a value and returns a `Deferred`.
 * - `coroutineScope` waits for children and cancels siblings when one fails.
 * Sequential suspend calls add delays. Independent `async` work overlaps.
 */
data class DashboardUser(
    val id: String,
    val name: String
)

data class DashboardOrder(
    val id: String,
    val amount: Int
)

data class Profile(
    val userId: String,
    val city: String
)

data class Dashboard(
    val user: DashboardUser,
    val orders: List<DashboardOrder>
)

data class FullDashboard(
    val user: DashboardUser,
    val orders: List<DashboardOrder>,
    val profile: Profile
)

class UserLoadException(val userId: String) : IllegalStateException("Failed to load user: $userId")

class OrderLoadException(val userId: String) : IllegalStateException("Failed to load orders: $userId")

class ProfileLoadException(val userId: String) : IllegalStateException("Failed to load profile: $userId")

class FakeUserApi(
    private val users: Map<String, DashboardUser> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val delays: Map<String, Long> = emptyMap(),
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()

    suspend fun fetchUser(id: String): DashboardUser {
        _fetchedIds += id
        delay(delays[id] ?: delayMillis)
        failures[id]?.let { throw it }
        val user = users[id] ?: throw UserLoadException(id)
        _completedIds += id
        return user
    }
}

class FakeOrderApi(
    private val orders: Map<String, List<DashboardOrder>> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()

    suspend fun fetchOrders(userId: String): List<DashboardOrder> {
        _fetchedIds += userId
        delay(delayMillis)
        failures[userId]?.let { throw it }
        val result = orders[userId] ?: emptyList()
        _completedIds += userId
        return result
    }
}

class FakeProfileApi(
    private val profiles: Map<String, Profile> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()

    suspend fun fetchProfile(userId: String): Profile {
        _fetchedIds += userId
        delay(delayMillis)
        failures[userId]?.let { throw it }
        val profile = profiles[userId] ?: throw ProfileLoadException(userId)
        _completedIds += userId
        return profile
    }
}

class FakeLogger(
    private val delayMillis: Long = 0L
) {
    private val _messages = mutableListOf<String>()
    val messages: List<String> get() = _messages.toList()

    suspend fun log(message: String) {
        delay(delayMillis)
        _messages += message
    }
}

object LaunchAsyncPractice {

    /**
     * Exercise 1 — sequential dashboard
     *
     * Load the user, then load that user's orders, then return a [Dashboard].
     *
     * Requirement: fetch sequentially. Do not use `launch` or `async`.
     */
    suspend fun loadDashboardSequential(
        userApi: FakeUserApi,
        orderApi: FakeOrderApi,
        userId: String
    ): Dashboard {
        val user = userApi.fetchUser(userId)
        val orders = orderApi.fetchOrders(userId)
        return Dashboard(user, orders)
    }

    /**
     * Exercise 2 — concurrent dashboard
     *
     * Load the user and orders at the same time, then return a [Dashboard].
     *
     * Requirement: use `async` and `await`. Use `coroutineScope`.
     */
    suspend fun loadDashboardConcurrent(
        userApi: FakeUserApi,
        orderApi: FakeOrderApi,
        userId: String
    ): Dashboard {
        TODO()
    }

    /**
     * Exercise 3 — async starts immediately
     *
     * Same result as [loadDashboardConcurrent], but await orders before the user.
     * Both calls must still overlap: total time is the max delay, not the sum.
     *
     * Requirement: start both with `async` before awaiting either.
     */
    suspend fun loadDashboardAwaitOrdersFirst(
        userApi: FakeUserApi,
        orderApi: FakeOrderApi,
        userId: String
    ): Dashboard {
        TODO()
    }

    /**
     * Exercise 4 — launch for a side effect
     *
     * Fetch the user and log `"loading:{userId}"` at the same time.
     * Return the user. The log must be finished when this function returns.
     *
     * Requirement: use `launch` for the log. Do not use `async` for the log.
     */
    suspend fun loadUserAndLog(
        userApi: FakeUserApi,
        logger: FakeLogger,
        userId: String
    ): DashboardUser {
        TODO()
    }

    /**
     * Exercise 5 — coroutineScope waits for children
     *
     * Log every message concurrently, one child coroutine per message.
     * Return how many messages were logged.
     *
     * Requirement: use `launch` inside `coroutineScope`.
     * Do not log sequentially.
     */
    suspend fun logAll(logger: FakeLogger, messages: List<String>): Int {
        TODO()
    }

    /**
     * Exercise 6 — concurrent list, original order
     *
     * Fetch every id concurrently. Return users in the original [ids] order.
     *
     * Requirement: use `async`. Do not fetch sequentially.
     */
    suspend fun loadUsersConcurrent(
        userApi: FakeUserApi,
        ids: List<String>
    ): List<DashboardUser> {
        TODO()
    }

    /**
     * Exercise 7 — sequential then concurrent
     *
     * Load the user first. If that fails, do not fetch orders or profile.
     * After the user succeeds, load orders and profile at the same time.
     *
     * Requirement: user is sequential. Orders and profile use `async`.
     */
    suspend fun loadFullDashboard(
        userApi: FakeUserApi,
        orderApi: FakeOrderApi,
        profileApi: FakeProfileApi,
        userId: String
    ): FullDashboard {
        TODO()
    }
}

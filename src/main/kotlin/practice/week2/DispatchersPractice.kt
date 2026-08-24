package practice.week2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * Day 10 — Dispatchers and threading.
 *
 * Key ideas:
 * - `suspend` does not pick a thread. The dispatcher in coroutine context does.
 * - IO: blocking network, disk, and database work.
 * - Default: CPU-heavy work such as sorting and aggregation.
 * - Main: UI updates. Do not block it with IO or CPU work.
 * - `withContext` switches dispatcher for a block, then restores the previous one.
 * - Inject dispatchers so tests can use `TestDispatcher` and production can
 *   use `Dispatchers.IO` / `Default` / `Main`. Do not hardcode those in this file.
 */
data class AccountOrder(
    val id: String,
    val amount: Int
)

data class AccountUser(
    val id: String,
    val name: String,
    val orders: List<AccountOrder> = emptyList()
)

data class AccountSummary(
    val userId: String,
    val displayName: String,
    val orderIds: List<String>,
    val totalAmount: Int
)

data class AccountScreen(
    val user: AccountUser,
    val summary: AccountSummary,
    val fromCache: Boolean
)

data class AppDispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher
)

class AccountNotFoundException(val userId: String) : IllegalStateException("Account not found: $userId")

class FakeAccountNetwork(
    private val users: Map<String, AccountUser> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _fetchedIds = mutableListOf<String>()
    private val _completedIds = mutableListOf<String>()
    private val _dispatchers = mutableListOf<CoroutineDispatcher>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()
    val completedIds: List<String> get() = _completedIds.toList()
    val dispatchers: List<CoroutineDispatcher> get() = _dispatchers.toList()
    val lastDispatcher: CoroutineDispatcher? get() = _dispatchers.lastOrNull()

    suspend fun fetchUser(id: String): AccountUser {
        _fetchedIds += id
        _dispatchers += coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        delay(delayMillis)
        failures[id]?.let { throw it }
        val user = users[id] ?: throw AccountNotFoundException(id)
        _completedIds += id
        return user
    }
}

class FakeAccountDatabase(
    private val users: MutableMap<String, AccountUser> = mutableMapOf(),
    private val delayMillis: Long = 0L
) {
    private val _readIds = mutableListOf<String>()
    private val _writtenIds = mutableListOf<String>()
    private val _readDispatchers = mutableListOf<CoroutineDispatcher>()
    private val _writeDispatchers = mutableListOf<CoroutineDispatcher>()
    val readIds: List<String> get() = _readIds.toList()
    val writtenIds: List<String> get() = _writtenIds.toList()
    val lastReadDispatcher: CoroutineDispatcher? get() = _readDispatchers.lastOrNull()
    val lastWriteDispatcher: CoroutineDispatcher? get() = _writeDispatchers.lastOrNull()

    suspend fun readUser(id: String): AccountUser? {
        _readIds += id
        _readDispatchers += coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        delay(delayMillis)
        return users[id]
    }

    suspend fun writeUser(user: AccountUser) {
        _writtenIds += user.id
        _writeDispatchers += coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        delay(delayMillis)
        users[user.id] = user
    }
}

class FakeAccountRenderer {
    private val _shown = mutableListOf<String>()
    private val _dispatchers = mutableListOf<CoroutineDispatcher>()
    val shown: List<String> get() = _shown.toList()
    val lastDispatcher: CoroutineDispatcher? get() = _dispatchers.lastOrNull()

    suspend fun show(text: String) {
        _dispatchers += coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        _shown += text
    }
}

object DispatchersPractice {

    /**
     * Exercise 1 — dispatcher lives in coroutine context
     *
     * Return the current [CoroutineDispatcher].
     *
     * Requirement: read `coroutineContext`. This function must stay `suspend`.
     * Do not hardcode `Dispatchers.Default` or any other dispatcher.
     */
    suspend fun currentDispatcher(): CoroutineDispatcher {
        return currentCoroutineContext()[ContinuationInterceptor] as CoroutineDispatcher
    }

    /**
     * Exercise 2 — withContext switches dispatcher
     *
     * Run [block] on [dispatcher] and return its result.
     * After [block] finishes, later work must use the previous dispatcher.
     *
     * Requirement: use `withContext`. Do not call [block] on the current dispatcher.
     */
    suspend fun <T> runOn(dispatcher: CoroutineDispatcher, block: suspend () -> T): T {
        return withContext(dispatcher) {
            block()
        }
    }

    /**
     * Exercise 3 — network work uses IO
     *
     * Fetch [userId] from [network] and return the user.
     *
     * Requirement: run the fetch on [io] with `withContext`.
     * Do not use `Dispatchers.IO`.
     */
    suspend fun fetchUser(
        network: FakeAccountNetwork,
        userId: String,
        io: CoroutineDispatcher
    ): AccountUser {
        return withContext(io) {
            network.fetchUser(userId)
        }
    }

    /**
     * Exercise 4 — database reads use IO
     *
     * Read [userId] from [database]. Return null when missing.
     *
     * Requirement: run the read on [io] with `withContext`.
     * Do not use `Dispatchers.IO`.
     */
    suspend fun readCachedUser(
        database: FakeAccountDatabase,
        userId: String,
        io: CoroutineDispatcher
    ): AccountUser? {
        return withContext(io) {
            database.readUser(userId)
        }
    }

    /**
     * Exercise 5 — database writes use IO
     *
     * Write [user] to [database].
     *
     * Requirement: run the write on [io] with `withContext`.
     * Do not use `Dispatchers.IO`.
     */
    suspend fun cacheUser(
        database: FakeAccountDatabase,
        user: AccountUser,
        io: CoroutineDispatcher
    ) {
        return withContext(io) {
            database.writeUser(user)
        }
    }

    /**
     * Exercise 6 — CPU work uses Default
     *
     * Build an [AccountSummary] from [user]:
     * - `displayName` is the uppercase name
     * - `orderIds` is every order id, sorted alphabetically
     * - `totalAmount` is the sum of order amounts
     *
     * Requirement: do this work on [default] with `withContext`.
     * Do not use `Dispatchers.Default`.
     */
    suspend fun summarizeUser(
        user: AccountUser,
        default: CoroutineDispatcher
    ): AccountSummary {
        return withContext(default) {
            AccountSummary(
                userId = user.id,
                displayName = user.name.uppercase(),
                orderIds = user.orders.map { it.id }.sorted(),
                totalAmount = user.orders.sumOf { it.amount }
            )
        }
    }

    /**
     * Exercise 7 — network then UI
     *
     * Fetch [userId] from [network], then show the user's name on [renderer].
     * Return the user.
     *
     * Requirement: fetch on [io], render on [main]. Use `withContext`.
     * Do not use `Dispatchers.IO` or `Dispatchers.Main`.
     */
    suspend fun fetchThenRender(
        network: FakeAccountNetwork,
        renderer: FakeAccountRenderer,
        userId: String,
        io: CoroutineDispatcher,
        main: CoroutineDispatcher
    ): AccountUser {
        val user = fetchUser(network, userId, io)
        withContext(main) {
            renderer.show(user.name)
        }
        return user
    }

    /**
     * Exercise 8 — cache-first load
     *
     * Load [userId] as an [AccountScreen].
     * Read the cache first on IO.
     * If the cache has the user, summarize on Default and return
     * `fromCache = true`. Do not fetch the network.
     * If the cache is empty, fetch the network on IO, write that user to
     * the database on IO, summarize on Default, and return `fromCache = false`.
     *
     * Requirement: use [dispatchers.io] and [dispatchers.default].
     * Do not use Main. Do not hardcode `Dispatchers.IO` or `Dispatchers.Default`.
     */
    suspend fun loadAccount(
        network: FakeAccountNetwork,
        database: FakeAccountDatabase,
        userId: String,
        dispatchers: AppDispatchers
    ): AccountScreen {
        TODO()
    }

    /**
     * Exercise 9 — full pipeline
     *
     * Load the account with [loadAccount], then show the summary display
     * name on [renderer]. Return the screen.
     *
     * Requirement: render on [dispatchers.main]. Loading must still follow
     * the Exercise 8 rules.
     */
    suspend fun loadAndRender(
        network: FakeAccountNetwork,
        database: FakeAccountDatabase,
        renderer: FakeAccountRenderer,
        userId: String,
        dispatchers: AppDispatchers
    ): AccountScreen {
        TODO()
    }
}

package practice.week2

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DispatchersPracticeTest {

    private val orders = listOf(
        AccountOrder("o2", 20),
        AccountOrder("o1", 50)
    )
    private val thomas = AccountUser("u1", "Thomas", orders)
    private val expectedSummary = AccountSummary(
        userId = "u1",
        displayName = "THOMAS",
        orderIds = listOf("o1", "o2"),
        totalAmount = 70
    )

    @Test
    fun `currentDispatcher reads the dispatcher from coroutine context`() = runTest {
        val expected = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher

        assertEquals(expected, DispatchersPractice.currentDispatcher())
    }

    @Test
    fun `runOn returns the block result`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")

        assertEquals(42, DispatchersPractice.runOn(io) { 42 })
    }

    @Test
    fun `runOn runs the block on the given dispatcher`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")

        val used = DispatchersPractice.runOn(io) {
            DispatchersPractice.currentDispatcher()
        }

        assertEquals(io, used)
    }

    @Test
    fun `runOn restores the previous dispatcher`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val original = DispatchersPractice.currentDispatcher()

        DispatchersPractice.runOn(io) { "done" }

        assertEquals(original, DispatchersPractice.currentDispatcher())
        assertNotEquals(io, original)
    }

    @Test
    fun `runOn still suspends with virtual time`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")

        val result = DispatchersPractice.runOn(io) {
            delay(1_000)
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `fetchUser returns the user`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))

        assertEquals(thomas, DispatchersPractice.fetchUser(network, "u1", io))
        assertEquals(listOf("u1"), network.fetchedIds)
        assertEquals(listOf("u1"), network.completedIds)
    }

    @Test
    fun `fetchUser runs on the io dispatcher`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))

        DispatchersPractice.fetchUser(network, "u1", io)

        assertEquals(io, network.lastDispatcher)
        assertNotEquals(DispatchersPractice.currentDispatcher(), network.lastDispatcher)
    }

    @Test
    fun `fetchUser throws AccountNotFoundException when missing`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val network = FakeAccountNetwork()

        val error = assertFailsWith<AccountNotFoundException> {
            DispatchersPractice.fetchUser(network, "missing", io)
        }

        assertEquals("missing", error.userId)
        assertEquals(io, network.lastDispatcher)
    }

    @Test
    fun `fetchUser suspends for the given duration`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas), delayMillis = 1_000)

        assertEquals(thomas, DispatchersPractice.fetchUser(network, "u1", io))
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `readCachedUser returns the cached user`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase(users = mutableMapOf("u1" to thomas))

        assertEquals(thomas, DispatchersPractice.readCachedUser(database, "u1", io))
        assertEquals(listOf("u1"), database.readIds)
    }

    @Test
    fun `readCachedUser returns null when missing`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase()

        assertNull(DispatchersPractice.readCachedUser(database, "u1", io))
        assertEquals(listOf("u1"), database.readIds)
        assertEquals(io, database.lastReadDispatcher)
    }

    @Test
    fun `readCachedUser runs on the io dispatcher`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase(users = mutableMapOf("u1" to thomas))

        DispatchersPractice.readCachedUser(database, "u1", io)

        assertEquals(io, database.lastReadDispatcher)
        assertNotEquals(DispatchersPractice.currentDispatcher(), database.lastReadDispatcher)
    }

    @Test
    fun `readCachedUser suspends for the given duration`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase(
            users = mutableMapOf("u1" to thomas),
            delayMillis = 750
        )

        assertEquals(thomas, DispatchersPractice.readCachedUser(database, "u1", io))
        assertEquals(750, currentTime)
    }

    @Test
    fun `cacheUser writes the user`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase()

        DispatchersPractice.cacheUser(database, thomas, io)

        assertEquals(listOf("u1"), database.writtenIds)
        assertEquals(thomas, database.readUser("u1"))
    }

    @Test
    fun `cacheUser runs on the io dispatcher`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase()

        DispatchersPractice.cacheUser(database, thomas, io)

        assertEquals(io, database.lastWriteDispatcher)
        assertNotEquals(DispatchersPractice.currentDispatcher(), database.lastWriteDispatcher)
    }

    @Test
    fun `cacheUser suspends for the given duration`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val database = FakeAccountDatabase(delayMillis = 1_000)

        DispatchersPractice.cacheUser(database, thomas, io)

        assertEquals(1_000, currentTime)
    }

    @Test
    fun `summarizeUser uppercases sorts and sums`() = runTest {
        val default = TrackingDispatcher(StandardTestDispatcher(testScheduler, name = "default"))

        assertEquals(expectedSummary, DispatchersPractice.summarizeUser(thomas, default))
    }

    @Test
    fun `summarizeUser handles a user with no orders`() = runTest {
        val default = TrackingDispatcher(StandardTestDispatcher(testScheduler, name = "default"))
        val empty = AccountUser("u2", "Ada")

        assertEquals(
            AccountSummary(
                userId = "u2",
                displayName = "ADA",
                orderIds = emptyList(),
                totalAmount = 0
            ),
            DispatchersPractice.summarizeUser(empty, default)
        )
    }

    @Test
    fun `summarizeUser runs on the default dispatcher`() = runTest {
        val default = TrackingDispatcher(StandardTestDispatcher(testScheduler, name = "default"))

        DispatchersPractice.summarizeUser(thomas, default)

        assertTrue(default.dispatchCount > 0)
        assertNotEquals(DispatchersPractice.currentDispatcher(), default)
    }

    @Test
    fun `fetchThenRender returns the user and shows the name`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val main = StandardTestDispatcher(testScheduler, name = "main")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))
        val renderer = FakeAccountRenderer()

        assertEquals(
            thomas,
            DispatchersPractice.fetchThenRender(network, renderer, "u1", io, main)
        )
        assertEquals(listOf("Thomas"), renderer.shown)
    }

    @Test
    fun `fetchThenRender fetches on io and renders on main`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val main = StandardTestDispatcher(testScheduler, name = "main")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))
        val renderer = FakeAccountRenderer()

        DispatchersPractice.fetchThenRender(network, renderer, "u1", io, main)

        assertEquals(io, network.lastDispatcher)
        assertEquals(main, renderer.lastDispatcher)
        assertNotEquals(io, main)
    }

    @Test
    fun `fetchThenRender waits for the network before rendering`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val main = StandardTestDispatcher(testScheduler, name = "main")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val renderer = FakeAccountRenderer()

        DispatchersPractice.fetchThenRender(network, renderer, "u1", io, main)

        assertEquals(listOf("Thomas"), renderer.shown)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `fetchThenRender does not render when the user is missing`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val main = StandardTestDispatcher(testScheduler, name = "main")
        val network = FakeAccountNetwork()
        val renderer = FakeAccountRenderer()

        assertFailsWith<AccountNotFoundException> {
            DispatchersPractice.fetchThenRender(network, renderer, "missing", io, main)
        }
        assertEquals(emptyList(), renderer.shown)
        assertEquals(io, network.lastDispatcher)
    }

    @Test
    fun `fetchThenRender does not render when cancelled during fetch`() = runTest {
        val io = StandardTestDispatcher(testScheduler, name = "io")
        val main = StandardTestDispatcher(testScheduler, name = "main")
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas), delayMillis = 5_000)
        val renderer = FakeAccountRenderer()
        var result: AccountUser? = null

        val job = launch {
            result = DispatchersPractice.fetchThenRender(network, renderer, "u1", io, main)
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertNull(result)
        assertEquals(emptyList(), renderer.shown)
        assertEquals(listOf("u1"), network.fetchedIds)
        assertEquals(emptyList(), network.completedIds)
    }

    @Test
    fun `loadAccount returns cached data without hitting the network`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val database = FakeAccountDatabase(users = mutableMapOf("u1" to thomas), delayMillis = 400)

        val screen = DispatchersPractice.loadAccount(network, database, "u1", dispatchers.app)

        assertEquals(AccountScreen(thomas, expectedSummary, fromCache = true), screen)
        assertEquals(emptyList(), network.fetchedIds)
        assertEquals(listOf("u1"), database.readIds)
        assertEquals(emptyList(), database.writtenIds)
        assertEquals(400, currentTime)
    }

    @Test
    fun `loadAccount fetches writes and summarizes on a cache miss`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val database = FakeAccountDatabase(delayMillis = 500)

        val screen = DispatchersPractice.loadAccount(network, database, "u1", dispatchers.app)

        assertEquals(AccountScreen(thomas, expectedSummary, fromCache = false), screen)
        assertEquals(listOf("u1"), network.fetchedIds)
        assertEquals(listOf("u1"), database.readIds)
        assertEquals(listOf("u1"), database.writtenIds)
        assertEquals(2_000, currentTime)
        assertEquals(thomas, database.readUser("u1"))
    }

    @Test
    fun `loadAccount uses io for cache and network and default for summary`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))
        val database = FakeAccountDatabase()

        DispatchersPractice.loadAccount(network, database, "u1", dispatchers.app)

        assertEquals(dispatchers.io, database.lastReadDispatcher)
        assertEquals(dispatchers.io, network.lastDispatcher)
        assertEquals(dispatchers.io, database.lastWriteDispatcher)
        assertTrue(dispatchers.default.dispatchCount > 0)
        assertEquals(0, dispatchers.main.dispatchCount)
    }

    @Test
    fun `loadAccount cache hit still summarizes on default and skips main`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))
        val database = FakeAccountDatabase(users = mutableMapOf("u1" to thomas))

        DispatchersPractice.loadAccount(network, database, "u1", dispatchers.app)

        assertEquals(dispatchers.io, database.lastReadDispatcher)
        assertNull(network.lastDispatcher)
        assertTrue(dispatchers.default.dispatchCount > 0)
        assertEquals(0, dispatchers.main.dispatchCount)
    }

    @Test
    fun `loadAccount does not write cache when the network fails`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork()
        val database = FakeAccountDatabase()

        val error = assertFailsWith<AccountNotFoundException> {
            DispatchersPractice.loadAccount(network, database, "missing", dispatchers.app)
        }

        assertEquals("missing", error.userId)
        assertEquals(listOf("missing"), database.readIds)
        assertEquals(emptyList(), database.writtenIds)
        assertEquals(dispatchers.io, network.lastDispatcher)
    }

    @Test
    fun `loadAndRender shows the display name on main`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))
        val database = FakeAccountDatabase()
        val renderer = FakeAccountRenderer()

        val screen = DispatchersPractice.loadAndRender(
            network,
            database,
            renderer,
            "u1",
            dispatchers.app
        )

        assertEquals(AccountScreen(thomas, expectedSummary, fromCache = false), screen)
        assertEquals(listOf("THOMAS"), renderer.shown)
        assertEquals(dispatchers.main, renderer.lastDispatcher)
        assertTrue(dispatchers.main.dispatchCount > 0)
    }

    @Test
    fun `loadAndRender still uses cache on a hit`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork(users = mapOf("u1" to thomas))
        val database = FakeAccountDatabase(users = mutableMapOf("u1" to thomas))
        val renderer = FakeAccountRenderer()

        val screen = DispatchersPractice.loadAndRender(
            network,
            database,
            renderer,
            "u1",
            dispatchers.app
        )

        assertEquals(AccountScreen(thomas, expectedSummary, fromCache = true), screen)
        assertEquals(emptyList(), network.fetchedIds)
        assertEquals(listOf("THOMAS"), renderer.shown)
        assertEquals(dispatchers.main, renderer.lastDispatcher)
    }

    @Test
    fun `loadAndRender does not render when load fails`() = runTest {
        val dispatchers = testDispatchers(testScheduler)
        val network = FakeAccountNetwork()
        val database = FakeAccountDatabase()
        val renderer = FakeAccountRenderer()

        assertFailsWith<AccountNotFoundException> {
            DispatchersPractice.loadAndRender(
                network,
                database,
                renderer,
                "missing",
                dispatchers.app
            )
        }
        assertEquals(emptyList(), renderer.shown)
        assertEquals(0, dispatchers.main.dispatchCount)
    }

    private fun testDispatchers(scheduler: TestCoroutineScheduler) = TestDispatchers(scheduler)
}

private class TestDispatchers(scheduler: TestCoroutineScheduler) {
    val main = TrackingDispatcher(StandardTestDispatcher(scheduler, name = "main"))
    val io = StandardTestDispatcher(scheduler, name = "io")
    val default = TrackingDispatcher(StandardTestDispatcher(scheduler, name = "default"))
    val app = AppDispatchers(main = main, io = io, default = default)
}

private class TrackingDispatcher(
    private val wrapped: CoroutineDispatcher
) : CoroutineDispatcher() {
    var dispatchCount: Int = 0
        private set

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount++
        wrapped.dispatch(context, block)
    }
}

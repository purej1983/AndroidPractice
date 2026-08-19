package practice.week1

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchAsyncPracticeTest {

    private val thomas = DashboardUser("u1", "Thomas")
    private val ada = DashboardUser("u2", "Ada")
    private val orders = listOf(DashboardOrder("o1", 50), DashboardOrder("o2", 20))
    private val profile = Profile("u1", "Hong Kong")

    @Test
    fun `loadDashboardSequential returns user and orders`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders))

        assertEquals(
            Dashboard(thomas, orders),
            LaunchAsyncPractice.loadDashboardSequential(userApi, orderApi, "u1")
        )
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(listOf("u1"), orderApi.fetchedIds)
    }

    @Test
    fun `loadDashboardSequential returns empty orders when none exist`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))
        val orderApi = FakeOrderApi()

        assertEquals(
            Dashboard(thomas, emptyList()),
            LaunchAsyncPractice.loadDashboardSequential(userApi, orderApi, "u1")
        )
    }

    @Test
    fun `loadDashboardSequential fetches one after another so delays add up`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 1_000)

        val dashboard = LaunchAsyncPractice.loadDashboardSequential(userApi, orderApi, "u1")

        assertEquals(Dashboard(thomas, orders), dashboard)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `loadDashboardSequential does not fetch orders when the user fails`() = runTest {
        val userApi = FakeUserApi()
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders))

        val error = assertFailsWith<UserLoadException> {
            LaunchAsyncPractice.loadDashboardSequential(userApi, orderApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(emptyList(), orderApi.fetchedIds)
    }

    @Test
    fun `loadDashboardConcurrent returns user and orders`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders))

        assertEquals(
            Dashboard(thomas, orders),
            LaunchAsyncPractice.loadDashboardConcurrent(userApi, orderApi, "u1")
        )
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(listOf("u1"), orderApi.fetchedIds)
    }

    @Test
    fun `loadDashboardConcurrent overlaps independent work`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 1_000)

        val dashboard = LaunchAsyncPractice.loadDashboardConcurrent(userApi, orderApi, "u1")

        assertEquals(Dashboard(thomas, orders), dashboard)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadDashboardConcurrent total time is the slower call`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 500)
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 2_000)

        val dashboard = LaunchAsyncPractice.loadDashboardConcurrent(userApi, orderApi, "u1")

        assertEquals(Dashboard(thomas, orders), dashboard)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `loadDashboardConcurrent cancels the user load when orders fail`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 5_000)
        val orderApi = FakeOrderApi(
            delayMillis = 200,
            failures = mapOf("u1" to OrderLoadException("u1"))
        )

        val error = assertFailsWith<OrderLoadException> {
            LaunchAsyncPractice.loadDashboardConcurrent(userApi, orderApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(emptyList(), userApi.completedIds)
        assertEquals(200, currentTime)
    }

    @Test
    fun `loadDashboardConcurrent cancels orders when the user fails`() = runTest {
        val userApi = FakeUserApi(
            delayMillis = 200,
            failures = mapOf("u1" to UserLoadException("u1"))
        )
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 5_000)

        val error = assertFailsWith<UserLoadException> {
            LaunchAsyncPractice.loadDashboardConcurrent(userApi, orderApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(listOf("u1"), orderApi.fetchedIds)
        assertEquals(emptyList(), orderApi.completedIds)
        assertEquals(200, currentTime)
    }

    @Test
    fun `loadDashboardAwaitOrdersFirst still overlaps both calls`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 400)

        val dashboard = LaunchAsyncPractice.loadDashboardAwaitOrdersFirst(userApi, orderApi, "u1")

        assertEquals(Dashboard(thomas, orders), dashboard)
        assertEquals(listOf("u1"), userApi.fetchedIds)
        assertEquals(listOf("u1"), orderApi.fetchedIds)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadUserAndLog returns the user and writes the log`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))
        val logger = FakeLogger()

        assertEquals(thomas, LaunchAsyncPractice.loadUserAndLog(userApi, logger, "u1"))
        assertEquals(listOf("loading:u1"), logger.messages)
    }

    @Test
    fun `loadUserAndLog runs the log concurrently with the user fetch`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val logger = FakeLogger(delayMillis = 400)

        val user = LaunchAsyncPractice.loadUserAndLog(userApi, logger, "u1")

        assertEquals(thomas, user)
        assertEquals(listOf("loading:u1"), logger.messages)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `logAll logs every message and returns the count`() = runTest {
        val logger = FakeLogger()
        val messages = listOf("a", "b", "c")

        assertEquals(3, LaunchAsyncPractice.logAll(logger, messages))
        assertEquals(messages.toSet(), logger.messages.toSet())
    }

    @Test
    fun `logAll logs concurrently so delays overlap`() = runTest {
        val logger = FakeLogger(delayMillis = 1_000)
        val messages = listOf("one", "two", "three")

        val count = LaunchAsyncPractice.logAll(logger, messages)

        assertEquals(3, count)
        assertEquals(messages.toSet(), logger.messages.toSet())
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `logAll returns zero for an empty list`() = runTest {
        val logger = FakeLogger(delayMillis = 1_000)

        assertEquals(0, LaunchAsyncPractice.logAll(logger, emptyList()))
        assertEquals(emptyList(), logger.messages)
        assertEquals(0, currentTime)
    }

    @Test
    fun `loadUsersConcurrent returns users in the original id order`() = runTest {
        val userApi = FakeUserApi(
            users = mapOf("u1" to thomas, "u2" to ada)
        )

        assertEquals(
            listOf(thomas, ada),
            LaunchAsyncPractice.loadUsersConcurrent(userApi, listOf("u1", "u2"))
        )
        assertEquals(listOf("u1", "u2"), userApi.fetchedIds)
    }

    @Test
    fun `loadUsersConcurrent preserves order even when a later id finishes first`() = runTest {
        val userApi = FakeUserApi(
            users = mapOf("u1" to thomas, "u2" to ada),
            delays = mapOf("u1" to 1_000, "u2" to 100)
        )

        val users = LaunchAsyncPractice.loadUsersConcurrent(userApi, listOf("u1", "u2"))

        assertEquals(listOf(thomas, ada), users)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadUsersConcurrent overlaps every fetch`() = runTest {
        val userApi = FakeUserApi(
            users = mapOf("u1" to thomas, "u2" to ada),
            delayMillis = 1_000
        )

        val users = LaunchAsyncPractice.loadUsersConcurrent(userApi, listOf("u1", "u2"))

        assertEquals(listOf(thomas, ada), users)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `loadUsersConcurrent returns empty for an empty id list`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))

        assertEquals(
            emptyList(),
            LaunchAsyncPractice.loadUsersConcurrent(userApi, emptyList())
        )
        assertEquals(emptyList(), userApi.fetchedIds)
    }

    @Test
    fun `loadUsersConcurrent throws UserLoadException when an id is missing`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))

        val error = assertFailsWith<UserLoadException> {
            LaunchAsyncPractice.loadUsersConcurrent(userApi, listOf("u1", "missing"))
        }

        assertEquals("missing", error.userId)
    }

    @Test
    fun `loadFullDashboard returns user orders and profile`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas))
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders))
        val profileApi = FakeProfileApi(profiles = mapOf("u1" to profile))

        assertEquals(
            FullDashboard(thomas, orders, profile),
            LaunchAsyncPractice.loadFullDashboard(userApi, orderApi, profileApi, "u1")
        )
    }

    @Test
    fun `loadFullDashboard loads user first then overlaps orders and profile`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 1_000)
        val profileApi = FakeProfileApi(profiles = mapOf("u1" to profile), delayMillis = 1_000)

        val dashboard = LaunchAsyncPractice.loadFullDashboard(userApi, orderApi, profileApi, "u1")

        assertEquals(FullDashboard(thomas, orders, profile), dashboard)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `loadFullDashboard total time is user delay plus the slower of the other two`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 1_000)
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders), delayMillis = 500)
        val profileApi = FakeProfileApi(profiles = mapOf("u1" to profile), delayMillis = 2_000)

        val dashboard = LaunchAsyncPractice.loadFullDashboard(userApi, orderApi, profileApi, "u1")

        assertEquals(FullDashboard(thomas, orders, profile), dashboard)
        assertEquals(3_000, currentTime)
    }

    @Test
    fun `loadFullDashboard does not fetch orders or profile when the user fails`() = runTest {
        val userApi = FakeUserApi()
        val orderApi = FakeOrderApi(orders = mapOf("u1" to orders))
        val profileApi = FakeProfileApi(profiles = mapOf("u1" to profile))

        val error = assertFailsWith<UserLoadException> {
            LaunchAsyncPractice.loadFullDashboard(userApi, orderApi, profileApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertEquals(emptyList(), orderApi.fetchedIds)
        assertEquals(emptyList(), profileApi.fetchedIds)
    }

    @Test
    fun `loadFullDashboard cancels profile when orders fail`() = runTest {
        val userApi = FakeUserApi(users = mapOf("u1" to thomas), delayMillis = 100)
        val orderApi = FakeOrderApi(
            delayMillis = 200,
            failures = mapOf("u1" to OrderLoadException("u1"))
        )
        val profileApi = FakeProfileApi(profiles = mapOf("u1" to profile), delayMillis = 5_000)

        val error = assertFailsWith<OrderLoadException> {
            LaunchAsyncPractice.loadFullDashboard(userApi, orderApi, profileApi, "u1")
        }

        assertEquals("u1", error.userId)
        assertTrue(profileApi.fetchedIds.contains("u1"))
        assertEquals(emptyList(), profileApi.completedIds)
        assertEquals(300, currentTime)
    }
}

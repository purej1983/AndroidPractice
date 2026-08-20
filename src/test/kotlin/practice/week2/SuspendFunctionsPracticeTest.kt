package practice.week2

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SuspendFunctionsPracticeTest {

    @Test
    fun `delayedValue returns the value`() = runTest {
        assertEquals(
            "done",
            SuspendFunctionsPractice.delayedValue("done", delayMillis = 0)
        )
    }

    @Test
    fun `delayedValue suspends for the given duration`() = runTest {
        val result = SuspendFunctionsPractice.delayedValue("ok", delayMillis = 1_000)

        assertEquals("ok", result)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `delayedValue uses delay so a long wait is virtual time`() = runTest {
        val result = SuspendFunctionsPractice.delayedValue("done", delayMillis = 120_000)

        assertEquals("done", result)
        assertEquals(120_000, currentTime)
    }

    @Test
    fun `fetchAfterDelay returns the user after suspending`() = runTest {
        val user = SuspendFunctionsPractice.fetchAfterDelay(
            id = "u1",
            name = "Thomas",
            delayMillis = 500
        )

        assertEquals(RemoteUser("u1", "Thomas"), user)
        assertEquals(500, currentTime)
    }

    @Test
    fun `loadUser returns the user from the data source`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas"))
        )

        assertEquals(
            RemoteUser("u1", "Thomas"),
            SuspendFunctionsPractice.loadUser(source, "u1")
        )
        assertEquals(listOf("u1"), source.fetchedIds)
    }

    @Test
    fun `loadUser waits for the data source delay`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas")),
            delayMillis = 750
        )

        val user = SuspendFunctionsPractice.loadUser(source, "u1")

        assertEquals(RemoteUser("u1", "Thomas"), user)
        assertEquals(750, currentTime)
    }

    @Test
    fun `loadUser throws UserNotFoundException when missing`() = runTest {
        val source = FakeUserDataSource()

        val error = assertFailsWith<UserNotFoundException> {
            SuspendFunctionsPractice.loadUser(source, "missing")
        }

        assertEquals("missing", error.userId)
    }

    @Test
    fun `loadUserOrNull returns the user`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas"))
        )

        assertEquals(
            RemoteUser("u1", "Thomas"),
            SuspendFunctionsPractice.loadUserOrNull(source, "u1")
        )
    }

    @Test
    fun `loadUserOrNull returns null when missing`() = runTest {
        val source = FakeUserDataSource()

        assertNull(SuspendFunctionsPractice.loadUserOrNull(source, "missing"))
    }

    @Test
    fun `loadUserName returns the name`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas"))
        )

        assertEquals("Thomas", SuspendFunctionsPractice.loadUserName(source, "u1"))
    }

    @Test
    fun `loadUserName throws UserNotFoundException when missing`() = runTest {
        val source = FakeUserDataSource()

        val error = assertFailsWith<UserNotFoundException> {
            SuspendFunctionsPractice.loadUserName(source, "missing")
        }

        assertEquals("missing", error.userId)
    }

    @Test
    fun `loadUsers returns found users in original order`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf(
                "u1" to RemoteUser("u1", "Thomas"),
                "u3" to RemoteUser("u3", "Ada")
            )
        )

        assertEquals(
            listOf(RemoteUser("u1", "Thomas"), RemoteUser("u3", "Ada")),
            SuspendFunctionsPractice.loadUsers(source, listOf("u1", "missing", "u3"))
        )
        assertEquals(listOf("u1", "missing", "u3"), source.fetchedIds)
    }

    @Test
    fun `loadUsers returns empty for an empty id list`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas"))
        )

        assertEquals(
            emptyList(),
            SuspendFunctionsPractice.loadUsers(source, emptyList())
        )
        assertEquals(emptyList(), source.fetchedIds)
    }

    @Test
    fun `loadUsers fetches sequentially so delays add up`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf(
                "u1" to RemoteUser("u1", "Thomas"),
                "u2" to RemoteUser("u2", "Ada")
            ),
            delayMillis = 1_000
        )

        val users = SuspendFunctionsPractice.loadUsers(source, listOf("u1", "u2"))

        assertEquals(
            listOf(RemoteUser("u1", "Thomas"), RemoteUser("u2", "Ada")),
            users
        )
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `loadUserOrFallback returns the user when found`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas"))
        )
        val fallback = RemoteUser("fb", "fallback")

        assertEquals(
            RemoteUser("u1", "Thomas"),
            SuspendFunctionsPractice.loadUserOrFallback(source, "u1", fallback)
        )
    }

    @Test
    fun `loadUserOrFallback returns fallback when missing`() = runTest {
        val source = FakeUserDataSource()
        val fallback = RemoteUser("fb", "fallback")

        assertEquals(
            fallback,
            SuspendFunctionsPractice.loadUserOrFallback(source, "missing", fallback)
        )
    }

    @Test
    fun `loadUserOrFallback returns fallback when fetch throws`() = runTest {
        val source = FakeUserDataSource(
            failures = mapOf("u1" to IllegalStateException("offline"))
        )
        val fallback = RemoteUser("fb", "fallback")

        assertEquals(
            fallback,
            SuspendFunctionsPractice.loadUserOrFallback(source, "u1", fallback)
        )
    }

    @Test
    fun `loadUserOrFallback does not return fallback when cancelled`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas")),
            delayMillis = 5_000
        )
        val fallback = RemoteUser("fb", "fallback")
        var result: RemoteUser? = null

        val job = launch {
            result = SuspendFunctionsPractice.loadUserOrFallback(source, "u1", fallback)
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(listOf("u1"), source.fetchedIds)
        assertNull(result)
    }

    @Test
    fun `mapUser transforms the loaded user`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas"))
        )

        val name = SuspendFunctionsPractice.mapUser(source, "u1") { user ->
            user.name.uppercase()
        }

        assertEquals("THOMAS", name)
    }

    @Test
    fun `mapUser throws UserNotFoundException when missing`() = runTest {
        val source = FakeUserDataSource()

        val error = assertFailsWith<UserNotFoundException> {
            SuspendFunctionsPractice.mapUser(source, "missing") { it.name }
        }

        assertEquals("missing", error.userId)
    }

    @Test
    fun `mapUser waits for a suspending transform`() = runTest {
        val source = FakeUserDataSource(
            users = mapOf("u1" to RemoteUser("u1", "Thomas")),
            delayMillis = 400
        )

        val name = SuspendFunctionsPractice.mapUser(source, "u1") { user ->
            delay(600)
            user.name
        }

        assertEquals("Thomas", name)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `currentCoroutineName reads CoroutineName from context`() = runTest {
        val name = withContext(CoroutineName("loader")) {
            SuspendFunctionsPractice.currentCoroutineName()
        }

        assertEquals("loader", name)
    }

    @Test
    fun `currentCoroutineName is null when no name is set`() = runTest {
        assertNull(SuspendFunctionsPractice.currentCoroutineName())
    }

    @Test
    fun `withName runs the block under CoroutineName`() = runTest {
        val name = SuspendFunctionsPractice.withName("network") {
            SuspendFunctionsPractice.currentCoroutineName()
        }

        assertEquals("network", name)
    }

    @Test
    fun `withName returns the block result`() = runTest {
        val result = SuspendFunctionsPractice.withName("network") { 42 }

        assertEquals(42, result)
    }
}

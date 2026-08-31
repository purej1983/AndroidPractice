package practice.week4

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFirstPracticeTest {

    private val alice = CachedUser("u1", "Alice")
    private val bob = CachedUser("u2", "Bob")
    private val cara = CachedUser("u3", "Cara")

    @Test
    fun `users and refreshStatus are not MutableStateFlow`() = runTest {
        val repository = CachedUserRepository(
            InMemoryUserLocalDataSource(),
            FakeUserRemoteDataSource()
        )
        val feed = OfflineFeedController(repository, backgroundScope)

        assertTrue(feed.users !is MutableStateFlow)
        assertTrue(feed.refreshStatus !is MutableStateFlow)
        val users: StateFlow<List<CachedUser>> = feed.users
        val status: StateFlow<RefreshStatus> = feed.refreshStatus
        assertEquals(emptyList(), users.value)
        assertEquals(RefreshStatus.Idle, status.value)
    }

    @Test
    fun `cached data is available immediately without a network call`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice, bob))
        val remote = FakeUserRemoteDataSource(users = listOf(cara), delayMillis = 5_000)
        val feed = OfflineFeedController(CachedUserRepository(local, remote), backgroundScope)
        testScheduler.runCurrent()

        assertEquals(listOf(alice, bob), feed.users.value)
        assertEquals(0, remote.startedFetches)
        assertEquals(RefreshStatus.Idle, feed.refreshStatus.value)
    }

    @Test
    fun `API success updates the database and observers`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(users = listOf(bob, cara))
        val emitted = mutableListOf<List<CachedUser>>()
        val feed = OfflineFeedController(CachedUserRepository(local, remote), backgroundScope)

        val job = launch { feed.users.collect { emitted += it } }
        testScheduler.runCurrent()
        feed.refresh()
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice), listOf(bob, cara)), emitted)
        assertEquals(listOf(bob, cara), local.currentUsers())
        assertEquals(RefreshStatus.Idle, feed.refreshStatus.value)
        job.cancelAndJoin()
    }

    @Test
    fun `database changes emit through the UI flow`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource()
        val feed = OfflineFeedController(CachedUserRepository(local, remote), backgroundScope)
        val emitted = mutableListOf<List<CachedUser>>()

        val job = launch { feed.users.collect { emitted += it } }
        testScheduler.runCurrent()
        local.upsert(bob)
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice), listOf(alice, bob)), emitted)
        assertEquals(0, remote.startedFetches)
        job.cancelAndJoin()
    }

    @Test
    fun `API failure does not destroy usable cache`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice, bob))
        val remote = FakeUserRemoteDataSource(
            users = emptyList(),
            failure = RemoteUsersException("unreachable")
        )
        val feed = OfflineFeedController(CachedUserRepository(local, remote), backgroundScope)
        testScheduler.runCurrent()

        feed.refresh()
        testScheduler.runCurrent()

        assertEquals(listOf(alice, bob), feed.users.value)
        assertEquals(listOf(alice, bob), local.currentUsers())
        assertEquals(RefreshStatus.Failed("unreachable"), feed.refreshStatus.value)
    }

    @Test
    fun `new server data replaces stale rows including deletes`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice, bob))
        val remote = FakeUserRemoteDataSource(users = listOf(cara))
        val feed = OfflineFeedController(CachedUserRepository(local, remote), backgroundScope)
        testScheduler.runCurrent()

        feed.refresh()
        testScheduler.runCurrent()

        assertEquals(listOf(cara), feed.users.value)
        assertEquals(listOf(cara), local.currentUsers())
    }

    @Test
    fun `refresh marks Refreshing until the network returns`() = runTest {
        val remote = FakeUserRemoteDataSource(users = listOf(alice), delayMillis = 250)
        val feed = OfflineFeedController(
            CachedUserRepository(InMemoryUserLocalDataSource(), remote),
            backgroundScope
        )
        testScheduler.runCurrent()

        feed.refresh()
        testScheduler.runCurrent()
        assertEquals(RefreshStatus.Refreshing, feed.refreshStatus.value)

        testScheduler.advanceTimeBy(250)
        testScheduler.runCurrent()
        assertEquals(RefreshStatus.Idle, feed.refreshStatus.value)
        assertEquals(listOf(alice), feed.users.value)
    }

    @Test
    fun `failed refresh is not the same as an empty user list`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(failure = RemoteUsersException("timeout"))
        val feed = OfflineFeedController(CachedUserRepository(local, remote), backgroundScope)
        testScheduler.runCurrent()

        feed.refresh()
        testScheduler.runCurrent()

        assertTrue(feed.users.value.isNotEmpty())
        assertTrue(feed.refreshStatus.value is RefreshStatus.Failed)
    }

    @Test
    fun `a newer refresh cancels the previous network call`() = runTest {
        val remote = FakeUserRemoteDataSource(users = listOf(alice), delayMillis = 1_000)
        val feed = OfflineFeedController(
            CachedUserRepository(InMemoryUserLocalDataSource(), remote),
            backgroundScope
        )
        testScheduler.runCurrent()

        feed.refresh()
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        remote.setUsers(listOf(bob))
        feed.refresh()
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertEquals(2, remote.startedFetches)
        assertEquals(1, remote.cancelledFetches)
        assertEquals(1, remote.completedFetches)
        assertEquals(listOf(bob), feed.users.value)
        assertEquals(RefreshStatus.Idle, feed.refreshStatus.value)
    }
}

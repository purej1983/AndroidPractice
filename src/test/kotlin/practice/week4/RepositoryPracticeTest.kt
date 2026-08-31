package practice.week4

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryPracticeTest {

    private val alice = CachedUser("u1", "Alice")
    private val bob = CachedUser("u2", "Bob")
    private val cara = CachedUser("u3", "Cara")

    @Test
    fun `local observeUsers emits the seed immediately`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val emitted = mutableListOf<List<CachedUser>>()

        val job = launch { local.observeUsers().collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice)), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `local observeUsers is not a MutableStateFlow`() = runTest {
        val local = InMemoryUserLocalDataSource()
        val flow = local.observeUsers()

        assertTrue(flow is StateFlow<*>)
        assertTrue(flow !is MutableStateFlow<*>)
    }

    @Test
    fun `local replaceAll replaces the table`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))

        local.replaceAll(listOf(bob, cara))

        assertEquals(listOf(bob, cara), local.currentUsers())
    }

    @Test
    fun `local upsert inserts a new id`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))

        local.upsert(bob)

        assertEquals(listOf(alice, bob), local.currentUsers())
    }

    @Test
    fun `local upsert replaces the same id and keeps others`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice, bob))
        val renamed = alice.copy(name = "Alicia")

        local.upsert(renamed)

        assertEquals(listOf(bob, renamed), local.currentUsers())
    }

    @Test
    fun `local replaceAll is visible to a collector`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val emitted = mutableListOf<List<CachedUser>>()

        val job = launch { local.observeUsers().collect { emitted += it } }
        testScheduler.runCurrent()
        local.replaceAll(listOf(bob))
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice), listOf(bob)), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `remote fetchUsers returns the configured list`() = runTest {
        val remote = FakeUserRemoteDataSource(users = listOf(alice, bob))

        assertEquals(listOf(alice, bob), remote.fetchUsers())
        assertEquals(1, remote.startedFetches)
        assertEquals(1, remote.completedFetches)
    }

    @Test
    fun `remote fetchUsers throws the configured failure`() = runTest {
        val remote = FakeUserRemoteDataSource(
            failure = RemoteUsersException("down")
        )

        val error = assertFailsWith<RemoteUsersException> { remote.fetchUsers() }
        assertEquals("down", error.message)
        assertEquals(1, remote.startedFetches)
        assertEquals(0, remote.completedFetches)
    }

    @Test
    fun `remote fetchUsers delay is cancellable`() = runTest {
        val remote = FakeUserRemoteDataSource(
            users = listOf(alice),
            delayMillis = 1_000
        )

        val job = launch { remote.fetchUsers() }
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(1, remote.startedFetches)
        assertEquals(0, remote.completedFetches)
        assertEquals(1, remote.cancelledFetches)
        assertEquals(100, currentTime)
    }

    @Test
    fun `repository observeUsers is the local table not the remote`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(users = listOf(bob))
        val repository = CachedUserRepository(local, remote)
        val emitted = mutableListOf<List<CachedUser>>()

        val job = launch { repository.observeUsers().collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice)), emitted)
        assertEquals(0, remote.startedFetches)
        job.cancelAndJoin()
    }

    @Test
    fun `repository refresh writes remote into local`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(users = listOf(bob, cara))
        val repository = CachedUserRepository(local, remote)

        repository.refresh()

        assertEquals(listOf(bob, cara), local.currentUsers())
        assertEquals(1, remote.completedFetches)
    }

    @Test
    fun `repository refresh is visible through observeUsers`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(users = listOf(bob))
        val repository = CachedUserRepository(local, remote)
        val emitted = mutableListOf<List<CachedUser>>()

        val job = launch { repository.observeUsers().collect { emitted += it } }
        testScheduler.runCurrent()
        repository.refresh()
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice), listOf(bob)), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `repository refresh failure leaves cache unchanged`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(
            users = listOf(bob),
            failure = RemoteUsersException("timeout")
        )
        val repository = CachedUserRepository(local, remote)

        assertFailsWith<RemoteUsersException> { repository.refresh() }
        assertEquals(listOf(alice), local.currentUsers())
        assertEquals(0, remote.completedFetches)
    }

    @Test
    fun `repository refresh does not treat cancellation as success`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(
            users = listOf(bob),
            delayMillis = 1_000
        )
        val repository = CachedUserRepository(local, remote)

        val job = launch { repository.refresh() }
        testScheduler.advanceTimeBy(200)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(listOf(alice), local.currentUsers())
        assertEquals(1, remote.cancelledFetches)
        assertEquals(0, remote.completedFetches)
    }

    @Test
    fun `repository local upsert is visible without refresh`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(users = listOf(alice))
        val repository = CachedUserRepository(local, remote)
        val emitted = mutableListOf<List<CachedUser>>()

        val job = launch { repository.observeUsers().collect { emitted += it } }
        testScheduler.runCurrent()
        local.upsert(bob)
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice), listOf(alice, bob)), emitted)
        assertEquals(0, remote.startedFetches)
        job.cancelAndJoin()
    }

    @Test
    fun `repository two collectors both receive the current cache`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource()
        val repository = CachedUserRepository(local, remote)
        val first = mutableListOf<List<CachedUser>>()
        val second = mutableListOf<List<CachedUser>>()

        val job1 = launch { repository.observeUsers().collect { first += it } }
        val job2 = launch { repository.observeUsers().collect { second += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice)), first)
        assertEquals(listOf(listOf(alice)), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `repository successful refresh drops stale local rows`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice, bob))
        val remote = FakeUserRemoteDataSource(users = listOf(cara))
        val repository = CachedUserRepository(local, remote)

        repository.refresh()

        assertEquals(listOf(cara), local.currentUsers())
    }

    @Test
    fun `repository setUsers then refresh replaces cache`() = runTest {
        val local = InMemoryUserLocalDataSource()
        val remote = FakeUserRemoteDataSource(users = listOf(alice))
        val repository = CachedUserRepository(local, remote)

        repository.refresh()
        remote.setUsers(listOf(bob))
        repository.refresh()

        assertEquals(listOf(bob), local.currentUsers())
        assertEquals(2, remote.completedFetches)
    }
}

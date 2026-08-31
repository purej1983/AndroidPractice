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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StateManagementPracticeTest {

    private val alice = CachedUser("u1", "Alice")
    private val bob = CachedUser("u2", "Bob")

    private fun controller(
        local: InMemoryUserLocalDataSource = InMemoryUserLocalDataSource(),
        remote: FakeUserRemoteDataSource = FakeUserRemoteDataSource(),
        scope: kotlinx.coroutines.CoroutineScope
    ): UsersController {
        return UsersController(CachedUserRepository(local, remote), scope)
    }

    @Test
    fun `state starts as initial not loading`() = runTest {
        val vm = controller(scope = backgroundScope)
        testScheduler.runCurrent()

        assertEquals(UsersScreenState(), vm.state.value)
    }

    @Test
    fun `state is not a MutableStateFlow`() = runTest {
        val vm = controller(scope = backgroundScope)
        val state: StateFlow<UsersScreenState> = vm.state

        assertTrue(state !is MutableStateFlow)
    }

    @Test
    fun `seeded cache is visible before load`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val vm = controller(local = local, scope = backgroundScope)
        testScheduler.runCurrent()

        assertEquals(listOf(alice), vm.state.value.users)
        assertEquals(false, vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `load goes Initial Loading Success`() = runTest {
        val remote = FakeUserRemoteDataSource(users = listOf(alice, bob), delayMillis = 100)
        val vm = controller(remote = remote, scope = backgroundScope)
        val emitted = mutableListOf<UsersScreenState>()

        val job = launch { vm.state.collect { emitted += it } }
        testScheduler.runCurrent()
        vm.onAction(UsersAction.Load)
        testScheduler.runCurrent()

        assertEquals(
            UsersScreenState(loading = true),
            emitted.last()
        )

        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        assertEquals(
            UsersScreenState(loading = false, users = listOf(alice, bob)),
            vm.state.value
        )
        job.cancelAndJoin()
    }

    @Test
    fun `load failure sets error and keeps cache`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(failure = RemoteUsersException("offline"))
        val vm = controller(local, remote, backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(UsersAction.Load)
        testScheduler.runCurrent()

        assertEquals(
            UsersScreenState(
                loading = false,
                users = listOf(alice),
                error = "offline"
            ),
            vm.state.value
        )
    }

    @Test
    fun `retry after error can succeed`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(failure = RemoteUsersException("offline"))
        val vm = controller(local, remote, backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(UsersAction.Load)
        testScheduler.runCurrent()
        remote.setFailure(null)
        remote.setUsers(listOf(bob))
        vm.onAction(UsersAction.Retry)
        testScheduler.runCurrent()

        assertEquals(
            UsersScreenState(loading = false, users = listOf(bob), error = null),
            vm.state.value
        )
    }

    @Test
    fun `loading keeps previous users on screen`() = runTest {
        val local = InMemoryUserLocalDataSource(listOf(alice))
        val remote = FakeUserRemoteDataSource(users = listOf(bob), delayMillis = 500)
        val vm = controller(local, remote, backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(UsersAction.Load)
        testScheduler.runCurrent()

        assertEquals(true, vm.state.value.loading)
        assertEquals(listOf(alice), vm.state.value.users)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `second load cancels the in-flight refresh`() = runTest {
        val remote = FakeUserRemoteDataSource(
            users = listOf(alice),
            delayMillis = 1_000
        )
        val vm = controller(remote = remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(UsersAction.Load)
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        remote.setUsers(listOf(bob))
        vm.onAction(UsersAction.Load)
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertEquals(2, remote.startedFetches)
        assertEquals(1, remote.cancelledFetches)
        assertEquals(1, remote.completedFetches)
        assertEquals(listOf(bob), vm.state.value.users)
        assertEquals(1_100, currentTime)
    }

    @Test
    fun `cancelled load is not recorded as an error`() = runTest {
        val remote = FakeUserRemoteDataSource(
            users = listOf(alice),
            delayMillis = 1_000
        )
        val vm = controller(remote = remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(UsersAction.Load)
        testScheduler.advanceTimeBy(50)
        testScheduler.runCurrent()
        vm.onAction(UsersAction.Load)
        testScheduler.runCurrent()

        assertNull(vm.state.value.error)
        assertEquals(true, vm.state.value.loading)
    }

    @Test
    fun `load and retry are the same action`() = runTest {
        val remote = FakeUserRemoteDataSource(users = listOf(alice))
        val vm = controller(remote = remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(UsersAction.Retry)
        testScheduler.runCurrent()

        assertEquals(listOf(alice), vm.state.value.users)
        assertEquals(1, remote.completedFetches)
    }
}

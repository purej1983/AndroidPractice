package practice.week3

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
class StateFlowPracticeTest {

    private val alice = User("u1", "Alice")
    private val bob = User("u2", "Bob")
    private val cara = User("u3", "Cara")

    @Test
    fun `stateOf current value is the initial value`() = runTest {
        val state = StateFlowPractice.stateOf("ready")

        assertEquals("ready", state.value)
    }

    @Test
    fun `stateOf collector receives the initial value immediately`() = runTest {
        val state = StateFlowPractice.stateOf(UiState())
        val emitted = mutableListOf<UiState>()

        val job = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(UiState()), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `stateOf two collectors both receive the current value`() = runTest {
        val state = StateFlowPractice.stateOf(7)
        val first = mutableListOf<Int>()
        val second = mutableListOf<Int>()

        val job1 = launch { state.collect { first += it } }
        val job2 = launch { state.collect { second += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(7), first)
        assertEquals(listOf(7), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `stateOf does not complete after emitting the current value`() = runTest {
        val state = StateFlowPractice.stateOf("ready")
        val emitted = mutableListOf<String>()

        val job = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("ready"), emitted)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `stateOf is not a MutableStateFlow`() = runTest {
        val state: StateFlow<String> = StateFlowPractice.stateOf("ready")

        assertTrue(state !is MutableStateFlow)
    }

    @Test
    fun `readOnly current value matches the mutable source`() = runTest {
        val mutable = MutableStateFlow(UiState(users = listOf(alice)))

        assertEquals(
            UiState(users = listOf(alice)),
            StateFlowPractice.readOnly(mutable).value
        )
    }

    @Test
    fun `readOnly later assignment is visible`() = runTest {
        val mutable = MutableStateFlow(UiState())
        val state = StateFlowPractice.readOnly(mutable)
        val emitted = mutableListOf<UiState>()

        val job = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()
        mutable.value = UiState(users = listOf(bob))
        testScheduler.runCurrent()

        assertEquals(
            listOf(UiState(), UiState(users = listOf(bob))),
            emitted
        )
        assertEquals(UiState(users = listOf(bob)), state.value)
        job.cancelAndJoin()
    }

    @Test
    fun `readOnly is not a MutableStateFlow`() = runTest {
        val mutable = MutableStateFlow(UiState())

        assertTrue(StateFlowPractice.readOnly(mutable) !is MutableStateFlow)
    }

    @Test
    fun `readOnly two collectors both receive an update`() = runTest {
        val mutable = MutableStateFlow("idle")
        val state = StateFlowPractice.readOnly(mutable)
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { state.collect { first += it } }
        val job2 = launch { state.collect { second += it } }
        testScheduler.runCurrent()
        mutable.value = "loaded"
        testScheduler.runCurrent()

        assertEquals(listOf("idle", "loaded"), first)
        assertEquals(listOf("idle", "loaded"), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `markLoading sets loading and clears error on the default state`() = runTest {
        val state = MutableStateFlow(UiState())

        StateFlowPractice.markLoading(state)

        assertEquals(UiState(loading = true), state.value)
        assertNull(state.value.error)
    }

    @Test
    fun `markLoading keeps users and clears error`() = runTest {
        val state = MutableStateFlow(
            UiState(users = listOf(alice), error = "old")
        )

        StateFlowPractice.markLoading(state)

        assertEquals(
            UiState(loading = true, users = listOf(alice), error = null),
            state.value
        )
    }

    @Test
    fun `markSuccess sets users and clears loading and error`() = runTest {
        val state = MutableStateFlow(UiState(loading = true, error = "old"))

        StateFlowPractice.markSuccess(state, listOf(alice, bob))

        assertEquals(
            UiState(loading = false, users = listOf(alice, bob), error = null),
            state.value
        )
    }

    @Test
    fun `markSuccess replaces previous users`() = runTest {
        val state = MutableStateFlow(UiState(users = listOf(alice)))

        StateFlowPractice.markSuccess(state, listOf(bob, cara))

        assertEquals(listOf(bob, cara), state.value.users)
    }

    @Test
    fun `markSuccess conflates an equal consecutive state`() = runTest {
        val state = MutableStateFlow(UiState(users = listOf(alice)))
        val emitted = mutableListOf<UiState>()

        val job = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()
        StateFlowPractice.markSuccess(state, listOf(alice))
        testScheduler.runCurrent()

        assertEquals(listOf(UiState(users = listOf(alice))), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `markError sets the message and clears loading`() = runTest {
        val state = MutableStateFlow(UiState(loading = true))

        StateFlowPractice.markError(state, "network down")

        assertEquals(
            UiState(loading = false, error = "network down"),
            state.value
        )
    }

    @Test
    fun `markError keeps existing users`() = runTest {
        val state = MutableStateFlow(
            UiState(loading = true, users = listOf(alice, bob))
        )

        StateFlowPractice.markError(state, "timeout")

        assertEquals(
            UiState(loading = false, users = listOf(alice, bob), error = "timeout"),
            state.value
        )
    }

    @Test
    fun `refreshUsers moves initial to loading then success`() = runTest {
        val state = MutableStateFlow(UiState())
        val api = FakeUserApi(users = listOf(alice, bob), delayMillis = 1_000)
        val emitted = mutableListOf<UiState>()

        val collector = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()

        val refresh = launch { StateFlowPractice.refreshUsers(state, api) }
        testScheduler.runCurrent()
        assertEquals(
            listOf(UiState(), UiState(loading = true)),
            emitted
        )

        refresh.join()
        assertEquals(
            listOf(
                UiState(),
                UiState(loading = true),
                UiState(users = listOf(alice, bob))
            ),
            emitted
        )
        assertEquals(1, api.startedLoads)
        assertEquals(1, api.completedLoads)
        collector.cancelAndJoin()
    }

    @Test
    fun `refreshUsers delays before success so virtual time matches`() = runTest {
        val state = MutableStateFlow(UiState())
        val api = FakeUserApi(users = listOf(alice), delayMillis = 1_000)

        StateFlowPractice.refreshUsers(state, api)

        assertEquals(UiState(users = listOf(alice)), state.value)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `refreshUsers keeps users visible while loading and clears error`() = runTest {
        val state = MutableStateFlow(
            UiState(users = listOf(alice), error = "old")
        )
        val api = FakeUserApi(users = listOf(alice, bob), delayMillis = 1_000)

        val job = launch { StateFlowPractice.refreshUsers(state, api) }
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()

        assertEquals(
            UiState(loading = true, users = listOf(alice), error = null),
            state.value
        )
        assertEquals(1, api.startedLoads)
        assertEquals(0, api.completedLoads)

        job.join()
        assertEquals(UiState(users = listOf(alice, bob)), state.value)
    }

    @Test
    fun `refreshUsers failure keeps cached users and sets error`() = runTest {
        val state = MutableStateFlow(UiState(users = listOf(alice)))
        val api = FakeUserApi(failure = LoadUsersException("network down"))

        StateFlowPractice.refreshUsers(state, api)

        assertEquals(
            UiState(users = listOf(alice), error = "network down"),
            state.value
        )
        assertEquals(1, api.startedLoads)
        assertEquals(0, api.completedLoads)
    }

    @Test
    fun `refreshUsers cancel during load is not a load error`() = runTest {
        val state = MutableStateFlow(UiState(users = listOf(alice)))
        val api = FakeUserApi(users = listOf(alice, bob), delayMillis = 1_000)

        val job = launch { StateFlowPractice.refreshUsers(state, api) }
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(
            UiState(loading = true, users = listOf(alice), error = null),
            state.value
        )
        assertEquals(1, api.startedLoads)
        assertEquals(0, api.completedLoads)
        assertEquals(1, api.cancelledLoads)
        assertEquals(500, currentTime)
    }

    @Test
    fun `refreshUsers late collector after success receives only current state`() = runTest {
        val state = MutableStateFlow(UiState())
        val api = FakeUserApi(users = listOf(alice))

        StateFlowPractice.refreshUsers(state, api)

        val late = mutableListOf<UiState>()
        val job = launch { state.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(UiState(users = listOf(alice))), late)
        job.cancelAndJoin()
    }

    @Test
    fun `refreshUsers late collector during load receives current loading state`() = runTest {
        val state = MutableStateFlow(UiState(users = listOf(alice)))
        val api = FakeUserApi(users = listOf(alice, bob), delayMillis = 1_000)

        val refresh = launch { StateFlowPractice.refreshUsers(state, api) }
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()

        val late = mutableListOf<UiState>()
        val collector = launch { state.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(
            listOf(UiState(loading = true, users = listOf(alice))),
            late
        )

        collector.cancelAndJoin()
        refresh.cancelAndJoin()
    }

    @Test
    fun `refreshUsers does not load until it is called`() = runTest {
        val api = FakeUserApi(users = listOf(alice))
        val state = MutableStateFlow(UiState())

        assertEquals(0, api.startedLoads)
        assertEquals(UiState(), state.value)
    }

    @Test
    fun `shareEagerly starts without a StateFlow collector`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice)),
            delayMillis = 1_000
        )

        val state = StateFlowPractice.shareEagerly(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        assertEquals(emptyList(), state.value)
        assertEquals(0, currentTime)
    }

    @Test
    fun `shareEagerly stays at initial until the first emission`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareEagerly(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )

        testScheduler.advanceTimeBy(999)
        testScheduler.runCurrent()
        assertEquals(emptyList(), state.value)

        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertEquals(listOf(alice), state.value)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `shareEagerly collector from the start receives initial then updates`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice), listOf(alice, bob)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareEagerly(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )
        val emitted = mutableListOf<List<User>>()

        val job = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()
        assertEquals(listOf(emptyList()), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(listOf(emptyList(), listOf(alice)), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(
            listOf(emptyList(), listOf(alice), listOf(alice, bob)),
            emitted
        )
        job.cancelAndJoin()
    }

    @Test
    fun `shareEagerly late collector receives only the current value`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice), listOf(alice, bob)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareEagerly(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        val late = mutableListOf<List<User>>()
        val job = launch { state.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice, bob)), late)
        job.cancelAndJoin()
    }

    @Test
    fun `shareEagerly two collectors share one upstream`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareEagerly(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )
        val first = mutableListOf<List<User>>()
        val second = mutableListOf<List<User>>()

        val job1 = launch { state.collect { first += it } }
        val job2 = launch { state.collect { second += it } }
        testScheduler.advanceUntilIdle()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        assertEquals(listOf(emptyList(), listOf(alice)), first)
        assertEquals(listOf(emptyList(), listOf(alice)), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `shareEagerly keeps the last value after upstream completes`() = runTest {
        val feed = FakeUserFeed(snapshots = listOf(listOf(alice, bob)))
        val state = StateFlowPractice.shareEagerly(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )

        testScheduler.advanceUntilIdle()
        assertEquals(listOf(alice, bob), state.value)

        val late = mutableListOf<List<User>>()
        val job = launch { state.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice, bob)), late)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `shareLazily does not start without a collector`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareLazily(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )
        testScheduler.runCurrent()

        assertEquals(0, feed.collectCount)
        assertEquals(emptyList(), feed.readIndexes)
        assertEquals(emptyList(), state.value)
    }

    @Test
    fun `shareLazily starts when the first collector appears`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareLazily(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )
        val emitted = mutableListOf<List<User>>()

        val job = launch { state.collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        assertEquals(listOf(emptyList<User>()), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(listOf(emptyList(), listOf(alice)), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `shareLazily second collector does not restart upstream`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareLazily(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )

        val job1 = launch { state.collect { } }
        testScheduler.runCurrent()
        val job2 = launch { state.collect { } }
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `shareLazily late collector receives only the current value`() = runTest {
        val feed = FakeUserFeed(
            snapshots = listOf(listOf(alice), listOf(alice, bob)),
            delayMillis = 1_000
        )
        val state = StateFlowPractice.shareLazily(
            feed.observeUsers(),
            scope = backgroundScope,
            initial = emptyList()
        )

        val first = launch { state.collect { } }
        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        val late = mutableListOf<List<User>>()
        val second = launch { state.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(alice, bob)), late)
        assertEquals(1, feed.collectCount)
        first.cancelAndJoin()
        second.cancelAndJoin()
    }
}

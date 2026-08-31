package practice.week4

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskManagerPracticeTest {

    private val inbox = Task("t1", "Inbox zero", completed = false, updatedAt = 1L)
    private val ship = Task("t2", "Ship build", completed = false, updatedAt = 2L)
    private val cats = Task("t3", "Feed the cats", completed = false, updatedAt = 3L)

    private fun manager(
        local: InMemoryTaskStore = InMemoryTaskStore(),
        remote: FakeTaskRemote = FakeTaskRemote(),
        debounceMillis: Long = 0L,
        clock: () -> Long = { 10L },
        ids: () -> String = { "new" },
        scope: kotlinx.coroutines.CoroutineScope
    ): TaskManager {
        return TaskManager(local, remote, scope, debounceMillis, clock, ids)
    }

    @Test
    fun `state is StateFlow not MutableStateFlow`() = runTest {
        val vm = manager(scope = backgroundScope)
        val state: StateFlow<TaskUiState> = vm.state

        assertTrue(state !is MutableStateFlow)
    }

    @Test
    fun `events is SharedFlow not MutableSharedFlow`() = runTest {
        val vm = manager(scope = backgroundScope)
        val events: SharedFlow<TaskEvent> = vm.events

        assertTrue(events !is MutableSharedFlow)
    }

    @Test
    fun `cached tasks are visible before refresh`() = runTest {
        val local = InMemoryTaskStore(listOf(inbox, ship))
        val remote = FakeTaskRemote(tasks = listOf(cats), fetchDelayMillis = 5_000)
        val vm = manager(local, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        assertEquals(listOf(inbox, ship), vm.state.value.tasks)
        assertEquals(0, remote.fetchCount)
    }

    @Test
    fun `refresh success replaces the cache`() = runTest {
        val local = InMemoryTaskStore(listOf(inbox))
        val remote = FakeTaskRemote(tasks = listOf(ship, cats))
        val vm = manager(local, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.refresh()
        testScheduler.runCurrent()

        assertEquals(listOf(ship, cats), vm.state.value.tasks)
        assertEquals(listOf(ship, cats), local.current())
        assertEquals(false, vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `refresh failure keeps cached tasks`() = runTest {
        val local = InMemoryTaskStore(listOf(inbox, ship))
        val remote = FakeTaskRemote(fetchFailure = TaskRemoteException("offline"))
        val vm = manager(local, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.refresh()
        testScheduler.runCurrent()

        assertEquals(listOf(inbox, ship), vm.state.value.tasks)
        assertEquals("offline", vm.state.value.error)
        assertEquals(false, vm.state.value.loading)
    }

    @Test
    fun `load marks loading until the network returns`() = runTest {
        val remote = FakeTaskRemote(tasks = listOf(inbox), fetchDelayMillis = 200)
        val vm = manager(remote = remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.load()
        testScheduler.runCurrent()
        assertEquals(true, vm.state.value.loading)

        testScheduler.advanceTimeBy(200)
        testScheduler.runCurrent()
        assertEquals(false, vm.state.value.loading)
        assertEquals(listOf(inbox), vm.state.value.tasks)
    }

    @Test
    fun `add writes locally even if remote save fails`() = runTest {
        val local = InMemoryTaskStore()
        val remote = FakeTaskRemote(saveFailure = TaskRemoteException("save failed"))
        val vm = manager(local, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.add("Write tests")
        testScheduler.runCurrent()

        assertEquals(
            listOf(Task("new", "Write tests", completed = false, updatedAt = 10L)),
            local.current()
        )
        assertEquals(emptyList(), remote.savedTasks)
    }

    @Test
    fun `save success emits Saved once to a live collector`() = runTest {
        val vm = manager(scope = backgroundScope)
        val received = mutableListOf<TaskEvent>()
        val job = launch { vm.events.collect { received += it } }
        testScheduler.runCurrent()

        vm.add("Write tests")
        testScheduler.runCurrent()

        assertEquals(listOf<TaskEvent>(TaskEvent.Saved("Write tests")), received)
        job.cancelAndJoin()
    }

    @Test
    fun `late collector does not replay Saved`() = runTest {
        val vm = manager(scope = backgroundScope)
        testScheduler.runCurrent()

        vm.add("Write tests")
        testScheduler.runCurrent()

        val received = mutableListOf<TaskEvent>()
        val job = launch { vm.events.collect { received += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), received)
        job.cancelAndJoin()
    }

    @Test
    fun `save failure emits ShowError once`() = runTest {
        val remote = FakeTaskRemote(saveFailure = TaskRemoteException("disk full"))
        val vm = manager(remote = remote, scope = backgroundScope)
        val received = mutableListOf<TaskEvent>()
        val job = launch { vm.events.collect { received += it } }
        testScheduler.runCurrent()

        vm.add("Write tests")
        testScheduler.runCurrent()

        assertEquals(listOf<TaskEvent>(TaskEvent.ShowError("disk full")), received)
        job.cancelAndJoin()
    }

    @Test
    fun `complete marks the task done in the cache`() = runTest {
        val local = InMemoryTaskStore(listOf(inbox, ship))
        val vm = manager(local = local, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.complete("t1")
        testScheduler.runCurrent()

        assertEquals(
            listOf(
                ship,
                inbox.copy(completed = true, updatedAt = 10L)
            ),
            local.current()
        )
        assertEquals(true, vm.state.value.tasks.single { it.id == "t1" }.completed)
    }

    @Test
    fun `search does not run on every keystroke`() = runTest {
        val remote = FakeTaskRemote(tasks = listOf(cats))
        val vm = manager(remote = remote, debounceMillis = 300, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.search("c")
        testScheduler.advanceTimeBy(50)
        vm.search("ca")
        testScheduler.advanceTimeBy(50)
        vm.search("cat")
        testScheduler.advanceTimeBy(50)
        vm.search("cats")
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        assertEquals(listOf("cats"), remote.startedSearches)
        assertEquals(listOf(cats), vm.state.value.tasks)
    }

    @Test
    fun `old search response cannot replace a newer result`() = runTest {
        val remote = FakeTaskRemote(
            tasks = listOf(inbox, cats),
            searchDelayByQuery = mapOf(
                "cat" to 1_000L,
                "cats" to 100L
            )
        )
        val vm = manager(remote = remote, debounceMillis = 0, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.search("cat")
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        vm.search("cats")
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertEquals(listOf("cat", "cats"), remote.startedSearches)
        assertEquals(listOf("cat"), remote.cancelledSearches)
        assertEquals(listOf("cats"), remote.completedSearches)
        assertEquals(listOf(cats), vm.state.value.tasks)
        assertEquals("cats", vm.state.value.query)
    }

    @Test
    fun `blank search restores cached tasks without a network search`() = runTest {
        val local = InMemoryTaskStore(listOf(inbox, ship))
        val remote = FakeTaskRemote(tasks = listOf(inbox, ship, cats))
        val vm = manager(local, remote, debounceMillis = 0, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.search("cats")
        testScheduler.runCurrent()
        assertEquals(listOf(cats), vm.state.value.tasks)
        assertEquals(listOf("cats"), remote.startedSearches)

        vm.search("")
        testScheduler.runCurrent()

        assertEquals(listOf(inbox, ship), vm.state.value.tasks)
        assertEquals(listOf("cats"), remote.startedSearches)
        assertEquals("", vm.state.value.query)
    }

    @Test
    fun `two Saved events in a row are both delivered`() = runTest {
        var nextId = 0
        val vm = manager(ids = { "id${nextId++}" }, scope = backgroundScope)
        val received = mutableListOf<TaskEvent>()
        val job = launch { vm.events.collect { received += it } }
        testScheduler.runCurrent()

        vm.add("One")
        testScheduler.runCurrent()
        vm.add("Two")
        testScheduler.runCurrent()

        assertEquals(
            listOf<TaskEvent>(TaskEvent.Saved("One"), TaskEvent.Saved("Two")),
            received
        )
        job.cancelAndJoin()
    }
}

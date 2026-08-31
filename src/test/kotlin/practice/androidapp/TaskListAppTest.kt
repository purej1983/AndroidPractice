package practice.androidapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import practice.week4.FakeTaskRemote
import practice.week4.InMemoryTaskStore
import practice.week4.Task
import practice.week4.TaskEvent
import practice.week4.TaskRemoteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListAppTest {

    private val inbox = Task("t1", "Inbox zero", completed = false, updatedAt = 1L)
    private val cats = Task("t3", "Feed the cats", completed = false, updatedAt = 3L)

    private fun viewModel(
        store: InMemoryTaskStore = InMemoryTaskStore(),
        remote: FakeTaskRemote = FakeTaskRemote(),
        debounceMillis: Long = 0L,
        clock: () -> Long = { 10L },
        ids: () -> String = { "new" },
        scope: kotlinx.coroutines.CoroutineScope
    ): TaskListViewModel {
        val repository = TaskListRepository(InMemoryTaskDao(store), FakeTaskApi(remote))
        return TaskListViewModel(repository, scope, debounceMillis, clock, ids)
    }

    @Test
    fun `ViewModel exposes read-only state and events`() = runTest {
        val vm = viewModel(scope = backgroundScope)

        assertTrue(vm.state !is MutableStateFlow)
        assertTrue(vm.events !is MutableSharedFlow)
    }

    @Test
    fun `Dao maps Room entities to domain on observe`() = runTest {
        val store = InMemoryTaskStore(listOf(inbox))
        val dao = InMemoryTaskDao(store)
        val emitted = mutableListOf<List<TaskEntity>>()
        val job = launch { dao.observeTasks().collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(listOf(inbox.toEntity())), emitted)
        assertEquals(listOf(inbox.toEntity()), dao.current())
        job.cancel()
    }

    @Test
    fun `cached tasks render before any network call`() = runTest {
        val store = InMemoryTaskStore(listOf(inbox))
        val remote = FakeTaskRemote(tasks = listOf(cats), fetchDelayMillis = 5_000)
        val vm = viewModel(store, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        val screen = renderTaskList(TaskListScreenSpec.from(vm))

        assertEquals("idle query= [ ] Inbox zero", screen)
        assertEquals(0, remote.fetchCount)
    }

    @Test
    fun `Refresh action loads remote into the Dao`() = runTest {
        val store = InMemoryTaskStore(listOf(inbox))
        val remote = FakeTaskRemote(tasks = listOf(cats))
        val vm = viewModel(store, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(TaskListAction.Refresh)
        testScheduler.runCurrent()

        assertEquals(listOf(cats), vm.state.value.tasks)
        assertEquals(listOf(cats), store.current())
        assertEquals("idle query= [ ] Feed the cats", renderTaskList(TaskListScreenSpec.from(vm)))
    }

    @Test
    fun `Refresh failure keeps cache and shows error in the screen spec`() = runTest {
        val store = InMemoryTaskStore(listOf(inbox))
        val remote = FakeTaskRemote(fetchFailure = TaskRemoteException("offline"))
        val vm = viewModel(store, remote, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(TaskListAction.Refresh)
        testScheduler.runCurrent()

        assertEquals(listOf(inbox), vm.state.value.tasks)
        assertEquals("error:offline query= [ ] Inbox zero", renderTaskList(TaskListScreenSpec.from(vm)))
    }

    @Test
    fun `Add action writes locally and emits a one-shot Saved event`() = runTest {
        val vm = viewModel(scope = backgroundScope)
        val received = mutableListOf<TaskEvent>()
        val job = launch { vm.events.collect { received += it } }
        testScheduler.runCurrent()

        vm.onAction(TaskListAction.Add("Write tests"))
        testScheduler.runCurrent()

        assertEquals(
            listOf(Task("new", "Write tests", completed = false, updatedAt = 10L)),
            vm.state.value.tasks
        )
        assertEquals(listOf<TaskEvent>(TaskEvent.Saved("Write tests")), received)
        job.cancel()
    }

    @Test
    fun `Complete action marks the row done for Compose to render`() = runTest {
        val store = InMemoryTaskStore(listOf(inbox))
        val vm = viewModel(store = store, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(TaskListAction.Complete("t1"))
        testScheduler.runCurrent()

        assertTrue(vm.state.value.tasks.single().completed)
        assertEquals("idle query= [x] Inbox zero", renderTaskList(TaskListScreenSpec.from(vm)))
    }

    @Test
    fun `QueryChanged does not search on every keystroke`() = runTest {
        val remote = FakeTaskRemote(tasks = listOf(cats))
        val vm = viewModel(remote = remote, debounceMillis = 300, scope = backgroundScope)
        testScheduler.runCurrent()

        vm.onAction(TaskListAction.QueryChanged("c"))
        testScheduler.advanceTimeBy(50)
        vm.onAction(TaskListAction.QueryChanged("ca"))
        testScheduler.advanceTimeBy(50)
        vm.onAction(TaskListAction.QueryChanged("cats"))
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        assertEquals(listOf("cats"), remote.startedSearches)
        assertEquals(listOf(cats), vm.state.value.tasks)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `screen spec is a function of state not of the repository`() = runTest {
        val vm = viewModel(
            store = InMemoryTaskStore(listOf(inbox)),
            scope = backgroundScope
        )
        testScheduler.runCurrent()

        val spec = TaskListScreenSpec.from(vm)
        val first = renderTaskList(spec)
        val second = renderTaskList(spec)

        assertEquals(first, second)
        assertTrue(first.startsWith("idle"))
    }
}

package practice.week4

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

/**
 * Day 20 — Task Manager capstone.
 *
 * You choose the stream type. The tests describe behaviour, not APIs:
 * - The UI can always read current task state (state holder).
 * - Save success notifies once; a late collector must not replay it.
 * - Search does not run on every keystroke.
 * - An old search response cannot replace a newer one.
 * - Cached tasks remain if refresh fails.
 *
 * Decide which primitive fits each surface, then defend it:
 * - [state] must be readable as current UI state (`.value`).
 * - [events] must be one-shot (snackbars). A late collector must not
 *   replay a past Saved. Two current collectors may both need it.
 * - Local table is the source of truth; remote writes into it.
 * - Search should not start a request per keystroke, and latest query
 *   must win.
 * Channel would be wrong for UI state (queue, one consumer) and wrong
 * for snackbars that two collectors might both need.
 */
data class Task(
    val id: String,
    val title: String,
    val completed: Boolean,
    val updatedAt: Long
)

data class TaskUiState(
    val loading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val query: String = "",
    val error: String? = null
)

sealed interface TaskEvent {
    data class Saved(val title: String) : TaskEvent
    data class ShowError(val message: String) : TaskEvent
}

class TaskRemoteException(reason: String) : IllegalStateException(reason)

interface TaskLocalStore {
    fun observeTasks(): StateFlow<List<Task>>
    fun current(): List<Task>
    suspend fun replaceAll(tasks: List<Task>)
    suspend fun upsert(task: Task)
}

interface TaskRemote {
    suspend fun fetchAll(): List<Task>
    suspend fun save(task: Task)
    suspend fun search(query: String): List<Task>
}

/**
 * In-memory Room stand-in. Current snapshot is always readable.
 */
class InMemoryTaskStore(
    initial: List<Task> = emptyList()
) : TaskLocalStore {
    private val table = MutableStateFlow(initial.toList())

    override fun observeTasks(): StateFlow<List<Task>> = table.asStateFlow()

    override fun current(): List<Task> = table.value

    override suspend fun replaceAll(tasks: List<Task>) {
        table.value = tasks.toList()
    }

    override suspend fun upsert(task: Task) {
        table.update { current ->
            current.filterNot { it.id == task.id } + task
        }
    }
}

/**
 * Network stand-in. [searchDelayByQuery] makes a slow older query lose
 * to a faster newer one when cancellation is wired correctly.
 */
class FakeTaskRemote(
    private var tasks: List<Task> = emptyList(),
    private val fetchDelayMillis: Long = 0L,
    private val saveDelayMillis: Long = 0L,
    private val searchDelayMillis: Long = 0L,
    private val searchDelayByQuery: Map<String, Long> = emptyMap(),
    private var fetchFailure: Throwable? = null,
    private var saveFailure: Throwable? = null,
    private val searchFailures: Map<String, Throwable> = emptyMap()
) : TaskRemote {
    private val _fetched = mutableListOf<Unit>()
    private val _saved = mutableListOf<Task>()
    private val _startedSearches = mutableListOf<String>()
    private val _completedSearches = mutableListOf<String>()
    private val _cancelledSearches = mutableListOf<String>()

    val fetchCount: Int get() = _fetched.size
    val savedTasks: List<Task> get() = _saved.toList()
    val startedSearches: List<String> get() = _startedSearches.toList()
    val completedSearches: List<String> get() = _completedSearches.toList()
    val cancelledSearches: List<String> get() = _cancelledSearches.toList()

    fun setTasks(next: List<Task>) {
        tasks = next.toList()
    }

    fun setFetchFailure(error: Throwable?) {
        fetchFailure = error
    }

    override suspend fun fetchAll(): List<Task> {
        _fetched += Unit
        try {
            delay(fetchDelayMillis)
            fetchFailure?.let { throw it }
            return tasks.toList()
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    override suspend fun save(task: Task) {
        try {
            delay(saveDelayMillis)
            saveFailure?.let { throw it }
            _saved += task
            tasks = tasks.filterNot { it.id == task.id } + task
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    override suspend fun search(query: String): List<Task> {
        _startedSearches += query
        try {
            delay(searchDelayByQuery[query] ?: searchDelayMillis)
            searchFailures[query]?.let { throw it }
            _completedSearches += query
            val needle = query.lowercase()
            return tasks.filter { it.title.lowercase().contains(needle) }
        } catch (cancelled: CancellationException) {
            _cancelledSearches += query
            throw cancelled
        }
    }
}

/**
 * Capstone controller. Wire [state], [events], load/refresh, add,
 * complete, and search. See file KDoc for the behaviour to defend.
 */
class TaskManager(
    private val local: TaskLocalStore,
    private val remote: TaskRemote,
    private val scope: CoroutineScope,
    debounceMillis: Long,
    private val clock: () -> Long,
    private val ids: () -> String
) {
    val state: StateFlow<TaskUiState> = TODO()

    val events: SharedFlow<TaskEvent> = TODO()

    fun load() {
        TODO()
    }

    fun refresh() {
        TODO()
    }

    fun add(title: String) {
        TODO()
    }

    fun complete(id: String) {
        TODO()
    }

    fun search(query: String) {
        TODO()
    }
}

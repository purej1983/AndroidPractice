package practice.androidapp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import practice.week4.FakeTaskRemote
import practice.week4.InMemoryTaskStore
import practice.week4.Task
import practice.week4.TaskEvent
import practice.week4.TaskManager
import practice.week4.TaskUiState

/**
 * Follow-on: JVM stand-in for a Compose + ViewModel + Room + network
 * Task List. It **reuses** Week 4 [TaskManager] instead of a second
 * state machine.
 *
 * Real Android mapping: `practice/androidapp/README.md`.
 */

/** Room `@Entity(tableName = "tasks")`. */
data class TaskEntity(
    val id: String,
    val title: String,
    val completed: Boolean,
    val updatedAt: Long
)

/** Retrofit / kotlinx.serialization JSON body. */
data class TaskDto(
    val id: String,
    val title: String,
    val completed: Boolean,
    val updatedAt: Long
)

fun TaskEntity.toDomain(): Task = Task(id, title, completed, updatedAt)

fun TaskDto.toDomain(): Task = Task(id, title, completed, updatedAt)

fun Task.toEntity(): TaskEntity = TaskEntity(id, title, completed, updatedAt)

fun Task.toDto(): TaskDto = TaskDto(id, title, completed, updatedAt)

/**
 * Room `@Dao`. Observe is a Flow so the table stays source of truth.
 */
interface TaskDao {
    fun observeTasks(): Flow<List<TaskEntity>>
    fun current(): List<TaskEntity>
    suspend fun replaceAll(tasks: List<TaskEntity>)
    suspend fun upsert(task: TaskEntity)
}

/** Retrofit interface: suspend, not callbacks. */
interface TaskApi {
    suspend fun getTasks(): List<TaskDto>
    suspend fun putTask(task: TaskDto)
    suspend fun searchTasks(query: String): List<TaskDto>
}

class InMemoryTaskDao(
    val store: InMemoryTaskStore = InMemoryTaskStore()
) : TaskDao {
    override fun observeTasks(): Flow<List<TaskEntity>> =
        store.observeTasks().map { tasks -> tasks.map { it.toEntity() } }

    override fun current(): List<TaskEntity> = store.current().map { it.toEntity() }

    override suspend fun replaceAll(tasks: List<TaskEntity>) {
        store.replaceAll(tasks.map { it.toDomain() })
    }

    override     suspend fun upsert(task: TaskEntity) {
        store.upsert(task.toDomain())
    }
}

class FakeTaskApi(
    val remote: FakeTaskRemote = FakeTaskRemote()
) : TaskApi {
    override suspend fun getTasks(): List<TaskDto> = remote.fetchAll().map { it.toDto() }

    override suspend fun putTask(task: TaskDto) {
        remote.save(task.toDomain())
    }

    override suspend fun searchTasks(query: String): List<TaskDto> =
        remote.search(query).map { it.toDto() }
}

/**
 * Repository: Dao is source of truth, Api writes into Dao.
 * The ViewModel never calls [TaskApi] directly.
 */
class TaskListRepository(
    val dao: InMemoryTaskDao,
    val api: FakeTaskApi
)

sealed interface TaskListAction {
    data object Refresh : TaskListAction
    data class Add(val title: String) : TaskListAction
    data class Complete(val id: String) : TaskListAction
    data class QueryChanged(val query: String) : TaskListAction
}

/**
 * ViewModel stand-in.
 *
 * Android: `class TaskListViewModel @Inject constructor(...) : ViewModel()`.
 * Replace [scope] with `viewModelScope`.
 *
 * The composable only renders [state] and sends [TaskListAction].
 */
class TaskListViewModel(
    repository: TaskListRepository,
    scope: CoroutineScope,
    debounceMillis: Long,
    clock: () -> Long,
    ids: () -> String
) {
    private val manager = TaskManager(
        local = repository.dao.store,
        remote = repository.api.remote,
        scope = scope,
        debounceMillis = debounceMillis,
        clock = clock,
        ids = ids
    )

    val state: StateFlow<TaskUiState> = manager.state
    val events: SharedFlow<TaskEvent> = manager.events

    fun onAction(action: TaskListAction) {
        when (action) {
            TaskListAction.Refresh -> manager.refresh()
            is TaskListAction.Add -> manager.add(action.title)
            is TaskListAction.Complete -> manager.complete(action.id)
            is TaskListAction.QueryChanged -> manager.search(action.query)
        }
    }
}

/**
 * Compose contract. Real file: `@Composable fun TaskListScreen(...)`.
 *
 * 1. State is a parameter (hoisted). Leaves do not own a ViewModel.
 * 2. One-shot events stay on SharedFlow; collect them with
 *    `LaunchedEffect` / `Events.collect` for snackbars.
 * 3. The UI is a function of [TaskUiState] plus event lambdas.
 */
data class TaskListScreenSpec(
    val state: TaskUiState,
    val onRefresh: () -> Unit,
    val onAdd: (String) -> Unit,
    val onComplete: (String) -> Unit,
    val onQueryChange: (String) -> Unit
) {
    companion object {
        fun from(viewModel: TaskListViewModel): TaskListScreenSpec =
            TaskListScreenSpec(
                state = viewModel.state.value,
                onRefresh = { viewModel.onAction(TaskListAction.Refresh) },
                onAdd = { title -> viewModel.onAction(TaskListAction.Add(title)) },
                onComplete = { id -> viewModel.onAction(TaskListAction.Complete(id)) },
                onQueryChange = { query -> viewModel.onAction(TaskListAction.QueryChanged(query)) }
            )
    }
}

fun renderTaskList(spec: TaskListScreenSpec): String {
    val body = spec.state.tasks.joinToString { task ->
        val mark = if (task.completed) "x" else " "
        "[$mark] ${task.title}"
    }
    val header = when {
        spec.state.loading -> "loading"
        spec.state.error != null -> "error:${spec.state.error}"
        else -> "idle"
    }
    return "$header query=${spec.state.query} $body".trim()
}

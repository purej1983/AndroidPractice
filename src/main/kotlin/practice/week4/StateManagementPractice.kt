package practice.week4

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Day 17 — State management.
 *
 * Key ideas:
 * - The UI sends **actions**. The ViewModel owns **state**. The repository
 *   owns **data**. The UI does not call the API.
 * - Expose `StateFlow`, keep `MutableStateFlow` private.
 * - Initial → Loading → Success/Error. Retry is Load again.
 * - Cached users stay on screen while loading or after an error.
 * - Cancellation is not a load error. Do not catch `CancellationException`
 *   as if it were a failed refresh.
 * - A second Load cancels the in-flight refresh so two refreshes do not
 *   fight. That is the same "latest wins" idea as search, applied to retry.
 *
 * Architecture:
 *
 * ```
 * User Action → ViewModel → Repository → Result → UiState → UI
 * ```
 */
data class UsersScreenState(
    val loading: Boolean = false,
    val users: List<CachedUser> = emptyList(),
    val error: String? = null
)

sealed interface UsersAction {
    data object Load : UsersAction
    data object Retry : UsersAction
}

/**
 * Exercise 1 — ViewModel-style controller
 *
 * Collect [UserRepository.observeUsers] into [state] so cache is visible
 * without a successful refresh.
 *
 * [onAction] Load/Retry:
 * 1. Mark loading, clear error, keep users.
 * 2. Call [UserRepository.refresh].
 * 3. On success, clear loading and error.
 * 4. On failure, clear loading, set [UsersScreenState.error], keep users.
 *
 * A new Load/Retry cancels the previous refresh job.
 *
 * Requirement: expose [state] as `StateFlow`, not `MutableStateFlow`.
 * Use `update` for state changes. Do not catch `CancellationException`.
 */
class UsersController(
    private val repository: UserRepository,
    private val scope: CoroutineScope
) {
    init {
        scope.launch {
            repository.observeUsers().collect { users ->
                _state.update { it.copy(users = users) }
            }
        }
    }

    private val _state = MutableStateFlow(UsersScreenState())
    val state: StateFlow<UsersScreenState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun onAction(action: UsersAction) {
        loadJob?.cancel()
        _state.update { state -> state.copy(loading = true) }
        loadJob = scope.launch {
            try {
                repository.refresh()
                repository.observeUsers().collect { users ->
                    _state.update { state -> state.copy(loading = false, users = users, error = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { state -> state.copy(loading = false, error = e.message ?: "Unknown error") }
            }
        }
    }
}

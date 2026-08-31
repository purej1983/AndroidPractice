package practice.week4

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

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
    val state: StateFlow<UsersScreenState> = TODO()

    fun onAction(action: UsersAction) {
        TODO()
    }
}

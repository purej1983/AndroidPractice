package practice.week3

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Day 13 — StateFlow.
 *
 * Key ideas:
 * - StateFlow is a **hot** state holder. It always has a current value.
 * - That is why it needs an initial value, and why it fits UI state:
 *   the screen can always render `state.value`.
 * - A new collector receives the **current** value immediately, not the
 *   full history.
 * - Equal consecutive values are conflated (`equals`). Same idea as
 *   `distinctUntilChanged`.
 * - Expose `StateFlow`, keep `MutableStateFlow` private (`asStateFlow`).
 * - `update` applies a transform atomically. Prefer it over
 *   `value = value.copy(...)`.
 * - `stateIn` turns a cold Flow into a StateFlow. `Eagerly` starts at
 *   once; `Lazily` starts on the first collector.
 * - StateFlow never completes. Do not call `toList()` on it.
 */
data class User(
    val id: String,
    val name: String
)

data class UiState(
    val loading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null
)

class LoadUsersException(val reason: String) : IllegalStateException(reason)

/**
 * One-shot user loader for tests.
 *
 * [loadUsers] records start / completion / cancellation so tests can prove
 * refresh goes Initial → Loading → Success/Error and does not treat
 * cancellation as a load error.
 */
class FakeUserApi(
    private val users: List<User> = emptyList(),
    private val delayMillis: Long = 0L,
    private val failure: Throwable? = null
) {
    private var _startedLoads = 0
    private var _completedLoads = 0
    private var _cancelledLoads = 0

    val startedLoads: Int get() = _startedLoads
    val completedLoads: Int get() = _completedLoads
    val cancelledLoads: Int get() = _cancelledLoads

    suspend fun loadUsers(): List<User> {
        _startedLoads += 1
        try {
            delay(delayMillis.milliseconds)
            failure?.let { throw it }
            _completedLoads += 1
            return users
        } catch (cancelled: CancellationException) {
            _cancelledLoads += 1
            throw cancelled
        }
    }
}

/**
 * Cold user-list feed for tests.
 *
 * [observeUsers] is a cold Flow of snapshots. [collectCount] proves
 * `stateIn(Eagerly)` starts without a StateFlow collector, and that two
 * StateFlow collectors share one upstream instead of replaying it.
 */
class FakeUserFeed(
    private val snapshots: List<List<User>> = emptyList(),
    private val delayMillis: Long = 0L
) {
    private var _collectCount = 0
    private val _readIndexes = mutableListOf<Int>()

    val collectCount: Int get() = _collectCount
    val readIndexes: List<Int> get() = _readIndexes.toList()
    val snapshotCount: Int get() = snapshots.size

    fun observeUsers(): Flow<List<User>> {
        return flow {
            _collectCount += 1
            for (index in snapshots.indices) {
                _readIndexes += index
                delay(delayMillis.milliseconds)
                emit(snapshots[index])
            }
        }
    }
}

object StateFlowPractice {

    /**
     * Exercise 1 — initial value
     *
     * Return a StateFlow whose current value is [initial].
     * Collectors must receive [initial] immediately. The Flow must not
     * complete after that first value.
     *
     * Requirement: use `MutableStateFlow` and `asStateFlow`.
     * Do not use `stateIn`.
     */
    fun <T> stateOf(initial: T): StateFlow<T> {
        return MutableStateFlow(initial).asStateFlow()
    }

    /**
     * Exercise 2 — asStateFlow
     *
     * Return a read-only StateFlow backed by [mutable].
     * Updates to [mutable] must be visible through the returned flow.
     * The returned type must not be `MutableStateFlow`.
     *
     * Requirement: use `asStateFlow`.
     */
    fun <T> readOnly(mutable: MutableStateFlow<T>): StateFlow<T> {
        return mutable.asStateFlow()
    }

    /**
     * Exercise 3 — update to Loading
     *
     * Atomically mark [state] as loading: `loading = true`, `error = null`,
     * keep the current users list so cached data stays on screen.
     *
     * Requirement: use `update`. Do not read `.value` and then assign.
     */
    fun markLoading(state: MutableStateFlow<UiState>) {
        TODO()
    }

    /**
     * Exercise 4 — update to Success
     *
     * Atomically set `loading = false`, `users = [users]`, `error = null`.
     *
     * Requirement: use `update`.
     */
    fun markSuccess(state: MutableStateFlow<UiState>, users: List<User>) {
        TODO()
    }

    /**
     * Exercise 5 — update to Error
     *
     * Atomically set `loading = false`, `error = [message]`, keep the
     * current users list so a failure does not wipe the cache.
     *
     * Requirement: use `update`.
     */
    fun markError(state: MutableStateFlow<UiState>, message: String) {
        TODO()
    }

    /**
     * Exercise 6 — Initial → Loading → Success/Error
     *
     * Drive [state] through a refresh:
     * 1. Mark loading (keep users, clear error).
     * 2. Call [FakeUserApi.loadUsers].
     * 3. On success, mark success with the loaded users.
     * 4. On failure, mark error with the exception message, keep users.
     *
     * Do not catch `CancellationException`. Cancellation is not a load error.
     *
     * Requirement: use [markLoading], [markSuccess], and [markError].
     */
    suspend fun refreshUsers(state: MutableStateFlow<UiState>, api: FakeUserApi) {
        TODO()
    }

    /**
     * Exercise 7 — stateIn Eagerly
     *
     * Convert cold [upstream] into a hot StateFlow that starts immediately,
     * even with no collectors. Until [upstream] emits, the current value is
     * [initial]. After [upstream] completes, keep the last value.
     *
     * Requirement: use `stateIn` with `SharingStarted.Eagerly`.
     * Do not collect into a `MutableStateFlow` yourself.
     */
    fun <T> shareEagerly(
        upstream: Flow<T>,
        scope: CoroutineScope,
        initial: T
    ): StateFlow<T> {
        TODO()
    }

    /**
     * Exercise 8 — stateIn Lazily
     *
     * Convert cold [upstream] into a hot StateFlow that starts on the first
     * collector. Later collectors must share that producer and receive the
     * current value, not a replay of every past emission.
     *
     * Requirement: use `stateIn` with `SharingStarted.Lazily`.
     * Do not use `SharingStarted.Eagerly`.
     */
    fun <T> shareLazily(
        upstream: Flow<T>,
        scope: CoroutineScope,
        initial: T
    ): StateFlow<T> {
        TODO()
    }
}

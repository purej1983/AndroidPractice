package practice.week4

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Day 19 — Offline-first architecture.
 *
 * Key ideas:
 * - The database is the **source of truth**. The UI never renders a
 *   network response that skipped the database.
 * - Cached rows are available immediately, with **zero** network calls.
 * - API success writes the database. Observers of the database emit.
 * - API failure does not clear the table. The user still has a list.
 * - Successful `replaceAll` drops stale rows that the server no longer
 *   returns. Merge-only caches lie about deletes.
 * - Refresh status is **separate** from the list. A failed refresh is
 *   not the same as "we have no users".
 *
 * Architecture:
 *
 * ```
 * API → Repository → Database → Flow → UI
 * ```
 */
sealed interface RefreshStatus {
    data object Idle : RefreshStatus
    data object Refreshing : RefreshStatus
    data class Failed(val message: String) : RefreshStatus
}

/**
 * Exercise 1 — offline feed
 *
 * [users] is always the local table. Seeded cache must appear without
 * calling the API.
 *
 * [refreshStatus] is Idle / Refreshing / Failed. A failed refresh must
 * leave [users] unchanged.
 *
 * A new refresh cancels the previous one. Cancellation is not a failure.
 *
 * Requirement: collect the repository, do not copy remote lists into
 * [users] yourself. Expose both flows as `StateFlow`, not mutable.
 */
class OfflineFeedController(
    private val repository: UserRepository,
    private val scope: CoroutineScope
) {
    val users: StateFlow<List<CachedUser>> = TODO()

    val refreshStatus: StateFlow<RefreshStatus> = TODO()

    fun refresh() {
        TODO()
    }
}

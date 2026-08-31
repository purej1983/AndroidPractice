package practice.week4

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Day 18 — Race conditions and search.
 *
 * Problem: a slow `cat` request starts, a faster `cats` request finishes
 * first, then the old `cat` response arrives and overwrites the UI.
 *
 * Key ideas:
 * - Do not solve this with a "request id" flag if Flow can cancel the
 *   stale work. `flatMapLatest` is the latest-wins operator.
 * - Debounce so fast typing does not start a request per keystroke.
 * - `distinctUntilChanged` so a settled duplicate does not search again.
 * - A blank query must **cancel** in-flight search and must not call the API.
 * - Cancellation is not a search error.
 *
 * Pipeline: debounce → distinctUntilChanged → flatMapLatest(search).
 */
data class CatalogHit(
    val id: String,
    val name: String
)

data class CatalogSearchResult(
    val query: String,
    val items: List<CatalogHit>
)

data class SearchUiState(
    val loading: Boolean = false,
    val query: String = "",
    val results: List<CatalogHit> = emptyList(),
    val error: String? = null
)

class CatalogSearchException(val query: String) : IllegalStateException("Search failed: $query")

/**
 * In-memory catalog. [delayByQuery] is how tests make `cat` slower than
 * `cats` so a naive "wait for every request" implementation loses.
 */
class StaleSearchApi(
    private val itemsByQuery: Map<String, List<CatalogHit>> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val delayByQuery: Map<String, Long> = emptyMap(),
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _startedQueries = mutableListOf<String>()
    private val _completedQueries = mutableListOf<String>()
    private val _cancelledQueries = mutableListOf<String>()

    val startedQueries: List<String> get() = _startedQueries.toList()
    val completedQueries: List<String> get() = _completedQueries.toList()
    val cancelledQueries: List<String> get() = _cancelledQueries.toList()

    suspend fun search(query: String): CatalogSearchResult {
        _startedQueries += query
        try {
            delay(delayByQuery[query] ?: delayMillis)
            failures[query]?.let { throw it }
            _completedQueries += query
            return CatalogSearchResult(query, itemsByQuery[query].orEmpty())
        } catch (cancelled: CancellationException) {
            _cancelledQueries += query
            throw cancelled
        }
    }
}

/**
 * Exercise 1 — latest query wins
 *
 * [onQueryChanged] updates the visible query immediately.
 * After [debounceMillis] of quiet, skip an unchanged query, then search.
 * A newer settled query cancels the in-flight search.
 * Blank query cancels in-flight search and does not call the API.
 * Keep previous [SearchUiState.results] on error. Do not treat cancel
 * as an error.
 *
 * Requirement: use `debounce`, `distinctUntilChanged`, and `flatMapLatest`.
 * Do not keep a "latest request id" integer. Do not use `flatMapConcat`.
 * Expose [state] as `StateFlow`, not `MutableStateFlow`.
 */
class SearchController(
    private val api: StaleSearchApi,
    scope: CoroutineScope,
    debounceMillis: Long
) {
    val state: StateFlow<SearchUiState> = TODO()

    fun onQueryChanged(query: String) {
        TODO()
    }
}

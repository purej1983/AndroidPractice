package practice.week3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.zip
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Day 12 — Flow operators.
 *
 * Key ideas:
 * - `debounce` emits a value only after the upstream stays quiet for a timeout.
 *   Rapid keystrokes collapse to the last query.
 * - `distinctUntilChanged` drops consecutive duplicates. "iphone" then "iphone"
 *   again does not start a second search.
 * - `combine` waits until every input has emitted once, then emits whenever
 *   **any** input emits, using the latest value from each.
 * - `zip` pairs emissions **1-to-1**. It does not skip ahead to the latest
 *   unpaired value.
 * - `flatMapLatest` cancels the previous inner Flow when a new value arrives.
 *   That is how "latest search wins".
 * - `flatMapConcat` waits for the previous inner Flow to finish before
 *   starting the next one, and before collecting the next upstream value.
 *   Older work is not cancelled.
 * - `collectLatest` is the collector-side version: a new emission cancels
 *   the previous collect block.
 *
 * Typical search-while-typing pipeline:
 * debounce → distinctUntilChanged → skip blanks → flatMapLatest(search).
 *
 * `debounce` is marked `@FlowPreview`. `flatMapLatest` and `flatMapConcat`
 * are marked `@ExperimentalCoroutinesApi`. This object already opts in.
 */
data class CatalogItem(
    val id: String,
    val name: String
)

data class SearchResult(
    val query: String,
    val items: List<CatalogItem>
)

data class SearchForm(
    val query: String,
    val category: String
)

data class PagedQuery(
    val query: String,
    val page: Int
)

class SearchFailedException(val query: String) : IllegalStateException("Search failed: $query")

/**
 * In-memory catalog search for tests.
 *
 * [search] records start / completion / cancellation so tests can prove
 * `flatMapLatest` cancels in-flight work and `flatMapConcat` does not.
 */
class FakeSearchApi(
    private val itemsByQuery: Map<String, List<CatalogItem>> = emptyMap(),
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

    suspend fun search(query: String): SearchResult {
        _startedQueries += query
        try {
            delay(delayByQuery[query] ?: delayMillis)
            failures[query]?.let { throw it }
            _completedQueries += query
            return SearchResult(query, itemsByQuery[query].orEmpty())
        } catch (cancelled: CancellationException) {
            _cancelledQueries += query
            throw cancelled
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
object FlowOperatorsPractice {

    /**
     * Exercise 1 — debounce
     *
     * Emit values from [queries] only after [timeoutMillis] with no newer
     * value. Rapid typing should collapse to the last query.
     *
     * Requirement: use `debounce`. Do not implement a timer yourself.
     */
    fun debounceQueries(queries: Flow<String>, timeoutMillis: Long): Flow<String> {
        return queries.debounce(timeoutMillis.milliseconds)
    }

    /**
     * Exercise 2 — distinctUntilChanged
     *
     * Emit values from [queries], dropping consecutive duplicates.
     * A later repeat that is not consecutive must still emit.
     *
     * Requirement: use `distinctUntilChanged`.
     */
    fun distinctQueries(queries: Flow<String>): Flow<String> {
        return queries.distinctUntilChanged()
    }

    /**
     * Exercise 3 — combine
     *
     * Combine [queries] and [categories] into [SearchForm].
     * Emit whenever either Flow emits, using the latest value from both.
     * Do not emit until both have produced at least one value.
     *
     * Requirement: use `combine`.
     */
    fun combineQueryAndCategory(
        queries: Flow<String>,
        categories: Flow<String>
    ): Flow<SearchForm> {
        return combine(queries, categories) { query, categories -> SearchForm(query, categories) }
    }

    /**
     * Exercise 4 — zip
     *
     * Pair each query with the next page number, in order.
     * Extra values on either side wait for a partner. zip does not skip
     * ahead to the latest unpaired query.
     *
     * Requirement: use `zip`.
     */
    fun zipQueryAndPage(
        queries: Flow<String>,
        pages: Flow<Int>
    ): Flow<PagedQuery> {
        return queries.zip(pages) { query, page -> PagedQuery(query, page) }
    }

    /**
     * Exercise 5 — flatMapLatest
     *
     * For each query, search [api] and emit the result.
     * A new query must cancel the previous in-flight search.
     *
     * Requirement: use `flatMapLatest` and [FakeSearchApi.search].
     * Do not use `flatMapConcat` or `flatMapMerge`.
     */
    fun searchLatest(queries: Flow<String>, api: FakeSearchApi): Flow<SearchResult> {
        return queries.flatMapLatest {  query ->
            flow{
                emit(api.search(query))
            }
        }
    }

    /**
     * Exercise 6 — flatMapConcat
     *
     * For each query, search [api] and emit the result.
     * A new query must wait for the previous search to finish. Do not
     * cancel in-flight work. `flatMapConcat` also does not collect the next
     * upstream value until the current search completes.
     *
     * Requirement: use `flatMapConcat` and [FakeSearchApi.search].
     * Do not use `flatMapLatest`.
     */
    fun searchAllInOrder(queries: Flow<String>, api: FakeSearchApi): Flow<SearchResult> {
        TODO()
    }

    /**
     * Exercise 7 — collectLatest
     *
     * Collect [results]. For each result, delay [delayMillis], then append
     * it to [destination]. If a new result arrives during the delay, cancel
     * that processing so the previous result is not appended.
     *
     * Requirement: use `collectLatest` and `delay`. Do not use `Thread.sleep`.
     */
    suspend fun collectLatestInto(
        results: Flow<SearchResult>,
        delayMillis: Long,
        destination: MutableList<SearchResult>
    ) {
        TODO()
    }

    /**
     * Exercise 8 — search while typing
     *
     * Turn a stream of keystrokes into search results:
     * 1. Wait until typing pauses (`debounce`).
     * 2. Skip a settled query that did not change (`distinctUntilChanged`).
     * 3. Skip blank queries. Do not call the API for them, and do not emit.
     * 4. Search [api], cancelling the previous search when a new settled
     *    query arrives (`flatMapLatest`).
     *
     * Operator order matters: debounce, then distinctUntilChanged, then
     * skip blanks, then search.
     *
     * Requirement: use `debounce`, `distinctUntilChanged`, and `flatMapLatest`.
     */
    fun searchWhileTyping(
        queries: Flow<String>,
        api: FakeSearchApi,
        debounceMillis: Long
    ): Flow<SearchResult> {
        TODO()
    }
}

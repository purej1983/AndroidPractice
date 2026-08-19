package practice.week2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * Day 8 — Cancellation.
 *
 * Key ideas:
 * - Cancellation is cooperative. Work stops at cancellation points.
 * - `delay`, `yield`, and `ensureActive` are cancellation points.
 * - A tight loop without those keeps running after `cancel()`.
 * - `CancellationException` must propagate. Do not swallow it.
 * - Use `withContext(NonCancellable)` for cleanup that must still run.
 */
data class SearchDocument(
    val id: String,
    val title: String,
    val body: String
)

class SearchFailedException(val query: String) : IllegalStateException("Search failed: $query")

/**
 * Records which document ids were examined. Tests use this to prove
 * cancellation stopped remaining work.
 */
class ScanTracker {
    private val _scannedIds = mutableListOf<String>()
    val scannedIds: List<String> get() = _scannedIds.toList()

    fun record(id: String) {
        _scannedIds += id
    }
}

class FakeSearchApi(
    private val documents: List<SearchDocument> = emptyList(),
    private val delayMillis: Long = 0L,
    private val failures: Map<String, Throwable> = emptyMap()
) {
    private val _queries = mutableListOf<String>()
    val queries: List<String> get() = _queries.toList()

    suspend fun search(query: String): List<SearchDocument> {
        _queries += query
        delay(delayMillis)
        failures[query]?.let { throw it }
        val needle = query.lowercase()
        return documents.filter { document ->
            needle in document.title.lowercase() || needle in document.body.lowercase()
        }
    }
}

class FakeNotifier(
    private val delayMillis: Long = 0L
) {
    private val _messages = mutableListOf<String>()
    val messages: List<String> get() = _messages.toList()

    suspend fun notify(message: String) {
        delay(delayMillis)
        _messages += message
    }
}

object CancellationPractice {

    /**
     * Exercise 1 — delay is a cancellation point
     *
     * Suspend for [delayMillis], then return [value].
     *
     * Requirement: use `delay`. Do not use `Thread.sleep`.
     */
    suspend fun delayedValue(value: String, delayMillis: Long): String {
        delay(delayMillis)
        return value
    }

    /**
     * Exercise 2 — cancel stops remaining work
     *
     * Scan every document in order. For each document, delay
     * [delayPerDocumentMillis], then record its id on [tracker].
     * Return every document id in original order.
     *
     * Requirement: delay before recording each document, so cancel can
     * stop remaining documents.
     */
    suspend fun scanDocuments(
        documents: List<SearchDocument>,
        tracker: ScanTracker,
        delayPerDocumentMillis: Long
    ): List<String> {
        return documents.map { document ->
            delay(delayPerDocumentMillis)
            tracker.record(document.id)
            document.id
        }
    }

    /**
     * Exercise 3 — cancellable search
     *
     * Search [documents] for [query].
     * A document matches when [query] is contained in title or body,
     * ignoring case.
     *
     * For each document, delay [delayPerDocumentMillis], record its id
     * on [tracker], then check for a match.
     * Return matching documents in original order.
     *
     * Requirement: delay before each document so cancel stops remaining work.
     */
    suspend fun searchDocuments(
        documents: List<SearchDocument>,
        query: String,
        tracker: ScanTracker,
        delayPerDocumentMillis: Long
    ): List<SearchDocument> {
        return documents.mapNotNull { document ->
            delay(delayPerDocumentMillis)
            tracker.record(document.id)
            val needle = query.lowercase()
            if(needle in document.title.lowercase() || needle in document.body.lowercase()) {
                document
            } else {
                null
            }
        }
    }

    /**
     * Exercise 4 — ensureActive
     *
     * Call [onStep] for every n in `1..count`, in order.
     *
     * Requirement: use `ensureActive()` so that `cancel()` from [onStep]
     * stops remaining steps. Do not add `delay` here.
     */
    suspend fun runSteps(count: Int, onStep: (Int) -> Unit) {
        for (n in 1..count) {
            coroutineContext.ensureActive()
            onStep(n)
        }
    }

    /**
     * Exercise 5 — isActive and partial results
     *
     * Delay [tickMillis] repeatedly. Return how many delays completed.
     *
     * Requirement: when cancelled, return the count. Do not throw
     * [CancellationException] to the caller.
     */
    suspend fun ticksCompleted(tickMillis: Long): Int {
        var count = 0
        try {
            while (true) {
                delay(tickMillis)
                count++
            }
        } catch (e: CancellationException) {
            return count
        }
    }

    /**
     * Exercise 6 — do not swallow cancellation
     *
     * Search [query] on [api].
     * If search throws [SearchFailedException], return an empty list.
     *
     * Requirement: do not catch [CancellationException]. A cancelled
     * coroutine must not return an empty list.
     */
    suspend fun searchOrEmpty(api: FakeSearchApi, query: String): List<SearchDocument> {
        try {
            return api.search(query)
        } catch (e: SearchFailedException) {
            return listOf()
        }
        
    }

    /**
     * Exercise 7 — timeout
     *
     * Search [query] on [api].
     * Return the results if they arrive within [timeoutMillis].
     * Return null if the search takes too long.
     *
     * Requirement: use `withTimeoutOrNull`. Do not use `withTimeout`.
     */
    suspend fun searchWithTimeout(
        api: FakeSearchApi,
        query: String,
        timeoutMillis: Long
    ): List<SearchDocument>? {
        return withTimeoutOrNull(timeoutMillis) {
            api.search(query)
        }
    }

    /**
     * Exercise 8 — NonCancellable cleanup
     *
     * Search [query] on [api], then notify `"done:{query}"`.
     * Return the search results.
     *
     * If the search is cancelled, still notify `"done:{query}"`, then let
     * cancellation propagate. Do not return a result.
     *
     * Requirement: use `withContext(NonCancellable)` for the notify.
     */
    suspend fun searchThenNotify(
        api: FakeSearchApi,
        query: String,
        notifier: FakeNotifier
    ): List<SearchDocument> {
        try {
            return api.search(query)
        } finally {
            withContext(NonCancellable) {
                notifier.notify("done:$query")
            }
        }
    }

    /**
     * Exercise 9 — cancel a returned Job
     *
     * Start a search on [scope] and return the [Job].
     * When the search finishes, call [onResult] with the matches.
     * Scanning uses the same rules as [searchDocuments].
     *
     * Requirement: use `scope.launch`. The caller cancels the returned
     * Job to stop remaining work.
     */
    fun searchInBackground(
        scope: CoroutineScope,
        documents: List<SearchDocument>,
        query: String,
        tracker: ScanTracker,
        delayPerDocumentMillis: Long,
        onResult: (List<SearchDocument>) -> Unit
    ): Job {
        return scope.launch { 
            val result = searchDocuments(documents, query, tracker, delayPerDocumentMillis)  
            onResult(result)
        }
    }
}

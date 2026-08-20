package practice.week2

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CancellationPracticeTest {

    private val kotlinDoc = SearchDocument(
        id = "d1",
        title = "Kotlin Coroutines",
        body = "cancellation is cooperative"
    )
    private val javaDoc = SearchDocument(
        id = "d2",
        title = "Java Threads",
        body = "interrupt a thread"
    )
    private val rustDoc = SearchDocument(
        id = "d3",
        title = "Rust Async",
        body = "cancel a future"
    )
    private val documents = listOf(kotlinDoc, javaDoc, rustDoc)

    @Test
    fun `delayedValue returns the value`() = runTest {
        assertEquals(
            "done",
            CancellationPractice.delayedValue("done", delayMillis = 0)
        )
    }

    @Test
    fun `delayedValue suspends for the given duration`() = runTest {
        val result = CancellationPractice.delayedValue("ok", delayMillis = 1_000)

        assertEquals("ok", result)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `delayedValue does not return after cancel during delay`() = runTest {
        var result: String? = null

        val job = launch {
            result = CancellationPractice.delayedValue("done", delayMillis = 5_000)
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertNull(result)
        assertEquals(0, currentTime)
    }

    @Test
    fun `scanDocuments records and returns every id in order`() = runTest {
        val tracker = ScanTracker()

        assertEquals(
            listOf("d1", "d2", "d3"),
            CancellationPractice.scanDocuments(documents, tracker, delayPerDocumentMillis = 0)
        )
        assertEquals(listOf("d1", "d2", "d3"), tracker.scannedIds)
    }

    @Test
    fun `scanDocuments returns empty for an empty list`() = runTest {
        val tracker = ScanTracker()

        assertEquals(
            emptyList(),
            CancellationPractice.scanDocuments(emptyList(), tracker, delayPerDocumentMillis = 1_000)
        )
        assertEquals(emptyList(), tracker.scannedIds)
        assertEquals(0, currentTime)
    }

    @Test
    fun `scanDocuments delays once per document so times add up`() = runTest {
        val tracker = ScanTracker()

        val ids = CancellationPractice.scanDocuments(
            documents,
            tracker,
            delayPerDocumentMillis = 1_000
        )

        assertEquals(listOf("d1", "d2", "d3"), ids)
        assertEquals(3_000, currentTime)
    }

    @Test
    fun `scanDocuments does not scan remaining documents after cancel`() = runTest {
        val tracker = ScanTracker()
        var result: List<String>? = null

        val job = launch {
            result = CancellationPractice.scanDocuments(
                documents,
                tracker,
                delayPerDocumentMillis = 1_000
            )
        }
        testScheduler.advanceTimeBy(1_500)
        job.cancelAndJoin()

        assertEquals(listOf("d1"), tracker.scannedIds)
        assertNull(result)
        assertEquals(1_500, currentTime)
    }

    @Test
    fun `searchDocuments returns case-insensitive matches in original order`() = runTest {
        val tracker = ScanTracker()

        assertEquals(
            listOf(kotlinDoc, rustDoc),
            CancellationPractice.searchDocuments(
                documents,
                query = "cancel",
                tracker = tracker,
                delayPerDocumentMillis = 0
            )
        )
        assertEquals(listOf("d1", "d2", "d3"), tracker.scannedIds)
    }

    @Test
    fun `searchDocuments returns empty when nothing matches`() = runTest {
        val tracker = ScanTracker()

        assertEquals(
            emptyList(),
            CancellationPractice.searchDocuments(
                documents,
                query = "python",
                tracker = tracker,
                delayPerDocumentMillis = 0
            )
        )
        assertEquals(listOf("d1", "d2", "d3"), tracker.scannedIds)
    }

    @Test
    fun `searchDocuments matches title or body`() = runTest {
        val tracker = ScanTracker()

        assertEquals(
            listOf(javaDoc),
            CancellationPractice.searchDocuments(
                documents,
                query = "interrupt",
                tracker = tracker,
                delayPerDocumentMillis = 0
            )
        )
    }

    @Test
    fun `searchDocuments delays once per document`() = runTest {
        val tracker = ScanTracker()

        val matches = CancellationPractice.searchDocuments(
            documents,
            query = "kotlin",
            tracker = tracker,
            delayPerDocumentMillis = 1_000
        )

        assertEquals(listOf(kotlinDoc), matches)
        assertEquals(3_000, currentTime)
    }

    @Test
    fun `searchDocuments does not scan remaining documents after cancel`() = runTest {
        val tracker = ScanTracker()
        var result: List<SearchDocument>? = null

        val job = launch {
            result = CancellationPractice.searchDocuments(
                documents,
                query = "a",
                tracker = tracker,
                delayPerDocumentMillis = 1_000
            )
        }
        testScheduler.advanceTimeBy(1_500)
        job.cancelAndJoin()

        assertEquals(listOf("d1"), tracker.scannedIds)
        assertNull(result)
        assertEquals(1_500, currentTime)
    }

    @Test
    fun `runSteps calls onStep for every value from 1 to count`() = runTest {
        val steps = mutableListOf<Int>()

        CancellationPractice.runSteps(4) { steps += it }

        assertEquals(listOf(1, 2, 3, 4), steps)
    }

    @Test
    fun `runSteps does nothing when count is zero`() = runTest {
        val steps = mutableListOf<Int>()

        CancellationPractice.runSteps(0) { steps += it }

        assertEquals(emptyList(), steps)
    }

    @Test
    fun `runSteps stops remaining steps when cancelled from onStep`() = runTest {
        val steps = mutableListOf<Int>()
        lateinit var job: Job

        job = launch {
            CancellationPractice.runSteps(20) { n ->
                steps += n
                if (n == 3) job.cancel()
            }
        }
        job.join()

        assertEquals(listOf(1, 2, 3), steps)
    }

    @Test
    fun `ticksCompleted returns zero when cancelled before the first tick`() = runTest {
        var ticks = -1

        val job = launch {
            ticks = CancellationPractice.ticksCompleted(tickMillis = 1_000)
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(0, ticks)
    }

    @Test
    fun `ticksCompleted returns how many delays finished before cancel`() = runTest {
        var ticks = -1

        val job = launch {
            ticks = CancellationPractice.ticksCompleted(tickMillis = 1_000)
        }
        testScheduler.advanceTimeBy(2_500)
        job.cancelAndJoin()

        assertEquals(2, ticks)
        assertEquals(2_500, currentTime)
    }

    @Test
    fun `searchOrEmpty returns matches`() = runTest {
        val api = FakeSearchApi(documents = documents)

        assertEquals(
            listOf(kotlinDoc),
            CancellationPractice.searchOrEmpty(api, "Kotlin")
        )
        assertEquals(listOf("Kotlin"), api.queries)
    }

    @Test
    fun `searchOrEmpty returns empty when search fails`() = runTest {
        val api = FakeSearchApi(
            failures = mapOf("kotlin" to SearchFailedException("kotlin"))
        )

        assertEquals(
            emptyList(),
            CancellationPractice.searchOrEmpty(api, "kotlin")
        )
        assertEquals(listOf("kotlin"), api.queries)
    }

    @Test
    fun `searchOrEmpty does not return empty when cancelled`() = runTest {
        val api = FakeSearchApi(documents = documents, delayMillis = 5_000)
        var result: List<SearchDocument>? = null

        val job = launch {
            result = CancellationPractice.searchOrEmpty(api, "Kotlin")
        }
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(listOf("Kotlin"), api.queries)
        assertNull(result)
    }

    @Test
    fun `searchWithTimeout returns results when the search finishes in time`() = runTest {
        val api = FakeSearchApi(documents = documents, delayMillis = 400)

        val result = CancellationPractice.searchWithTimeout(
            api,
            query = "Kotlin",
            timeoutMillis = 1_000
        )

        assertEquals(listOf(kotlinDoc), result)
        assertEquals(400, currentTime)
    }

    @Test
    fun `searchWithTimeout returns null when the search is too slow`() = runTest {
        val api = FakeSearchApi(documents = documents, delayMillis = 2_000)

        val result = CancellationPractice.searchWithTimeout(
            api,
            query = "Kotlin",
            timeoutMillis = 1_000
        )

        assertNull(result)
        assertEquals(1_000, currentTime)
        assertEquals(listOf("Kotlin"), api.queries)
    }

    @Test
    fun `searchThenNotify returns results and notifies`() = runTest {
        val api = FakeSearchApi(documents = documents)
        val notifier = FakeNotifier()

        assertEquals(
            listOf(kotlinDoc),
            CancellationPractice.searchThenNotify(api, "Kotlin", notifier)
        )
        assertEquals(listOf("done:Kotlin"), notifier.messages)
    }

    @Test
    fun `searchThenNotify still notifies after cancel`() = runTest {
        val api = FakeSearchApi(documents = documents, delayMillis = 1_000)
        val notifier = FakeNotifier()
        var result: List<SearchDocument>? = null

        val job = launch {
            result = CancellationPractice.searchThenNotify(api, "Kotlin", notifier)
        }
        testScheduler.advanceTimeBy(500)
        job.cancelAndJoin()

        assertEquals(listOf("Kotlin"), api.queries)
        assertEquals(listOf("done:Kotlin"), notifier.messages)
        assertNull(result)
    }

    @Test
    fun `searchInBackground delivers results when joined`() = runTest {
        val tracker = ScanTracker()
        var result: List<SearchDocument>? = null

        val job = CancellationPractice.searchInBackground(
            scope = this,
            documents = documents,
            query = "Kotlin",
            tracker = tracker,
            delayPerDocumentMillis = 0
        ) { result = it }
        job.join()

        assertEquals(listOf(kotlinDoc), result)
        assertEquals(listOf("d1", "d2", "d3"), tracker.scannedIds)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `searchInBackground cancel stops remaining work and skips onResult`() = runTest {
        val tracker = ScanTracker()
        var result: List<SearchDocument>? = null

        val job = CancellationPractice.searchInBackground(
            scope = this,
            documents = documents,
            query = "a",
            tracker = tracker,
            delayPerDocumentMillis = 1_000
        ) { result = it }
        testScheduler.advanceTimeBy(1_500)
        job.cancelAndJoin()

        assertEquals(listOf("d1"), tracker.scannedIds)
        assertNull(result)
        assertEquals(1_500, currentTime)
    }
}

package practice.week3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlowOperatorsPracticeTest {

    private val iphone = CatalogItem("p1", "iPhone 16")
    private val iphoneCase = CatalogItem("p2", "iPhone Case")
    private val ipad = CatalogItem("p3", "iPad")
    private val catToy = CatalogItem("c1", "Cat Toy")
    private val catsPoster = CatalogItem("c2", "Cats Poster")

    private val iphoneResult = SearchResult("iphone", listOf(iphone, iphoneCase))
    private val ipadResult = SearchResult("ipad", listOf(ipad))
    private val catResult = SearchResult("cat", listOf(catToy))
    private val catsResult = SearchResult("cats", listOf(catsPoster))

    @Test
    fun `debounceQueries emits only the last rapid keystroke`() = runTest {
        val queries = timedFlow(
            0L to "i",
            50L to "ip",
            100L to "iph",
            150L to "ipho",
            200L to "iphone"
        )

        assertEquals(
            listOf("iphone"),
            FlowOperatorsPractice.debounceQueries(queries, timeoutMillis = 300).toList()
        )
    }

    @Test
    fun `debounceQueries emits nothing for an empty flow`() = runTest {
        assertEquals(
            emptyList(),
            FlowOperatorsPractice.debounceQueries(flow<String> { }, timeoutMillis = 300).toList()
        )
        assertEquals(0, currentTime)
    }

    @Test
    fun `debounceQueries waits for the timeout while upstream stays open`() = runTest {
        val emitted = mutableListOf<String>()

        val job = launch {
            FlowOperatorsPractice
                .debounceQueries(
                    timedFlow(0L to "iphone", lingerMillis = 10_000),
                    timeoutMillis = 300
                )
                .collect { emitted += it }
        }
        testScheduler.advanceTimeBy(299)
        testScheduler.runCurrent()
        assertEquals(emptyList(), emitted)

        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertEquals(listOf("iphone"), emitted)
        assertEquals(300, currentTime)

        job.cancelAndJoin()
    }

    @Test
    fun `debounceQueries cancel during the timeout emits nothing`() = runTest {
        val emitted = mutableListOf<String>()

        val job = launch {
            FlowOperatorsPractice
                .debounceQueries(
                    timedFlow(0L to "iphone", lingerMillis = 10_000),
                    timeoutMillis = 300
                )
                .collect { emitted += it }
        }
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(emptyList(), emitted)
        assertEquals(100, currentTime)
    }

    @Test
    fun `debounceQueries emits each value spaced farther than the timeout`() = runTest {
        val queries = timedFlow(
            0L to "i",
            400L to "ip",
            800L to "iphone",
            lingerMillis = 400
        )

        assertEquals(
            listOf("i", "ip", "iphone"),
            FlowOperatorsPractice.debounceQueries(queries, timeoutMillis = 300).toList()
        )
        assertEquals(1_200, currentTime)
    }

    @Test
    fun `distinctQueries drops consecutive duplicates`() = runTest {
        val queries = flow {
            emit("ip")
            emit("ip")
            emit("iphone")
            emit("iphone")
        }

        assertEquals(
            listOf("ip", "iphone"),
            FlowOperatorsPractice.distinctQueries(queries).toList()
        )
    }

    @Test
    fun `distinctQueries keeps a non-consecutive repeat`() = runTest {
        val queries = flow {
            emit("iphone")
            emit("ipad")
            emit("iphone")
        }

        assertEquals(
            listOf("iphone", "ipad", "iphone"),
            FlowOperatorsPractice.distinctQueries(queries).toList()
        )
    }

    @Test
    fun `distinctQueries emits once when every value is the same`() = runTest {
        val queries = flow {
            emit("iphone")
            emit("iphone")
            emit("iphone")
        }

        assertEquals(
            listOf("iphone"),
            FlowOperatorsPractice.distinctQueries(queries).toList()
        )
    }

    @Test
    fun `distinctQueries emits nothing for an empty flow`() = runTest {
        assertEquals(
            emptyList(),
            FlowOperatorsPractice.distinctQueries(flow<String> { }).toList()
        )
    }

    @Test
    fun `combineQueryAndCategory waits until both have a value`() = runTest {
        val emitted = mutableListOf<SearchForm>()

        val job = launch {
            FlowOperatorsPractice
                .combineQueryAndCategory(
                    timedFlow(1_000L to "iphone"),
                    timedFlow(400L to "phones")
                )
                .collect { emitted += it }
        }
        testScheduler.advanceTimeBy(400)
        testScheduler.runCurrent()
        assertEquals(emptyList(), emitted)

        testScheduler.advanceTimeBy(600)
        testScheduler.runCurrent()
        assertEquals(listOf(SearchForm("iphone", "phones")), emitted)
        assertEquals(1_000, currentTime)

        job.cancelAndJoin()
    }

    @Test
    fun `combineQueryAndCategory emits query then category`() = runTest {
        assertEquals(
            listOf(SearchForm("iphone", "phones")),
            FlowOperatorsPractice.combineQueryAndCategory(
                timedFlow(0L to "iphone"),
                timedFlow(200L to "phones")
            ).toList()
        )
        assertEquals(200, currentTime)
    }

    @Test
    fun `combineQueryAndCategory re-emits when the query changes`() = runTest {
        assertEquals(
            listOf(
                SearchForm("i", "phones"),
                SearchForm("iphone", "phones")
            ),
            FlowOperatorsPractice.combineQueryAndCategory(
                timedFlow(0L to "i", 500L to "iphone"),
                timedFlow(100L to "phones")
            ).toList()
        )
    }

    @Test
    fun `combineQueryAndCategory re-emits when the category changes`() = runTest {
        assertEquals(
            listOf(
                SearchForm("iphone", "all"),
                SearchForm("iphone", "phones")
            ),
            FlowOperatorsPractice.combineQueryAndCategory(
                timedFlow(0L to "iphone"),
                timedFlow(100L to "all", 500L to "phones")
            ).toList()
        )
    }

    @Test
    fun `combineQueryAndCategory emits nothing when one side is empty`() = runTest {
        assertEquals(
            emptyList(),
            FlowOperatorsPractice.combineQueryAndCategory(
                flow { emit("iphone") },
                flow<String> { }
            ).toList()
        )
        assertEquals(
            emptyList(),
            FlowOperatorsPractice.combineQueryAndCategory(
                flow<String> { },
                flow { emit("phones") }
            ).toList()
        )
    }

    @Test
    fun `zipQueryAndPage pairs queries and pages in order`() = runTest {
        assertEquals(
            listOf(
                PagedQuery("iphone", 1),
                PagedQuery("ipad", 2)
            ),
            FlowOperatorsPractice.zipQueryAndPage(
                flow {
                    emit("iphone")
                    emit("ipad")
                },
                flow {
                    emit(1)
                    emit(2)
                }
            ).toList()
        )
    }

    @Test
    fun `zipQueryAndPage waits for a page and keeps the first unpaired query`() = runTest {
        assertEquals(
            listOf(PagedQuery("cat", 1)),
            FlowOperatorsPractice.zipQueryAndPage(
                timedFlow(0L to "cat", 50L to "cats"),
                timedFlow(200L to 1)
            ).toList()
        )
        assertEquals(200, currentTime)
    }

    @Test
    fun `zipQueryAndPage waits for a query when a page arrives first`() = runTest {
        assertEquals(
            listOf(PagedQuery("iphone", 1)),
            FlowOperatorsPractice.zipQueryAndPage(
                timedFlow(500L to "iphone"),
                timedFlow(0L to 1, 50L to 2)
            ).toList()
        )
        assertEquals(500, currentTime)
    }

    @Test
    fun `zipQueryAndPage does not skip ahead to the latest query`() = runTest {
        val zipped = FlowOperatorsPractice.zipQueryAndPage(
            timedFlow(0L to "cat", 100L to "cats"),
            timedFlow(300L to 1)
        ).toList()
        val combined = FlowOperatorsPractice.combineQueryAndCategory(
            timedFlow(0L to "cat", 100L to "cats"),
            timedFlow(300L to "all")
        ).toList()

        assertEquals(listOf(PagedQuery("cat", 1)), zipped)
        assertEquals(listOf(SearchForm("cats", "all")), combined)
    }

    @Test
    fun `searchLatest completes sequential searches`() = runTest {
        val api = catalogApi(delayMillis = 100)
        val results = FlowOperatorsPractice
            .searchLatest(timedFlow(0L to "ipad", 200L to "iphone"), api)
            .toList()

        assertEquals(listOf(ipadResult, iphoneResult), results)
        assertEquals(listOf("ipad", "iphone"), api.startedQueries)
        assertEquals(listOf("ipad", "iphone"), api.completedQueries)
        assertEquals(emptyList(), api.cancelledQueries)
        assertEquals(300, currentTime)
    }

    @Test
    fun `searchLatest cancels the previous in-flight search`() = runTest {
        val api = catalogApi(
            delayByQuery = mapOf("cat" to 1_000L, "cats" to 100L)
        )
        val results = FlowOperatorsPractice
            .searchLatest(timedFlow(0L to "cat", 50L to "cats"), api)
            .toList()

        assertEquals(listOf(catsResult), results)
        assertEquals(listOf("cat", "cats"), api.startedQueries)
        assertEquals(listOf("cats"), api.completedQueries)
        assertEquals(listOf("cat"), api.cancelledQueries)
        assertEquals(150, currentTime)
    }

    @Test
    fun `searchLatest does not let a cancelled failure fail collection`() = runTest {
        val api = catalogApi(
            delayByQuery = mapOf("cat" to 1_000L, "cats" to 100L),
            failures = mapOf("cat" to SearchFailedException("cat"))
        )

        assertEquals(
            listOf(catsResult),
            FlowOperatorsPractice.searchLatest(timedFlow(0L to "cat", 50L to "cats"), api).toList()
        )
        assertEquals(listOf("cat"), api.cancelledQueries)
    }

    @Test
    fun `searchLatest fails collection when the latest search fails`() = runTest {
        val api = catalogApi(
            delayMillis = 100,
            failures = mapOf("bad" to SearchFailedException("bad"))
        )
        val emitted = mutableListOf<SearchResult>()

        val error = assertFailsWith<SearchFailedException> {
            FlowOperatorsPractice.searchLatest(flow { emit("bad") }, api).collect { emitted += it }
        }

        assertEquals("bad", error.query)
        assertEquals(emptyList(), emitted)
        assertEquals(listOf("bad"), api.startedQueries)
        assertEquals(emptyList(), api.completedQueries)
    }

    @Test
    fun `searchLatest emits nothing for an empty flow`() = runTest {
        val api = catalogApi()

        assertEquals(
            emptyList(),
            FlowOperatorsPractice.searchLatest(flow<String> { }, api).toList()
        )
        assertEquals(emptyList(), api.startedQueries)
        assertEquals(0, currentTime)
    }

    @Test
    fun `searchAllInOrder runs searches one after another`() = runTest {
        val api = catalogApi(
            delayByQuery = mapOf("cat" to 1_000L, "cats" to 100L)
        )
        val results = FlowOperatorsPractice
            .searchAllInOrder(
                flow {
                    emit("cat")
                    emit("cats")
                },
                api
            )
            .toList()

        assertEquals(listOf(catResult, catsResult), results)
        assertEquals(listOf("cat", "cats"), api.startedQueries)
        assertEquals(listOf("cat", "cats"), api.completedQueries)
        assertEquals(emptyList(), api.cancelledQueries)
        assertEquals(1_100, currentTime)
    }

    @Test
    fun `searchAllInOrder starts the next search only after the previous finishes`() = runTest {
        val api = catalogApi(
            delayByQuery = mapOf("cat" to 1_000L, "cats" to 100L)
        )
        val startedDuringFirstSearch = mutableListOf<String>()

        val job = launch {
            FlowOperatorsPractice
                .searchAllInOrder(
                    flow {
                        emit("cat")
                        emit("cats")
                    },
                    api
                )
                .collect { }
        }
        testScheduler.advanceTimeBy(50)
        testScheduler.runCurrent()
        startedDuringFirstSearch += api.startedQueries

        job.join()

        assertEquals(listOf("cat"), startedDuringFirstSearch)
        assertEquals(listOf("cat", "cats"), api.startedQueries)
    }

    @Test
    fun `searchAllInOrder emits nothing for an empty flow`() = runTest {
        val api = catalogApi()

        assertEquals(
            emptyList(),
            FlowOperatorsPractice.searchAllInOrder(flow<String> { }, api).toList()
        )
        assertEquals(emptyList(), api.startedQueries)
    }

    @Test
    fun `collectLatestInto keeps only the last overlapping result`() = runTest {
        val destination = mutableListOf<SearchResult>()

        FlowOperatorsPractice.collectLatestInto(
            timedFlow(0L to catResult, 100L to catsResult, 200L to iphoneResult),
            delayMillis = 1_000,
            destination = destination
        )

        assertEquals(listOf(iphoneResult), destination)
        assertEquals(1_200, currentTime)
    }

    @Test
    fun `collectLatestInto keeps spaced results`() = runTest {
        val destination = mutableListOf<SearchResult>()

        FlowOperatorsPractice.collectLatestInto(
            timedFlow(0L to catResult, 1_500L to iphoneResult),
            delayMillis = 1_000,
            destination = destination
        )

        assertEquals(listOf(catResult, iphoneResult), destination)
        assertEquals(2_500, currentTime)
    }

    @Test
    fun `collectLatestInto does nothing for an empty flow`() = runTest {
        val destination = mutableListOf<SearchResult>()

        FlowOperatorsPractice.collectLatestInto(
            flow<SearchResult> { },
            delayMillis = 1_000,
            destination = destination
        )

        assertEquals(emptyList(), destination)
        assertEquals(0, currentTime)
    }

    @Test
    fun `collectLatestInto cancel during processing does not append`() = runTest {
        val destination = mutableListOf<SearchResult>()

        val job = launch {
            FlowOperatorsPractice.collectLatestInto(
                timedFlow(0L to iphoneResult, lingerMillis = 10_000),
                delayMillis = 1_000,
                destination = destination
            )
        }
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(emptyList(), destination)
        assertEquals(500, currentTime)
    }

    @Test
    fun `searchWhileTyping searches only the settled iphone query`() = runTest {
        val api = catalogApi()
        val queries = timedFlow(
            0L to "i",
            50L to "ip",
            100L to "iph",
            150L to "ipho",
            200L to "iphone",
            lingerMillis = 400
        )

        assertEquals(
            listOf(iphoneResult),
            FlowOperatorsPractice.searchWhileTyping(queries, api, debounceMillis = 300).toList()
        )
        assertEquals(listOf("iphone"), api.startedQueries)
        assertEquals(listOf("iphone"), api.completedQueries)
        assertEquals(emptyList(), api.cancelledQueries)
    }

    @Test
    fun `searchWhileTyping does not search until typing pauses`() = runTest {
        val api = catalogApi()
        val emitted = mutableListOf<SearchResult>()

        val job = launch {
            FlowOperatorsPractice
                .searchWhileTyping(
                    timedFlow(0L to "iphone", lingerMillis = 10_000),
                    api,
                    debounceMillis = 300
                )
                .collect { emitted += it }
        }
        testScheduler.advanceTimeBy(299)
        testScheduler.runCurrent()
        assertEquals(emptyList(), api.startedQueries)
        assertEquals(emptyList(), emitted)

        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertEquals(listOf("iphone"), api.startedQueries)
        assertEquals(listOf(iphoneResult), emitted)
        assertEquals(300, currentTime)

        job.cancelAndJoin()
    }

    @Test
    fun `searchWhileTyping does not search a duplicate settled query`() = runTest {
        val api = catalogApi()
        val queries = timedFlow(
            0L to "iphone",
            1_000L to "iphone",
            lingerMillis = 400
        )

        assertEquals(
            listOf(iphoneResult),
            FlowOperatorsPractice.searchWhileTyping(queries, api, debounceMillis = 300).toList()
        )
        assertEquals(listOf("iphone"), api.startedQueries)
        assertEquals(listOf("iphone"), api.completedQueries)
    }

    @Test
    fun `searchWhileTyping cancels an in-flight search when the query changes`() = runTest {
        val api = catalogApi(
            delayByQuery = mapOf("cat" to 1_000L, "cats" to 100L)
        )
        val queries = timedFlow(
            0L to "cat",
            400L to "cats",
            lingerMillis = 400
        )

        assertEquals(
            listOf(catsResult),
            FlowOperatorsPractice.searchWhileTyping(queries, api, debounceMillis = 300).toList()
        )
        assertEquals(listOf("cat", "cats"), api.startedQueries)
        assertEquals(listOf("cats"), api.completedQueries)
        assertEquals(listOf("cat"), api.cancelledQueries)
    }

    @Test
    fun `searchWhileTyping does not search a blank query`() = runTest {
        val api = catalogApi()
        val queries = timedFlow(
            0L to "iphone",
            1_000L to "",
            lingerMillis = 400
        )

        assertEquals(
            listOf(iphoneResult),
            FlowOperatorsPractice.searchWhileTyping(queries, api, debounceMillis = 300).toList()
        )
        assertEquals(listOf("iphone"), api.startedQueries)
        assertTrue("" !in api.startedQueries)
        assertTrue(" " !in api.startedQueries)
    }

    @Test
    fun `searchWhileTyping emits nothing for an empty flow`() = runTest {
        val api = catalogApi()

        assertEquals(
            emptyList(),
            FlowOperatorsPractice.searchWhileTyping(flow<String> { }, api, debounceMillis = 300).toList()
        )
        assertEquals(emptyList(), api.startedQueries)
        assertEquals(0, currentTime)
    }

    private fun catalogApi(
        delayMillis: Long = 0L,
        delayByQuery: Map<String, Long> = emptyMap(),
        failures: Map<String, Throwable> = emptyMap()
    ): FakeSearchApi {
        return FakeSearchApi(
            itemsByQuery = mapOf(
                "iphone" to listOf(iphone, iphoneCase),
                "ipad" to listOf(ipad),
                "cat" to listOf(catToy),
                "cats" to listOf(catsPoster)
            ),
            delayMillis = delayMillis,
            delayByQuery = delayByQuery,
            failures = failures
        )
    }

    private fun <T> timedFlow(
        vararg emissions: Pair<Long, T>,
        lingerMillis: Long = 0L
    ): Flow<T> {
        return flow {
            var lastAt = 0L
            for ((at, value) in emissions) {
                delay(at - lastAt)
                emit(value)
                lastAt = at
            }
            if (lingerMillis > 0L) {
                delay(lingerMillis)
            }
        }
    }
}

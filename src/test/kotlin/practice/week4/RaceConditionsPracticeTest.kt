package practice.week4

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RaceConditionsPracticeTest {

    private val catToy = CatalogHit("c1", "Cat Toy")
    private val catsPoster = CatalogHit("c2", "Cats Poster")
    private val iphone = CatalogHit("p1", "iPhone")

    @Test
    fun `state is not a MutableStateFlow`() = runTest {
        val api = StaleSearchApi()
        val vm = SearchController(api, backgroundScope, debounceMillis = 0)
        val state: StateFlow<SearchUiState> = vm.state

        assertTrue(state !is MutableStateFlow)
    }

    @Test
    fun `onQueryChanged updates the visible query immediately`() = runTest {
        val api = StaleSearchApi()
        val vm = SearchController(api, backgroundScope, debounceMillis = 300)
        testScheduler.runCurrent()

        vm.onQueryChanged("c")
        testScheduler.runCurrent()

        assertEquals("c", vm.state.value.query)
        assertEquals(0, api.startedQueries.size)
    }

    @Test
    fun `fast typing collapses to one search after debounce`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf("iphone" to listOf(iphone))
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 300)
        testScheduler.runCurrent()

        vm.onQueryChanged("i")
        testScheduler.advanceTimeBy(50)
        vm.onQueryChanged("ip")
        testScheduler.advanceTimeBy(50)
        vm.onQueryChanged("iph")
        testScheduler.advanceTimeBy(50)
        vm.onQueryChanged("ipho")
        testScheduler.advanceTimeBy(50)
        vm.onQueryChanged("iphone")
        testScheduler.advanceTimeBy(300)
        testScheduler.runCurrent()

        assertEquals(listOf("iphone"), api.startedQueries)
        assertEquals(listOf(iphone), vm.state.value.results)
        assertEquals(false, vm.state.value.loading)
    }

    @Test
    fun `duplicate settled query does not search again`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf("iphone" to listOf(iphone))
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 100)
        testScheduler.runCurrent()

        vm.onQueryChanged("iphone")
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()
        vm.onQueryChanged("iphone")
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        assertEquals(listOf("iphone"), api.startedQueries)
        assertEquals(1, api.completedQueries.size)
    }

    @Test
    fun `slow older query cannot replace a faster newer query`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf(
                "cat" to listOf(catToy),
                "cats" to listOf(catsPoster)
            ),
            delayByQuery = mapOf(
                "cat" to 1_000L,
                "cats" to 100L
            )
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 0)
        testScheduler.runCurrent()

        vm.onQueryChanged("cat")
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        vm.onQueryChanged("cats")
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertEquals(listOf("cat", "cats"), api.startedQueries)
        assertEquals(listOf("cat"), api.cancelledQueries)
        assertEquals(listOf("cats"), api.completedQueries)
        assertEquals(listOf(catsPoster), vm.state.value.results)
        assertEquals("cats", vm.state.value.query)
        assertEquals(false, vm.state.value.loading)
    }

    @Test
    fun `blank query does not call the API`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf("iphone" to listOf(iphone))
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 0)
        testScheduler.runCurrent()

        vm.onQueryChanged("   ")
        testScheduler.advanceUntilIdle()
        vm.onQueryChanged("")
        testScheduler.advanceUntilIdle()

        assertEquals(emptyList(), api.startedQueries)
        assertEquals(false, vm.state.value.loading)
    }

    @Test
    fun `blank query cancels an in-flight search`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf("cat" to listOf(catToy)),
            delayMillis = 1_000
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 0)
        testScheduler.runCurrent()

        vm.onQueryChanged("cat")
        testScheduler.advanceTimeBy(10)
        testScheduler.runCurrent()
        vm.onQueryChanged("")
        testScheduler.runCurrent()

        assertEquals(listOf("cat"), api.startedQueries)
        assertEquals(listOf("cat"), api.cancelledQueries)
        assertEquals(emptyList(), api.completedQueries)
        assertEquals(emptyList(), vm.state.value.results)
        assertEquals(false, vm.state.value.loading)
    }

    @Test
    fun `search failure keeps previous results`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf("cat" to listOf(catToy)),
            failures = mapOf("cats" to CatalogSearchException("cats"))
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 0)
        testScheduler.runCurrent()

        vm.onQueryChanged("cat")
        testScheduler.runCurrent()
        vm.onQueryChanged("cats")
        testScheduler.runCurrent()

        assertEquals(listOf(catToy), vm.state.value.results)
        assertEquals("Search failed: cats", vm.state.value.error)
        assertEquals(false, vm.state.value.loading)
    }

    @Test
    fun `search marks loading until the result arrives`() = runTest {
        val api = StaleSearchApi(
            itemsByQuery = mapOf("cat" to listOf(catToy)),
            delayMillis = 400
        )
        val vm = SearchController(api, backgroundScope, debounceMillis = 0)
        testScheduler.runCurrent()

        vm.onQueryChanged("cat")
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()

        assertEquals(true, vm.state.value.loading)
        assertEquals("cat", vm.state.value.query)

        testScheduler.advanceTimeBy(400)
        testScheduler.runCurrent()

        assertEquals(false, vm.state.value.loading)
        assertEquals(listOf(catToy), vm.state.value.results)
        assertEquals(401, currentTime)
    }

    @Test
    fun `collecting state does not complete`() = runTest {
        val vm = SearchController(StaleSearchApi(), backgroundScope, debounceMillis = 0)
        val emitted = mutableListOf<SearchUiState>()

        val job = launch { vm.state.collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(SearchUiState()), emitted)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }
}

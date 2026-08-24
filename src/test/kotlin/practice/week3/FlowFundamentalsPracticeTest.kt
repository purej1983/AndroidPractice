package practice.week3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class FlowFundamentalsPracticeTest {

    private val pending = Order("o1", "c1", 10, OrderStatus.PENDING)
    private val paid = Order("o2", "c1", 50, OrderStatus.PAID)
    private val shipped = Order("o3", "c2", 20, OrderStatus.SHIPPED)
    private val cancelled = Order("o4", "c1", 5, OrderStatus.CANCELLED)
    private val paidLate = Order("o5", "c2", 30, OrderStatus.PAID)
    private val allOrders = listOf(pending, paid, shipped, cancelled, paidLate)

    @Test
    fun `emitEach emits values in order`() = runTest {
        assertEquals(
            listOf("a", "b", "c"),
            FlowFundamentalsPractice.emitEach(listOf("a", "b", "c")).toList()
        )
    }

    @Test
    fun `emitEach emits nothing for an empty list`() = runTest {
        assertEquals(
            emptyList(),
            FlowFundamentalsPractice.emitEach<String>(emptyList()).toList()
        )
    }

    @Test
    fun `emitEach is cold so a later list mutation is visible`() = runTest {
        val values = mutableListOf("a")
        val flow = FlowFundamentalsPractice.emitEach(values)

        values += "b"

        assertEquals(listOf("a", "b"), flow.toList())
    }

    @Test
    fun `emitEach collecting twice yields the same values`() = runTest {
        val flow = FlowFundamentalsPractice.emitEach(listOf(1, 2, 3))

        assertEquals(listOf(1, 2, 3), flow.toList())
        assertEquals(listOf(1, 2, 3), flow.toList())
    }

    @Test
    fun `emitEachDelayed emits values in order`() = runTest {
        assertEquals(
            listOf("a", "b"),
            FlowFundamentalsPractice.emitEachDelayed(listOf("a", "b"), delayMillis = 0).toList()
        )
    }

    @Test
    fun `emitEachDelayed delays before every emission so times add up`() = runTest {
        val values = FlowFundamentalsPractice
            .emitEachDelayed(listOf("a", "b", "c"), delayMillis = 1_000)
            .toList()

        assertEquals(listOf("a", "b", "c"), values)
        assertEquals(3_000, currentTime)
    }

    @Test
    fun `emitEachDelayed does not delay when the list is empty`() = runTest {
        assertEquals(
            emptyList(),
            FlowFundamentalsPractice.emitEachDelayed<String>(emptyList(), delayMillis = 1_000).toList()
        )
        assertEquals(0, currentTime)
    }

    @Test
    fun `emitEachDelayed can be cancelled between emissions`() = runTest {
        val values = mutableListOf<String>()

        val job = launch {
            FlowFundamentalsPractice
                .emitEachDelayed(listOf("a", "b", "c"), delayMillis = 1_000)
                .collect { values += it }
        }
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertEquals(listOf("a"), values)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `emitEachDelayed collectors run independently and overlap`() = runTest {
        val flow = FlowFundamentalsPractice.emitEachDelayed(listOf("a", "b"), delayMillis = 1_000)
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { flow.collect { first += it } }
        val job2 = launch { flow.collect { second += it } }
        job1.join()
        job2.join()

        assertEquals(listOf("a", "b"), first)
        assertEquals(listOf("a", "b"), second)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `collectAll returns every emitted value`() = runTest {
        val flow = flow {
            emit("a")
            emit("b")
            emit("c")
        }

        assertEquals(listOf("a", "b", "c"), FlowFundamentalsPractice.collectAll(flow))
    }

    @Test
    fun `collectAll returns empty for an empty flow`() = runTest {
        val flow = flow<String> { }

        assertEquals(emptyList(), FlowFundamentalsPractice.collectAll(flow))
    }

    @Test
    fun `collectAll waits for delayed emissions`() = runTest {
        val flow = flow {
            delay(400)
            emit("a")
            delay(600)
            emit("b")
        }

        assertEquals(listOf("a", "b"), FlowFundamentalsPractice.collectAll(flow))
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `observeOrders emits every snapshot in order`() = runTest {
        val first = listOf(pending)
        val second = listOf(pending, paid)
        val store = FakeOrderStore(snapshots = listOf(first, second))

        assertEquals(
            listOf(first, second),
            FlowFundamentalsPractice.observeOrders(store).toList()
        )
        assertEquals(listOf(0, 1), store.readIndexes)
    }

    @Test
    fun `observeOrders does not read the store until collected`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(paid), listOf(paid, shipped)),
            delayMillis = 1_000
        )

        val flow = FlowFundamentalsPractice.observeOrders(store)

        assertEquals(emptyList(), store.readIndexes)
        assertEquals(0, currentTime)

        assertEquals(
            listOf(listOf(paid), listOf(paid, shipped)),
            flow.toList()
        )
        assertEquals(listOf(0, 1), store.readIndexes)
        assertEquals(2_000, currentTime)
    }

    @Test
    fun `observeOrders is cold so a second collector reads again`() = runTest {
        val snapshots = listOf(listOf(pending), listOf(paid))
        val store = FakeOrderStore(snapshots = snapshots)
        val flow = FlowFundamentalsPractice.observeOrders(store)

        assertEquals(snapshots, flow.toList())
        assertEquals(snapshots, flow.toList())
        assertEquals(listOf(0, 1, 0, 1), store.readIndexes)
    }

    @Test
    fun `observeOrders delays once per snapshot`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(paid), listOf(shipped)),
            delayMillis = 500
        )

        assertEquals(
            listOf(listOf(pending), listOf(paid), listOf(shipped)),
            FlowFundamentalsPractice.observeOrders(store).toList()
        )
        assertEquals(1_500, currentTime)
    }

    @Test
    fun `observeOrders emits nothing when the store has no snapshots`() = runTest {
        val store = FakeOrderStore()

        assertEquals(emptyList(), FlowFundamentalsPractice.observeOrders(store).toList())
        assertEquals(emptyList(), store.readIndexes)
        assertEquals(0, currentTime)
    }

    @Test
    fun `observeOrders fails collection after earlier snapshots`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(paid), listOf(shipped)),
            failures = mapOf(1 to SnapshotReadException(1))
        )
        val emitted = mutableListOf<List<Order>>()

        val error = assertFailsWith<SnapshotReadException> {
            FlowFundamentalsPractice.observeOrders(store).collect { emitted += it }
        }

        assertEquals(1, error.index)
        assertEquals(listOf(listOf(pending)), emitted)
        assertEquals(listOf(0, 1), store.readIndexes)
    }

    @Test
    fun `observeOrders cancel stops remaining snapshot reads`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(paid), listOf(shipped)),
            delayMillis = 1_000
        )
        val emitted = mutableListOf<List<Order>>()

        val job = launch {
            FlowFundamentalsPractice.observeOrders(store).collect { emitted += it }
        }
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        job.cancelAndJoin()

        // First snapshot was emitted. The next read already started, then cancel
        // stopped it before a second emission.
        assertEquals(listOf(listOf(pending)), emitted)
        assertEquals(listOf(0, 1), store.readIndexes)
        assertEquals(1_000, currentTime)
    }

    @Test
    fun `orderIds maps each order to its id`() = runTest {
        assertEquals(
            listOf("o1", "o2", "o3", "o4", "o5"),
            FlowFundamentalsPractice.orderIds(allOrders).toList()
        )
    }

    @Test
    fun `orderIds emits nothing for an empty list`() = runTest {
        assertEquals(emptyList(), FlowFundamentalsPractice.orderIds(emptyList()).toList())
    }

    @Test
    fun `paidOrders keeps only paid orders in original order`() = runTest {
        assertEquals(
            listOf(paid, paidLate),
            FlowFundamentalsPractice.paidOrders(allOrders).toList()
        )
    }

    @Test
    fun `paidOrders emits nothing when none are paid`() = runTest {
        assertEquals(
            emptyList(),
            FlowFundamentalsPractice.paidOrders(listOf(pending, shipped, cancelled)).toList()
        )
    }

    @Test
    fun `paidOrders emits every order when all are paid`() = runTest {
        assertEquals(
            listOf(paid, paidLate),
            FlowFundamentalsPractice.paidOrders(listOf(paid, paidLate)).toList()
        )
    }

    @Test
    fun `paidAmounts emits paid amounts in original order`() = runTest {
        assertEquals(
            listOf(50, 30),
            FlowFundamentalsPractice.paidAmounts(allOrders).toList()
        )
    }

    @Test
    fun `paidAmounts emits nothing when none are paid`() = runTest {
        assertEquals(
            emptyList(),
            FlowFundamentalsPractice.paidAmounts(listOf(pending, cancelled)).toList()
        )
    }

    @Test
    fun `loadOrdersOnce returns the latest snapshot`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(pending, paid))
        )

        assertEquals(listOf(pending, paid), FlowFundamentalsPractice.loadOrdersOnce(store))
        assertEquals(1, store.oneShotLoadCount)
        assertEquals(emptyList(), store.readIndexes)
    }

    @Test
    fun `loadOrdersOnce returns empty when the store has no snapshots`() = runTest {
        val store = FakeOrderStore()

        assertEquals(emptyList(), FlowFundamentalsPractice.loadOrdersOnce(store))
        assertEquals(1, store.oneShotLoadCount)
    }

    @Test
    fun `loadOrdersOnce suspends once instead of per snapshot`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(paid), listOf(shipped)),
            delayMillis = 1_000
        )

        assertEquals(listOf(shipped), FlowFundamentalsPractice.loadOrdersOnce(store))
        assertEquals(1_000, currentTime)
        assertEquals(1, store.oneShotLoadCount)
        assertEquals(emptyList(), store.readIndexes)
    }

    @Test
    fun `loadOrdersOnce does not emit intermediate snapshots`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(pending, paid))
        )

        val once = FlowFundamentalsPractice.loadOrdersOnce(store)
        val observed = FlowFundamentalsPractice.observeOrders(store).toList()

        assertEquals(listOf(pending, paid), once)
        assertEquals(listOf(listOf(pending), listOf(pending, paid)), observed)
        assertEquals(1, store.oneShotLoadCount)
        assertEquals(listOf(0, 1), store.readIndexes)
    }

    @Test
    fun `observePaidOrders maps each snapshot to paid orders`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(
                listOf(pending, paid),
                listOf(paid, shipped, paidLate)
            )
        )

        assertEquals(
            listOf(listOf(paid), listOf(paid, paidLate)),
            FlowFundamentalsPractice.observePaidOrders(store).toList()
        )
    }

    @Test
    fun `observePaidOrders still emits empty paid lists`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(pending), listOf(paid, cancelled))
        )

        assertEquals(
            listOf(emptyList(), listOf(paid)),
            FlowFundamentalsPractice.observePaidOrders(store).toList()
        )
    }

    @Test
    fun `observePaidOrders is still cold`() = runTest {
        val store = FakeOrderStore(
            snapshots = listOf(listOf(paid), listOf(pending, paidLate))
        )
        val flow = FlowFundamentalsPractice.observePaidOrders(store)

        assertEquals(emptyList(), store.readIndexes)
        assertEquals(listOf(listOf(paid), listOf(paidLate)), flow.toList())
        assertEquals(listOf(listOf(paid), listOf(paidLate)), flow.toList())
        assertEquals(listOf(0, 1, 0, 1), store.readIndexes)
    }
}

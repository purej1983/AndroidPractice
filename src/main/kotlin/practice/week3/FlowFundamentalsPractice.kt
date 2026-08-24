package practice.week3

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Day 11 — Flow fundamentals.
 *
 * Key ideas:
 * - A `Flow` is a stream of values over time. A `suspend` function returns
 *   one value, then finishes.
 * - `flow { }` builds a **cold** flow. The block runs only when collected,
 *   and it runs again for every collector.
 * - `emit` sends one value to the current collector.
 * - `collect` suspends and receives values until the flow completes or
 *   the collector is cancelled.
 * - `map` and `filter` transform a Flow. They stay lazy: nothing runs
 *   until someone collects the resulting Flow.
 * - `delay` inside a Flow is a cancellation point. Cancelling collection
 *   stops remaining emissions.
 */
enum class OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    CANCELLED
}

data class Order(
    val id: String,
    val customerId: String,
    val amount: Int,
    val status: OrderStatus
)

class SnapshotReadException(val index: Int) : IllegalStateException("Failed to read snapshot $index")

/**
 * In-memory order store for tests.
 *
 * Snapshots are private on purpose. Observation must go through
 * [readSnapshot] so tests can prove work starts only on collect.
 * [loadOrders] is the one-shot `suspend` counterpart: latest snapshot only.
 */
class FakeOrderStore(
    private val snapshots: List<List<Order>> = emptyList(),
    private val delayMillis: Long = 0L,
    private val failures: Map<Int, Throwable> = emptyMap()
) {
    private val _readIndexes = mutableListOf<Int>()
    private var _oneShotLoadCount = 0

    val readIndexes: List<Int> get() = _readIndexes.toList()
    val oneShotLoadCount: Int get() = _oneShotLoadCount
    val snapshotCount: Int get() = snapshots.size

    suspend fun readSnapshot(index: Int): List<Order> {
        _readIndexes += index
        delay(delayMillis)
        failures[index]?.let { throw it }
        return snapshots.getOrElse(index) { emptyList() }
    }

    suspend fun loadOrders(): List<Order> {
        _oneShotLoadCount += 1
        delay(delayMillis)
        return snapshots.lastOrNull().orEmpty()
    }
}

object FlowFundamentalsPractice {

    /**
     * Exercise 1 — flow builder and emit
     *
     * Return a cold Flow that emits each item in [values] in order.
     *
     * The Flow must read [values] at collection time, not at construction
     * time, so a later mutation of the list is visible to a collector.
     *
     * Requirement: use `flow { }` and `emit`. Do not use `flowOf` or `asFlow`.
     */
    fun <T> emitEach(values: List<T>): Flow<T> {
        return flow {
            values.forEach { emit(it) }
        }
    }

    /**
     * Exercise 2 — delayed emissions
     *
     * Return a Flow that emits each item in [values] in order.
     * Suspend [delayMillis] before every emission, including the first.
     *
     * Requirement: use `flow`, `emit`, and `delay`. Do not use `Thread.sleep`.
     */
    fun <T> emitEachDelayed(values: List<T>, delayMillis: Long): Flow<T> {
        TODO()
    }

    /**
     * Exercise 3 — collect
     *
     * Collect [flow] and return every emitted value in a list, in order.
     *
     * Requirement: use `collect`. Do not use `toList()`.
     */
    suspend fun <T> collectAll(flow: Flow<T>): List<T> {
        TODO()
    }

    /**
     * Exercise 4 — observe orders (cold Flow)
     *
     * Return a Flow that reads every snapshot from [store] in index order
     * (`0` until [FakeOrderStore.snapshotCount]) and emits each snapshot.
     *
     * Work must start only when collected. A second collector must read
     * the snapshots again.
     *
     * Requirement: use `flow` and [FakeOrderStore.readSnapshot].
     * Do not share one running producer across collectors.
     */
    fun observeOrders(store: FakeOrderStore): Flow<List<Order>> {
        TODO()
    }

    /**
     * Exercise 5 — map
     *
     * Emit every order id from [orders], in the same order.
     *
     * Requirement: start from [emitEach] and use `map`. Do not loop with `emit`.
     */
    fun orderIds(orders: List<Order>): Flow<String> {
        TODO()
    }

    /**
     * Exercise 6 — filter
     *
     * Emit only PAID orders from [orders], in the same order.
     *
     * Requirement: start from [emitEach] and use `filter`.
     */
    fun paidOrders(orders: List<Order>): Flow<Order> {
        TODO()
    }

    /**
     * Exercise 7 — filter then map
     *
     * Emit the amounts of PAID orders from [orders], in the same order.
     *
     * Requirement: use `filter` and `map` on a Flow. Do not pre-filter the list.
     */
    fun paidAmounts(orders: List<Order>): Flow<Int> {
        TODO()
    }

    /**
     * Exercise 8 — Flow vs suspend
     *
     * Load the current orders once from [store] and return them.
     * This is the one-shot counterpart of [observeOrders]: latest snapshot
     * only, no stream of updates.
     *
     * Requirement: this must stay a one-shot `suspend` function.
     * Use [FakeOrderStore.loadOrders]. Do not return a Flow.
     */
    suspend fun loadOrdersOnce(store: FakeOrderStore): List<Order> {
        TODO()
    }

    /**
     * Exercise 9 — transform observed snapshots
     *
     * Observe [store] with [observeOrders], then emit only the paid orders
     * from each snapshot (same snapshot order). Empty paid lists still emit.
     *
     * Requirement: use `map` on the Flow from [observeOrders].
     */
    fun observePaidOrders(store: FakeOrderStore): Flow<List<Order>> {
        TODO()
    }
}

package practice.week1

enum class OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    CANCELLED
}

data class LineItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Int
)

data class Order(
    val id: String,
    val customerId: String,
    val status: OrderStatus,
    val items: List<LineItem>,
    val couponCode: String? = null
)

data class CustomerSummary(
    val customerId: String,
    val orderCount: Int,
    val totalAmount: Int
)

object CollectionsPractice {

    /**
     * Exercise 1 — filter
     *
     * Return orders that are not CANCELLED, preserving original order.
     *
     * Requirement: use `filter`.
     */
    fun activeOrders(orders: List<Order>): List<Order> {
        return orders.filter{ it.status != OrderStatus.CANCELLED }
    }

    /**
     * Exercise 2 — map
     *
     * Return the ids of [orders], preserving original order.
     *
     * Requirement: use `map`.
     */
    fun orderIds(orders: List<Order>): List<String> {
        return orders.map{ it.id }
    }

    /**
     * Exercise 3 — mapNotNull
     *
     * Return coupon codes from [orders], skipping orders with a null coupon.
     *
     * Requirement: use `mapNotNull`.
     */
    fun couponCodes(orders: List<Order>): List<String> {
        return orders.mapNotNull{ it.couponCode }
    }

    /**
     * Exercise 4 — flatMap
     *
     * Return every line-item name across all [orders], preserving order.
     *
     * Requirement: use `flatMap`. Do not use nested loops.
     */
    fun itemNames(orders: List<Order>): List<String> {
        return orders.flatMap{ it.items }.map{ it.name }
    }

    /**
     * Exercise 5 — associate
     *
     * Return a map of order id to order.
     * If two orders share an id, the later one wins.
     *
     * Requirement: use `associate`.
     */
    fun ordersById(orders: List<Order>): Map<String, Order> {
        return orders.associate{ it.id to it }
    }

    /**
     * Exercise 6 — groupBy
     *
     * Group [orders] by [Order.customerId].
     *
     * Requirement: use `groupBy`.
     */
    fun ordersByCustomer(orders: List<Order>): Map<String, List<Order>> {
        return orders.groupBy{ it.customerId }
    }

    /**
     * Exercise 7 — fold
     *
     * Return the total amount of [orders].
     * An order's amount is the sum of `quantity * unitPrice` for its items.
     *
     * Requirement: use `fold`. Do not use `sum` or `sumOf`.
     */
    fun totalAmount(orders: List<Order>): Int {
        return orders.flatMap{ it.items }.fold(0) {total, item -> total + item.quantity * item.unitPrice }
    }

    /**
     * Exercise 8 — any
     *
     * Return true if at least one order is CANCELLED.
     *
     * Requirement: use `any`.
     */
    fun hasCancelled(orders: List<Order>): Boolean {
        return orders.any{ it.status == OrderStatus.CANCELLED }
    }

    /**
     * Exercise 9 — all
     *
     * Return true if every order is PAID or SHIPPED.
     * An empty list should return true.
     *
     * Requirement: use `all`.
     */
    fun allFulfilled(orders: List<Order>): Boolean {
        return orders.all{ it.status == OrderStatus.PAID || it.status == OrderStatus.SHIPPED }
    }

    /**
     * Exercise 10 — Sequence
     *
     * Build one [CustomerSummary] per customer from non-cancelled [orders]:
     * - ignore CANCELLED orders
     * - group remaining orders by customerId
     * - orderCount is the number of remaining orders
     * - totalAmount is the sum of those orders' item totals
     *
     * Return summaries sorted by customerId.
     *
     * Requirement: start with `asSequence()`.
     */
    fun customerSummaries(orders: List<Order>): List<CustomerSummary> {
        return orders
        .asSequence()
        .filter{ it.status != OrderStatus.CANCELLED }
        .groupBy{ it.customerId }
        .map{entry ->
            CustomerSummary(
                customerId = entry.key,
                orderCount = entry.value.size,
                totalAmount = totalAmount(entry.value)
            )
        }.sortedBy{ it.customerId }
    }

    /**
     * Exercise 11 — lazy evaluation
     *
     * Return the amount of the first PAID order in [orders].
     * Amount is the sum of `quantity * unitPrice`.
     * Return null if none are PAID.
     *
     * Stay lazy: do not convert [orders] to a List.
     *
     * Requirement: use Sequence operations. Do not call `toList()`.
     */
    fun firstPaidAmount(orders: Sequence<Order>): Int? {
        return orders.firstOrNull { it.status == OrderStatus.PAID }
        ?.let { order ->
            order.items.sumOf { item ->
                item.unitPrice * item.quantity
            }
        }
    }
}

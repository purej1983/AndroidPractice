package practice.week1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionsPracticeTest {

    @Test
    fun `activeOrders keeps non-cancelled orders in original order`() {
        val pending = order(id = "o1", status = OrderStatus.PENDING)
        val paid = order(id = "o2", status = OrderStatus.PAID)
        val shipped = order(id = "o3", status = OrderStatus.SHIPPED)
        val cancelled = order(id = "o4", status = OrderStatus.CANCELLED)

        assertEquals(
            listOf(pending, paid, shipped),
            CollectionsPractice.activeOrders(listOf(pending, cancelled, paid, shipped))
        )
    }

    @Test
    fun `activeOrders returns empty when every order is cancelled`() {
        val cancelled = order(id = "o1", status = OrderStatus.CANCELLED)

        assertEquals(
            emptyList(),
            CollectionsPractice.activeOrders(listOf(cancelled))
        )
    }

    @Test
    fun `orderIds maps each order to its id`() {
        val orders = listOf(
            order(id = "o1"),
            order(id = "o2"),
            order(id = "o3")
        )

        assertEquals(
            listOf("o1", "o2", "o3"),
            CollectionsPractice.orderIds(orders)
        )
    }

    @Test
    fun `couponCodes skips orders without a coupon`() {
        val orders = listOf(
            order(id = "o1", couponCode = "SAVE10"),
            order(id = "o2", couponCode = null),
            order(id = "o3", couponCode = "FREESHIP")
        )

        assertEquals(
            listOf("SAVE10", "FREESHIP"),
            CollectionsPractice.couponCodes(orders)
        )
    }

    @Test
    fun `itemNames flattens line item names across orders`() {
        val orders = listOf(
            order(
                id = "o1",
                items = listOf(
                    LineItem("Keyboard", 1, 100),
                    LineItem("Mouse", 2, 40)
                )
            ),
            order(
                id = "o2",
                items = listOf(LineItem("Cable", 3, 10))
            )
        )

        assertEquals(
            listOf("Keyboard", "Mouse", "Cable"),
            CollectionsPractice.itemNames(orders)
        )
    }

    @Test
    fun `itemNames returns empty when orders have no items`() {
        val orders = listOf(order(id = "o1", items = emptyList()))

        assertEquals(emptyList(), CollectionsPractice.itemNames(orders))
    }

    @Test
    fun `ordersById associates each id to its order`() {
        val first = order(id = "o1", customerId = "cust-1")
        val second = order(id = "o2", customerId = "cust-2")

        assertEquals(
            mapOf("o1" to first, "o2" to second),
            CollectionsPractice.ordersById(listOf(first, second))
        )
    }

    @Test
    fun `ordersById keeps the later order when ids collide`() {
        val first = order(id = "o1", customerId = "cust-1")
        val replacement = order(id = "o1", customerId = "cust-9")

        assertEquals(
            mapOf("o1" to replacement),
            CollectionsPractice.ordersById(listOf(first, replacement))
        )
    }

    @Test
    fun `ordersByCustomer groups orders by customer id`() {
        val alice1 = order(id = "o1", customerId = "alice")
        val bob = order(id = "o2", customerId = "bob")
        val alice2 = order(id = "o3", customerId = "alice")

        assertEquals(
            mapOf(
                "alice" to listOf(alice1, alice2),
                "bob" to listOf(bob)
            ),
            CollectionsPractice.ordersByCustomer(listOf(alice1, bob, alice2))
        )
    }

    @Test
    fun `totalAmount folds quantity times unit price across all items`() {
        val orders = listOf(
            order(
                id = "o1",
                items = listOf(
                    LineItem("Keyboard", 1, 100),
                    LineItem("Mouse", 2, 40)
                )
            ),
            order(
                id = "o2",
                items = listOf(LineItem("Cable", 3, 10))
            )
        )

        assertEquals(210, CollectionsPractice.totalAmount(orders))
    }

    @Test
    fun `totalAmount is zero for an empty list`() {
        assertEquals(0, CollectionsPractice.totalAmount(emptyList()))
    }

    @Test
    fun `hasCancelled is true when any order is cancelled`() {
        val orders = listOf(
            order(id = "o1", status = OrderStatus.PAID),
            order(id = "o2", status = OrderStatus.CANCELLED)
        )

        assertTrue(CollectionsPractice.hasCancelled(orders))
    }

    @Test
    fun `hasCancelled is false when no order is cancelled`() {
        val orders = listOf(
            order(id = "o1", status = OrderStatus.PAID),
            order(id = "o2", status = OrderStatus.SHIPPED)
        )

        assertFalse(CollectionsPractice.hasCancelled(orders))
    }

    @Test
    fun `allFulfilled is true when every order is paid or shipped`() {
        val orders = listOf(
            order(id = "o1", status = OrderStatus.PAID),
            order(id = "o2", status = OrderStatus.SHIPPED)
        )

        assertTrue(CollectionsPractice.allFulfilled(orders))
    }

    @Test
    fun `allFulfilled is false when any order is still pending`() {
        val orders = listOf(
            order(id = "o1", status = OrderStatus.PAID),
            order(id = "o2", status = OrderStatus.PENDING)
        )

        assertFalse(CollectionsPractice.allFulfilled(orders))
    }

    @Test
    fun `allFulfilled is true for an empty list`() {
        assertTrue(CollectionsPractice.allFulfilled(emptyList()))
    }

    @Test
    fun `customerSummaries ignores cancelled orders and sorts by customer id`() {
        val orders = listOf(
            order(
                id = "o1",
                customerId = "bob",
                status = OrderStatus.PAID,
                items = listOf(LineItem("Cable", 2, 10))
            ),
            order(
                id = "o2",
                customerId = "alice",
                status = OrderStatus.CANCELLED,
                items = listOf(LineItem("Ignored", 9, 100))
            ),
            order(
                id = "o3",
                customerId = "alice",
                status = OrderStatus.SHIPPED,
                items = listOf(LineItem("Keyboard", 1, 100))
            ),
            order(
                id = "o4",
                customerId = "alice",
                status = OrderStatus.PAID,
                items = listOf(LineItem("Mouse", 2, 40))
            )
        )

        assertEquals(
            listOf(
                CustomerSummary(customerId = "alice", orderCount = 2, totalAmount = 180),
                CustomerSummary(customerId = "bob", orderCount = 1, totalAmount = 20)
            ),
            CollectionsPractice.customerSummaries(orders)
        )
    }

    @Test
    fun `customerSummaries omits customers who only have cancelled orders`() {
        val orders = listOf(
            order(
                id = "o1",
                customerId = "alice",
                status = OrderStatus.CANCELLED,
                items = listOf(LineItem("Keyboard", 1, 100))
            )
        )

        assertEquals(emptyList(), CollectionsPractice.customerSummaries(orders))
    }

    @Test
    fun `firstPaidAmount returns the first paid order amount`() {
        val orders = sequenceOf(
            order(
                id = "o1",
                status = OrderStatus.CANCELLED,
                items = listOf(LineItem("Ignored", 1, 999))
            ),
            order(
                id = "o2",
                status = OrderStatus.PAID,
                items = listOf(LineItem("Keyboard", 1, 100), LineItem("Mouse", 2, 40))
            ),
            order(
                id = "o3",
                status = OrderStatus.PAID,
                items = listOf(LineItem("Later", 1, 1))
            )
        )

        assertEquals(180, CollectionsPractice.firstPaidAmount(orders))
    }

    @Test
    fun `firstPaidAmount returns null when no order is paid`() {
        val orders = sequenceOf(
            order(id = "o1", status = OrderStatus.PENDING),
            order(id = "o2", status = OrderStatus.CANCELLED)
        )

        assertNull(CollectionsPractice.firstPaidAmount(orders))
    }

    @Test
    fun `firstPaidAmount stays lazy and does not visit later orders`() {
        var visited = 0
        val orders = sequence {
            yield(order(id = "o1", status = OrderStatus.CANCELLED).also { visited++ })
            yield(
                order(
                    id = "o2",
                    status = OrderStatus.PAID,
                    items = listOf(LineItem("Keyboard", 1, 50))
                ).also { visited++ }
            )
            yield(order(id = "o3", status = OrderStatus.PAID).also { visited++ })
        }

        assertEquals(50, CollectionsPractice.firstPaidAmount(orders))
        assertEquals(2, visited)
    }

    private fun order(
        id: String,
        customerId: String = "cust-1",
        status: OrderStatus = OrderStatus.PAID,
        items: List<LineItem> = listOf(LineItem("Keyboard", 1, 100)),
        couponCode: String? = null
    ) = Order(
        id = id,
        customerId = customerId,
        status = status,
        items = items,
        couponCode = couponCode
    )
}

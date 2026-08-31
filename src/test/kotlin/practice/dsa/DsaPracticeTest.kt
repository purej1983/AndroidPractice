package practice.dsa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DsaPracticeTest {

    @Test
    fun `LRU get missing key is null`() {
        val cache = LruCache<String, Int>(2)
        assertNull(cache.get("a"))
    }

    @Test
    fun `LRU evicts the least recently used key`() {
        val cache = LruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a")
        cache.put("c", 3)

        assertNull(cache.get("b"))
        assertEquals(1, cache.get("a"))
        assertEquals(3, cache.get("c"))
        assertEquals(listOf("a", "c"), cache.snapshot())
    }

    @Test
    fun `LRU put on an existing key counts as a use`() {
        val cache = LruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("a", 9)
        cache.put("c", 3)

        assertNull(cache.get("b"))
        assertEquals(9, cache.get("a"))
    }

    @Test
    fun `rate limiter allows up to the max in the window`() {
        var now = 1_000L
        val limiter = SlidingWindowRateLimiter(maxRequests = 2, windowMillis = 1_000, clock = { now })

        assertTrue(limiter.allow())
        assertTrue(limiter.allow())
        assertFalse(limiter.allow())
        now = 2_001L
        assertTrue(limiter.allow())
    }

    @Test
    fun `rate limiter timestamps on the window edge fall out`() {
        var now = 0L
        val limiter = SlidingWindowRateLimiter(maxRequests = 1, windowMillis = 100, clock = { now })

        assertTrue(limiter.allow())
        now = 100L
        assertTrue(limiter.allow())
    }

    @Test
    fun `mergeK merges sorted lists`() {
        assertEquals(
            listOf(1, 1, 2, 3, 4, 5, 6),
            MergeSortedPractice.mergeK(
                listOf(
                    listOf(1, 4, 6),
                    listOf(2, 3),
                    listOf(1, 5)
                )
            )
        )
    }

    @Test
    fun `mergeK empty lists`() {
        assertEquals(emptyList(), MergeSortedPractice.mergeK(emptyList()))
        assertEquals(emptyList(), MergeSortedPractice.mergeK(listOf(emptyList())))
    }

    @Test
    fun `twoSumSorted finds a pair`() {
        assertEquals(2 to 7, MergeSortedPractice.twoSumSorted(listOf(2, 3, 7, 11), 9))
        assertNull(MergeSortedPractice.twoSumSorted(listOf(1, 2, 3), 100))
    }

    @Test
    fun `longest substring without repeating characters`() {
        assertEquals(3, SlidingWindowPractice.lengthOfLongestSubstring("abcabcbb"))
        assertEquals(1, SlidingWindowPractice.lengthOfLongestSubstring("bbbbb"))
        assertEquals(3, SlidingWindowPractice.lengthOfLongestSubstring("pwwkew"))
        assertEquals(0, SlidingWindowPractice.lengthOfLongestSubstring(""))
    }

    @Test
    fun `max sum of k`() {
        assertEquals(9, SlidingWindowPractice.maxSumOfK(listOf(2, 1, 5, 1, 3, 2), 3))
        assertNull(SlidingWindowPractice.maxSumOfK(listOf(1, 2), 3))
    }

    @Test
    fun `course schedule detects a cycle`() {
        assertTrue(GraphPractice.canFinish(2, listOf(1 to 0)))
        assertFalse(GraphPractice.canFinish(2, listOf(1 to 0, 0 to 1)))
        assertTrue(GraphPractice.canFinish(1, emptyList()))
    }

    @Test
    fun `grid BFS shortest path`() {
        val open = listOf(
            listOf(0, 0, 0),
            listOf(1, 1, 0),
            listOf(0, 0, 0)
        )
        assertEquals(5, GraphPractice.shortestPath(open))
        assertNull(GraphPractice.shortestPath(listOf(listOf(1))))
        assertEquals(1, GraphPractice.shortestPath(listOf(listOf(0))))
    }

    @Test
    fun `merge overlapping intervals`() {
        assertEquals(
            listOf(1..6, 8..10, 15..18),
            IntervalsPractice.merge(listOf(1..3, 2..6, 8..10, 15..18))
        )
        assertEquals(listOf(1..5), IntervalsPractice.merge(listOf(1..4, 4..5)))
        assertEquals(emptyList(), IntervalsPractice.merge(emptyList()))
    }
}

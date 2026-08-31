package practice.dsa

/**
 * LRU cache — FAANG favorite that maps to Coil/Glide memory cache.
 *
 * Newest-used at the tail. On overflow, evict the head (least recent).
 * `get` and `put` of an existing key both count as use.
 *
 * Production: `LinkedHashMap(capacity, 0.75f, accessOrder = true)` plus
 * `removeEldestEntry`. If the interviewer wants pointers, say
 * HashMap + doubly linked list; same complexity, O(1) get/put.
 */
class LruCache<K, V>(private val capacity: Int) {
    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    private val map = object : LinkedHashMap<K, V>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > capacity
        }
    }

    fun get(key: K): V? = map[key]

    fun put(key: K, value: V) {
        map[key] = value
    }

    fun snapshot(): List<K> = map.keys.toList()
}

/**
 * Sliding-window rate limiter — maps to "debounce / latest-wins" product talk
 * and to OkHttp interceptors.
 *
 * Allow at most [maxRequests] in the last [windowMillis] milliseconds.
 * Older timestamps fall out of the window. Not a token bucket: a burst
 * at t=0 then one more at t=window-1 still counts as the same window.
 */
class SlidingWindowRateLimiter(
    private val maxRequests: Int,
    private val windowMillis: Long,
    private val clock: () -> Long
) {
    private val timestamps = ArrayDeque<Long>()

    fun allow(): Boolean {
        val now = clock()
        val cutoff = now - windowMillis
        while (timestamps.isNotEmpty() && timestamps.first() <= cutoff) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= maxRequests) return false
        timestamps.addLast(now)
        return true
    }
}

/**
 * Merge k sorted lists — heap. Same idea as merging paged APIs.
 */
object MergeSortedPractice {
    fun mergeK(lists: List<List<Int>>): List<Int> {
        data class Node(val value: Int, val listIndex: Int, val elementIndex: Int)

        val heap = java.util.PriorityQueue(compareBy<Node> { it.value })
        lists.forEachIndexed { listIndex, list ->
            if (list.isNotEmpty()) {
                heap.add(Node(list[0], listIndex, 0))
            }
        }
        val out = ArrayList<Int>()
        while (heap.isNotEmpty()) {
            val node = heap.remove()
            out += node.value
            val next = node.elementIndex + 1
            val list = lists[node.listIndex]
            if (next < list.size) {
                heap.add(Node(list[next], node.listIndex, next))
            }
        }
        return out
    }

    /**
     * Two pointers on a sorted array. Pair that sums to [target].
     */
    fun twoSumSorted(nums: List<Int>, target: Int): Pair<Int, Int>? {
        var left = 0
        var right = nums.lastIndex
        while (left < right) {
            val sum = nums[left] + nums[right]
            when {
                sum == target -> return nums[left] to nums[right]
                sum < target -> left += 1
                else -> right -= 1
            }
        }
        return null
    }
}

/**
 * Sliding window on a string — longest substring without repeating chars.
 */
object SlidingWindowPractice {
    fun lengthOfLongestSubstring(s: String): Int {
        val lastIndex = HashMap<Char, Int>()
        var start = 0
        var best = 0
        s.forEachIndexed { index, char ->
            val previous = lastIndex[char]
            if (previous != null && previous >= start) {
                start = previous + 1
            }
            lastIndex[char] = index
            best = maxOf(best, index - start + 1)
        }
        return best
    }

    /**
     * Max sum of any subarray of length [k]. Returns null if the array is
     * shorter than [k].
     */
    fun maxSumOfK(nums: List<Int>, k: Int): Int? {
        if (k <= 0 || nums.size < k) return null
        var window = nums.take(k).sum()
        var best = window
        for (index in k until nums.size) {
            window += nums[index] - nums[index - k]
            best = maxOf(best, window)
        }
        return best
    }
}

/**
 * Course schedule — can you finish all courses? Kahn topological sort.
 * `prerequisites[i] = [a, b]` means b must be taken before a.
 */
object GraphPractice {
    fun canFinish(numCourses: Int, prerequisites: List<Pair<Int, Int>>): Boolean {
        val indegree = IntArray(numCourses)
        val edges = List(numCourses) { mutableListOf<Int>() }
        for ((course, needed) in prerequisites) {
            edges[needed].add(course)
            indegree[course] += 1
        }
        val ready = ArrayDeque<Int>()
        indegree.forEachIndexed { course, count ->
            if (count == 0) ready.add(course)
        }
        var taken = 0
        while (ready.isNotEmpty()) {
            val course = ready.removeFirst()
            taken += 1
            for (next in edges[course]) {
                indegree[next] -= 1
                if (indegree[next] == 0) ready.add(next)
            }
        }
        return taken == numCourses
    }

    /**
     * Grid BFS shortest path. 0 is open, 1 is a wall. Start at (0,0),
     * end at last cell. 4-directional. Null if unreachable.
     */
    fun shortestPath(grid: List<List<Int>>): Int? {
        if (grid.isEmpty() || grid[0].isEmpty() || grid[0][0] == 1) return null
        val rows = grid.size
        val cols = grid[0].size
        if (grid[rows - 1][cols - 1] == 1) return null
        val seen = Array(rows) { BooleanArray(cols) }
        val queue = ArrayDeque<Triple<Int, Int, Int>>()
        queue.add(Triple(0, 0, 1))
        seen[0][0] = true
        val deltas = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
        while (queue.isNotEmpty()) {
            val (row, col, dist) = queue.removeFirst()
            if (row == rows - 1 && col == cols - 1) return dist
            for ((dr, dc) in deltas) {
                val nr = row + dr
                val nc = col + dc
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                if (seen[nr][nc] || grid[nr][nc] == 1) continue
                seen[nr][nc] = true
                queue.add(Triple(nr, nc, dist + 1))
            }
        }
        return null
    }
}

/**
 * Interval merge — calendar / "don't overlap work windows".
 */
object IntervalsPractice {
    fun merge(intervals: List<IntRange>): List<IntRange> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val merged = mutableListOf(sorted.first())
        for (next in sorted.drop(1)) {
            val last = merged.last()
            if (next.first <= last.last) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, next.last)
            } else {
                merged += next
            }
        }
        return merged
    }
}

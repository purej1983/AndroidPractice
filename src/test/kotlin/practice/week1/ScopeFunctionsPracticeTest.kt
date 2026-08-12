package practice.week1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScopeFunctionsPracticeTest {

    @Test
    fun `normalizedEmail uppercases non-null email`() {
        assertEquals(
            "TEST@EXAMPLE.COM",
            ScopeFunctionsPractice.normalizedEmail("test@example.com")
        )
    }

    @Test
    fun `normalizedEmail returns fallback for null`() {
        assertEquals(
            "NO_EMAIL",
            ScopeFunctionsPractice.normalizedEmail(null)
        )
    }

    @Test
    fun `displayName returns expected string`() {
        val user = User("Thomas", "thomas@example.com")

        assertEquals(
            "Thomas <thomas@example.com>",
            ScopeFunctionsPractice.displayName(user)
        )
    }

    @Test
    fun `activate modifies and returns same user instance`() {
        val user = User("  Thomas  ", "THOMAS@EXAMPLE.COM")

        val result = ScopeFunctionsPractice.activate(user)

        assertSame(user, result)
        assertEquals("Thomas", result.name)
        assertEquals("thomas@example.com", result.email)
        assertTrue(result.active)
    }

    @Test
    fun `addAndLog adds value logs message and returns same list`() {
        val numbers = mutableListOf(1, 2)
        val logs = mutableListOf<String>()

        val result = ScopeFunctionsPractice.addAndLog(
            numbers = numbers,
            value = 3,
            logger = logs::add
        )

        assertSame(numbers, result)
        assertEquals(listOf(1, 2, 3), result)
        assertEquals(listOf("Added: 3"), logs)
    }
}

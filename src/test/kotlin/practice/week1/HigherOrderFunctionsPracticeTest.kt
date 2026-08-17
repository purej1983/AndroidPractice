package practice.week1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HigherOrderFunctionsPracticeTest {

    @Test
    fun `keepIf keeps items matching the predicate`() {
        val items = listOf(1, 2, 3, 4, 5)

        assertEquals(
            listOf(2, 4),
            HigherOrderFunctionsPractice.keepIf(items) { it % 2 == 0 }
        )
    }

    @Test
    fun `keepIf uses the provided predicate rather than a hardcoded rule`() {
        val items = listOf("ab", "a", "abcd")

        assertEquals(
            listOf("ab", "abcd"),
            HigherOrderFunctionsPractice.keepIf(items) { it.length >= 2 }
        )
    }

    @Test
    fun `keepIf returns empty when nothing matches`() {
        assertEquals(
            emptyList(),
            HigherOrderFunctionsPractice.keepIf(listOf(1, 3, 5)) { it % 2 == 0 }
        )
    }

    @Test
    fun `transformEach maps every item with the transform`() {
        val items = listOf("a", "bb", "ccc")

        assertEquals(
            listOf(1, 2, 3),
            HigherOrderFunctionsPractice.transformEach(items) { it.length }
        )
    }

    @Test
    fun `transformEach uses the provided transform rather than a hardcoded mapping`() {
        val items = listOf(1, 2, 3)

        assertEquals(
            listOf("1", "2", "3"),
            HigherOrderFunctionsPractice.transformEach(items) { it.toString() }
        )
    }

    @Test
    fun `fieldNames returns each field name in original order`() {
        val fields = listOf(
            FormField(name = "email", value = "thomas@example.com"),
            FormField(name = "city", value = "Hong Kong")
        )

        assertEquals(
            listOf("email", "city"),
            HigherOrderFunctionsPractice.fieldNames(fields)
        )
    }

    @Test
    fun `fieldNames returns empty for an empty list`() {
        assertEquals(
            emptyList(),
            HigherOrderFunctionsPractice.fieldNames(emptyList())
        )
    }

    @Test
    fun `startsWith returns a predicate that captures the prefix`() {
        val startsWithTh = HigherOrderFunctionsPractice.startsWith("th")

        assertTrue(startsWithTh("thomas"))
        assertTrue(startsWithTh("th"))
        assertFalse(startsWithTh("lam"))
        assertFalse(startsWithTh("Thomas"))
    }

    @Test
    fun `startsWith can be reused with a different prefix`() {
        val startsWithKt = HigherOrderFunctionsPractice.startsWith("kt")

        assertTrue(startsWithKt("ktx"))
        assertFalse(startsWithKt("kotlin"))
    }

    @Test
    fun `isValidEmail accepts a simple email`() {
        assertTrue("thomas@example.com".isValidEmail())
        assertTrue("a@b".isValidEmail())
    }

    @Test
    fun `isValidEmail rejects missing or extra at signs`() {
        assertFalse("thomas".isValidEmail())
        assertFalse("a@b@c".isValidEmail())
        assertFalse("".isValidEmail())
    }

    @Test
    fun `isValidEmail rejects blank local or domain parts`() {
        assertFalse("@example.com".isValidEmail())
        assertFalse("thomas@".isValidEmail())
        assertFalse(" @example.com".isValidEmail())
        assertFalse("thomas@ ".isValidEmail())
    }

    @Test
    fun `secondOrNull returns the second element`() {
        assertEquals("b", listOf("a", "b", "c").secondOrNull())
        assertEquals(2, listOf(1, 2).secondOrNull())
    }

    @Test
    fun `secondOrNull returns null when the list is too short`() {
        assertNull(emptyList<String>().secondOrNull())
        assertNull(listOf("only").secondOrNull())
    }

    @Test
    fun `allOf returns null when every validator passes`() {
        val validator = HigherOrderFunctionsPractice.allOf(
            { value: String -> if (value.isBlank()) "blank" else null },
            { value: String -> if (value.length < 3) "short" else null }
        )

        assertNull(validator("hello"))
    }

    @Test
    fun `allOf returns the first error and skips later validators`() {
        var laterRan = false
        val validator = HigherOrderFunctionsPractice.allOf(
            { value: String -> if (value.isBlank()) "blank" else null },
            { _: String ->
                laterRan = true
                "later"
            }
        )

        assertEquals("blank", validator("   "))
        assertFalse(laterRan)
    }

    @Test
    fun `allOf returns a later error when earlier validators pass`() {
        val validator = HigherOrderFunctionsPractice.allOf(
            { value: String -> if (value.isBlank()) "blank" else null },
            { value: String -> if (value.length < 3) "short" else null }
        )

        assertEquals("short", validator("ab"))
    }

    @Test
    fun `allOf with no validators is always valid`() {
        val validator = HigherOrderFunctionsPractice.allOf<String>()

        assertNull(validator("anything"))
    }

    @Test
    fun `allOf does not run validators until the returned function is called`() {
        var built = false
        val validator = HigherOrderFunctionsPractice.allOf<String>(
            {
                built = true
                null
            }
        )

        assertFalse(built)
        assertNull(validator("ok"))
        assertTrue(built)
    }

    @Test
    fun `retryUntil returns the first result that should not be retried`() {
        var attempts = 0

        val result = HigherOrderFunctionsPractice.retryUntil(
            times = 5,
            shouldRetry = { it < 3 }
        ) {
            attempts += 1
            attempts
        }

        assertEquals(3, result)
        assertEquals(3, attempts)
    }

    @Test
    fun `retryUntil returns the last result when every attempt should retry`() {
        var attempts = 0

        val result = HigherOrderFunctionsPractice.retryUntil(
            times = 2,
            shouldRetry = { true }
        ) {
            attempts += 1
            attempts
        }

        assertEquals(2, result)
        assertEquals(2, attempts)
    }

    @Test
    fun `retryUntil calls block only once when the first result is accepted`() {
        var attempts = 0

        val result = HigherOrderFunctionsPractice.retryUntil(
            times = 4,
            shouldRetry = { false }
        ) {
            attempts += 1
            42
        }

        assertEquals(42, result)
        assertEquals(1, attempts)
    }

    @Test
    fun `buildText runs the receiver lambda against a StringBuilder`() {
        val text = HigherOrderFunctionsPractice.buildText {
            append("Hello")
            append(' ')
            append("Thomas")
        }

        assertEquals("Hello Thomas", text)
    }

    @Test
    fun `buildText returns empty when the block writes nothing`() {
        assertEquals("", HigherOrderFunctionsPractice.buildText { })
    }

    @Test
    fun `runCatchingOr returns the block result`() {
        val result = HigherOrderFunctionsPractice.runCatchingOr(
            onError = { "err" }
        ) {
            "ok"
        }

        assertEquals("ok", result)
    }

    @Test
    fun `runCatchingOr returns onError when the block throws`() {
        val result = HigherOrderFunctionsPractice.runCatchingOr(
            onError = { it.message ?: "err" }
        ) {
            error("boom")
        }

        assertEquals("boom", result)
    }

    @Test
    fun `runCatchingOr allows a non-local return from the caller`() {
        fun caller(): String {
            HigherOrderFunctionsPractice.runCatchingOr(
                onError = { "err" }
            ) {
                return "early"
            }
            return "late"
        }

        assertEquals("early", caller())
    }

    @Test
    fun `castOrNull returns the value when it is the target type`() {
        val value: Any? = "thomas"

        assertEquals("thomas", value.castOrNull<String>())
        assertEquals(7, (7 as Any).castOrNull<Int>())
    }

    @Test
    fun `castOrNull returns null when the type does not match`() {
        val value: Any? = 7

        assertNull(value.castOrNull<String>())
        assertNull(null.castOrNull<String>())
    }
}

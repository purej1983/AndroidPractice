package practice.week1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NullSafetyPracticeTest {

    @Test
    fun `lowercaseEmail lowercases a present email`() {
        assertEquals(
            "thomas@example.com",
            NullSafetyPractice.lowercaseEmail("Thomas@Example.COM")
        )
    }

    @Test
    fun `lowercaseEmail returns null when email is missing`() {
        assertNull(NullSafetyPractice.lowercaseEmail(null))
    }

    @Test
    fun `displayName prefers nickname`() {
        assertEquals(
            "Tom",
            NullSafetyPractice.displayName(nickname = "Tom", name = "Thomas")
        )
    }

    @Test
    fun `displayName falls back to name when nickname is missing`() {
        assertEquals(
            "Thomas",
            NullSafetyPractice.displayName(nickname = null, name = "Thomas")
        )
    }

    @Test
    fun `displayName falls back to Anonymous when both are missing`() {
        assertEquals(
            "Anonymous",
            NullSafetyPractice.displayName(nickname = null, name = null)
        )
    }

    @Test
    fun `requireId returns the id when present`() {
        assertEquals("user-1", NullSafetyPractice.requireId("user-1"))
    }

    @Test
    fun `requireId throws IllegalArgumentException when id is missing`() {
        assertFailsWith<IllegalArgumentException> {
            NullSafetyPractice.requireId(null)
        }
    }

    @Test
    fun `checkToken returns the token when present`() {
        assertEquals("abc123", NullSafetyPractice.checkToken("abc123"))
    }

    @Test
    fun `checkToken throws IllegalStateException when token is missing`() {
        assertFailsWith<IllegalStateException> {
            NullSafetyPractice.checkToken(null)
        }
    }

    @Test
    fun `toUserProfile converts a fully populated dto`() {
        val dto = UserDto(
            id = "user-1",
            name = "Thomas",
            email = "Thomas@Example.COM",
            age = 30,
            nickname = "Tom",
            bio = "Kotlin developer"
        )

        val profile = NullSafetyPractice.toUserProfile(dto)

        assertEquals(
            UserProfile(
                id = "user-1",
                name = "Thomas",
                email = "thomas@example.com",
                age = 30,
                nickname = "Tom",
                bio = "Kotlin developer"
            ),
            profile
        )
    }

    @Test
    fun `toUserProfile uses fallbacks for missing optional fields`() {
        val dto = UserDto(
            id = "user-1",
            name = "Thomas",
            email = null,
            age = null,
            nickname = null,
            bio = null
        )

        val profile = NullSafetyPractice.toUserProfile(dto)

        assertEquals(
            UserProfile(
                id = "user-1",
                name = "Thomas",
                email = null,
                age = 0,
                nickname = "Thomas",
                bio = null
            ),
            profile
        )
    }

    @Test
    fun `toUserProfile throws when required id is missing`() {
        val dto = UserDto(
            id = null,
            name = "Thomas",
            email = null,
            age = null,
            nickname = null,
            bio = null
        )

        assertFailsWith<IllegalArgumentException> {
            NullSafetyPractice.toUserProfile(dto)
        }
    }

    @Test
    fun `toUserProfile throws when required name is missing`() {
        val dto = UserDto(
            id = "user-1",
            name = null,
            email = null,
            age = null,
            nickname = null,
            bio = null
        )

        assertFailsWith<IllegalArgumentException> {
            NullSafetyPractice.toUserProfile(dto)
        }
    }
}

package practice.week1

/**
 * Nullable API payload. Every field can be missing because JSON/network data
 * is untrusted.
 */
data class UserDto(
    val id: String?,
    val name: String?,
    val email: String?,
    val age: Int?,
    val nickname: String?,
    val bio: String?
)

/**
 * Valid domain model after mapping. Required fields are non-null.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val email: String?,
    val age: Int,
    val nickname: String,
    val bio: String?
)

object NullSafetyPractice {

    /**
     * Exercise 1 — safe call `?.`
     *
     * If [email] is not null, return it lowercased.
     * If it is null, return null.
     *
     * Requirement: use `?.`. Do not use `!!`.
     */
    fun lowercaseEmail(email: String?): String? {
        TODO()
    }

    /**
     * Exercise 2 — Elvis `?:`
     *
     * Prefer [nickname] when it is not null.
     * Otherwise use [name].
     * If both are null, return "Anonymous".
     *
     * Requirement: use `?:`. Do not use `!!`.
     */
    fun displayName(nickname: String?, name: String?): String {
        TODO()
    }

    /**
     * Exercise 3 — requireNotNull
     *
     * Return [id] when it is present.
     * If it is null, throw [IllegalArgumentException].
     *
     * Requirement: use `requireNotNull`. Do not use `!!`.
     */
    fun requireId(id: String?): String {
        TODO()
    }

    /**
     * Exercise 4 — checkNotNull
     *
     * Return [token] when an active session exists.
     * If it is null, throw [IllegalStateException].
     *
     * Requirement: use `checkNotNull`. Do not use `!!`.
     */
    fun checkToken(token: String?): String {
        TODO()
    }

    /**
     * Exercise 5 — DTO to domain
     *
     * Convert a nullable API [dto] into a valid [UserProfile]:
     * - [UserDto.id] is required. Throw [IllegalArgumentException] if missing.
     * - [UserDto.name] is required. Throw [IllegalArgumentException] if missing.
     * - [UserDto.email] is optional. Lowercase it when present, otherwise keep null.
     * - [UserDto.age] is optional. Fall back to 0 when missing.
     * - [UserDto.nickname] is optional. Fall back to the resolved name when missing.
     * - [UserDto.bio] is optional. Keep null when missing.
     *
     * Requirement: do not use `!!`.
     */
    fun toUserProfile(dto: UserDto): UserProfile {
        TODO()
    }
}

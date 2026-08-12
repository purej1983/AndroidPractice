package practice.week1

data class User(
    var name: String,
    var email: String,
    var active: Boolean = false
)

object ScopeFunctionsPractice {

    /**
     * Exercise 1 — let
     *
     * Return the uppercase email if [email] is not null.
     * Return "NO_EMAIL" if it is null.
     *
     * Requirement: use `let` somewhere in your solution.
     */
    fun normalizedEmail(email: String?): String {
        return email?.let { it.uppercase() } ?: "NO_EMAIL"
    }

    /**
     * Exercise 2 — run
     *
     * Build and return this exact string:
     * "Thomas <thomas@example.com>"
     * using the properties of [user].
     *
     * Requirement: use `run`.
     */
    fun displayName(user: User): String {
        return user.run { "$name <$email>" }
    }

    /**
     * Exercise 3 — apply
     *
     * Update the SAME User instance:
     * - name = trimmed name
     * - email = lowercase email
     * - active = true
     *
     * Return that same instance.
     *
     * Requirement: use `apply`.
     */
    fun activate(user: User): User {
        return user.apply {
            name = name.trim()
            email = email.lowercase()
            active = true
        }
    }

    /**
     * Exercise 4 — also
     *
     * Add [value] to [numbers].
     * Call [logger] with exactly: "Added: <value>"
     * Return the SAME list instance.
     *
     * Requirement: use `also`.
     */
    fun addAndLog(
        numbers: MutableList<Int>,
        value: Int,
        logger: (String) -> Unit
    ): MutableList<Int> {
        return numbers.also {
            it.add(value)
            logger("Added: $value")
        }
    }
}

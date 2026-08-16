package practice.week1

data class FormField(
    val name: String,
    val value: String
)

object HigherOrderFunctionsPractice {

    /**
     * Exercise 1 — lambda parameter
     *
     * Return the items for which [predicate] is true, preserving original order.
     *
     * Requirement: use [predicate]. Do not hardcode the filter condition.
     */
    fun <T> keepIf(items: List<T>, predicate: (T) -> Boolean): List<T> {
        TODO()
    }

    /**
     * Exercise 2 — transform lambda
     *
     * Apply [transform] to every item, preserving original order.
     *
     * Requirement: use [transform]. Do not hardcode the mapping.
     */
    fun <T, R> transformEach(items: List<T>, transform: (T) -> R): List<R> {
        TODO()
    }

    /**
     * Exercise 3 — function reference
     *
     * Return each field's [FormField.name], preserving original order.
     *
     * Requirement: pass a function reference (`FormField::name`) into
     * [transformEach] or `map`. Do not write a lambda with a body.
     */
    fun fieldNames(fields: List<FormField>): List<String> {
        TODO()
    }

    /**
     * Exercise 4 — returning a function
     *
     * Return a predicate that is true when its argument starts with [prefix].
     *
     * Requirement: return a function. Do not apply it to a value here.
     */
    fun startsWith(prefix: String): (String) -> Boolean {
        TODO()
    }

    /**
     * Exercise 7 — composing validators
     *
     * Each validator returns an error message, or null when the value is valid.
     * Return a new validator that runs [validators] in order and returns the
     * first error, or null if every validator passes.
     *
     * An empty [validators] list is always valid.
     *
     * Requirement: return a function. Do not run the validators until that
     * function is called.
     */
    fun <T> allOf(vararg validators: (T) -> String?): (T) -> String? {
        TODO()
    }

    /**
     * Exercise 8 — retry condition
     *
     * Call [block] up to [times] times.
     * After each result, return it immediately if [shouldRetry] is false.
     * If every attempt still needs a retry, return the last result.
     *
     * [times] is at least 1.
     *
     * Requirement: use [shouldRetry] as the retry condition.
     */
    fun <T> retryUntil(
        times: Int,
        shouldRetry: (T) -> Boolean,
        block: () -> T
    ): T {
        TODO()
    }

    /**
     * Exercise 9 — lambda with receiver
     *
     * Create a StringBuilder, run [block] against it as the receiver, and
     * return the built string.
     *
     * Requirement: use [block] as a `StringBuilder.() -> Unit` receiver lambda.
     */
    fun buildText(block: StringBuilder.() -> Unit): String {
        TODO()
    }

    /**
     * Exercise 10 — inline
     *
     * Run [block] and return its result.
     * If [block] throws, return [onError] applied to the throwable.
     *
     * Requirement: keep this function `inline` so [block] can use a non-local
     * `return` from the caller.
     */
    inline fun <T> runCatchingOr(
        onError: (Throwable) -> T,
        block: () -> T
    ): T {
        TODO()
    }
}

/**
 * Exercise 5 — extension function
 *
 * Return true when this string looks like a simple email:
 * it contains exactly one `@`, and both the local part and the domain are
 * non-blank.
 *
 * Requirement: implement as an extension on [String].
 */
fun String.isValidEmail(): Boolean {
    TODO()
}

/**
 * Exercise 6 — generic extension
 *
 * Return the second element, or null if this list has fewer than two items.
 *
 * Requirement: implement as an extension on [List].
 */
fun <T> List<T>.secondOrNull(): T? {
    TODO()
}

/**
 * Exercise 11 — inline reified
 *
 * If this value is an instance of [T], return it as [T].
 * Otherwise return null.
 *
 * Requirement: use `inline` and `reified`. Do not use Java `Class` checks.
 */
inline fun <reified T> Any?.castOrNull(): T? {
    TODO()
}

package practice.week1

data class Address(
    val city: String,
    val country: String
)

data class Account(
    val id: String,
    val displayName: String,
    val email: String,
    val address: Address,
    val active: Boolean = true
)

/**
 * Async load result. Covariance (`out T`) lets [Loading] and [Error]
 * be used as `LoadResult<Account>`, `LoadResult<String>`, and so on.
 */
sealed interface LoadResult<out T> {
    data object Loading : LoadResult<Nothing>
    data class Success<T>(val data: T) : LoadResult<T>
    data class Error(val message: String) : LoadResult<Nothing>
}

sealed class ScreenState {
    data object Loading : ScreenState()
    data class Content(val title: String, val subtitle: String) : ScreenState()
    data class Failed(val message: String) : ScreenState()
}

object DataClassesPractice {

    /**
     * Exercise 1 — copy
     *
     * Return an [Account] with the same fields as [account], except `active = false`.
     *
     * Requirement: use `copy`. Do not mutate [account].
     */
    fun deactivate(account: Account): Account {
        return account.copy(active = false)
    }

    /**
     * Exercise 2 — copy one property
     *
     * Return an [Account] with [email] replaced. Every other field stays the same.
     *
     * Requirement: use `copy`. Do not mutate [account].
     */
    fun withEmail(account: Account, email: String): Account {
        return account.copy(email = email)
    }

    /**
     * Exercise 3 — nested copy
     *
     * Return an [Account] whose [Address.city] is [city].
     * Country and every other account field stay the same.
     *
     * Requirement: copy both [Account] and [Address]. Do not mutate [account].
     */
    fun withCity(account: Account, city: String): Account {
        return account.copy(address = account.address.copy(city = city))
    }

    /**
     * Exercise 4 — `==` vs `===`
     *
     * If [current] and [updated] are equal by value (`==`), return [current]
     * so callers keep the same instance.
     * Otherwise return [updated].
     *
     * Requirement: compare with `==`, not `===`.
     */
    fun replacedIfChanged(current: Account, updated: Account): Account {
        return if (current == updated) current else updated
    }

    /**
     * Exercise 5 — when
     *
     * Return true only when [result] is [LoadResult.Loading].
     *
     * Requirement: use `when`.
     */
    fun isLoading(result: LoadResult<*>): Boolean {
        return when(result) {
            LoadResult.Loading -> true
            else -> false
        }
    }

    /**
     * Exercise 6 — when
     *
     * Return the success data, or null for Loading and Error.
     *
     * Requirement: use `when`.
     */
    fun <T> dataOrNull(result: LoadResult<T>): T? {
        return when(result) {
            is LoadResult.Success -> result.data
            else -> null
        }
    }

    /**
     * Exercise 7 — when and errors
     *
     * Return the success data.
     * If [result] is Loading or Error, throw [IllegalStateException].
     *
     * Requirement: use `when`.
     */
    fun <T> requireData(result: LoadResult<T>): T {
        return when(result) {
            is LoadResult.Success -> result.data
            else -> throw IllegalStateException()
        }
    }

    /**
     * Exercise 8 — exhaustive when
     *
     * Transform success data with [transform].
     * Loading stays Loading. Error keeps the same message.
     *
     * Requirement: use `when`.
     */
    fun <T, R> mapData(result: LoadResult<T>, transform: (T) -> R): LoadResult<R> {
        TODO("Exercise 8: use when to map Success and pass through other states")
    }

    /**
     * Exercise 9 — recover
     *
     * If [result] is Error, return Success([fallback]).
     * Loading and Success are unchanged.
     *
     * Requirement: use `when`.
     */
    fun <T> recoverError(result: LoadResult<T>, fallback: T): LoadResult<T> {
        TODO("Exercise 9: use when to recover Error into Success")
    }

    /**
     * Exercise 10 — fold
     *
     * Convert every [LoadResult] branch into a single value:
     * - Loading -> [onLoading]
     * - Success -> [onSuccess] with the data
     * - Error -> [onError] with the message
     *
     * Requirement: use `when`.
     */
    fun <T, R> fold(
        result: LoadResult<T>,
        onLoading: () -> R,
        onSuccess: (T) -> R,
        onError: (String) -> R
    ): R {
        TODO("Exercise 10: use when to fold every LoadResult branch")
    }

    /**
     * Exercise 11 — sealed class mapping
     *
     * Convert [result] into [ScreenState]:
     * - Loading -> [ScreenState.Loading]
     * - Success -> [ScreenState.Content] with title = displayName, subtitle = email
     * - Error -> [ScreenState.Failed] with the error message
     *
     * Requirement: use `when`.
     */
    fun toScreenState(result: LoadResult<Account>): ScreenState {
        TODO("Exercise 11: map LoadResult to ScreenState with when")
    }
}

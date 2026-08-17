package practice.week1

import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

/**
 * Day 6 — Suspend functions.
 *
 * Key idea: `suspend` marks a function that can pause and resume.
 * It does **not** mean "run on a background thread".
 * `delay` suspends the coroutine; `Thread.sleep` blocks a thread.
 */
data class RemoteUser(
    val id: String,
    val name: String
)

class UserNotFoundException(val userId: String) : IllegalStateException("User not found: $userId")

interface UserDataSource {
    suspend fun fetchUser(id: String): RemoteUser?
}

/**
 * In-memory source for tests. [delayMillis] uses `delay`, so it suspends
 * instead of blocking a thread.
 */
class FakeUserDataSource(
    private val users: Map<String, RemoteUser> = emptyMap(),
    private val delayMillis: Long = 0L,
    private val failures: Map<String, Throwable> = emptyMap()
) : UserDataSource {
    private val _fetchedIds = mutableListOf<String>()
    val fetchedIds: List<String> get() = _fetchedIds.toList()

    override suspend fun fetchUser(id: String): RemoteUser? {
        _fetchedIds += id
        delay(delayMillis)
        failures[id]?.let { throw it }
        return users[id]
    }
}

object SuspendFunctionsPractice {

    /**
     * Exercise 1 — delay vs blocking
     *
     * Suspend for [delayMillis], then return [value].
     *
     * Requirement: use `delay`. Do not use `Thread.sleep`.
     */
    suspend fun delayedValue(value: String, delayMillis: Long): String {
        delay(delayMillis)
        return value
    }

    /**
     * Exercise 2 — delayed load
     *
     * Suspend for [delayMillis], then return a [RemoteUser] with [id] and [name].
     *
     * Requirement: use `delay`. Do not use `Thread.sleep`.
     */
    suspend fun fetchAfterDelay(id: String, name: String, delayMillis: Long): RemoteUser {
        delay(delayMillis)
        return RemoteUser(id = id, name = name)
    }

    /**
     * Exercise 3 — load from a data source
     *
     * Fetch [id] from [source].
     * If the user is missing, throw [UserNotFoundException] with that id.
     */
    suspend fun loadUser(source: UserDataSource, id: String): RemoteUser {
        return source.fetchUser(id) ?: throw UserNotFoundException(id)
    }

    /**
     * Exercise 4 — nullable load
     *
     * Fetch [id] from [source].
     * Return null when the user is missing. Do not throw.
     */
    suspend fun loadUserOrNull(source: UserDataSource, id: String): RemoteUser? {
        return source.fetchUser(id)
    }

    /**
     * Exercise 5 — calling suspend from suspend
     *
     * Return the user's name.
     * If the user is missing, throw [UserNotFoundException].
     *
     * Requirement: fetch with a suspend call, then read `name`.
     */
    suspend fun loadUserName(source: UserDataSource, id: String): String {
        return source.fetchUser(id)?.name ?: throw UserNotFoundException(id)
    }

    /**
     * Exercise 6 — sequential suspend calls
     *
     * Fetch each id in order. Skip ids that are missing.
     * Preserve the order of found users.
     *
     * Requirement: fetch sequentially, one after another. Do not load concurrently.
     */
    suspend fun loadUsers(source: UserDataSource, ids: List<String>): List<RemoteUser> {
        return ids.mapNotNull { loadUserOrNull(source, it) }
    }

    /**
     * Exercise 7 — recover from failure
     *
     * Fetch [id] from [source].
     * If the user is missing, or if fetch throws, return [fallback].
     *
     * Requirement: do not catch [CancellationException]. Cancellation must propagate.
     * A cancelled coroutine must not return [fallback].
     */
    suspend fun loadUserOrFallback(
        source: UserDataSource,
        id: String,
        fallback: RemoteUser
    ): RemoteUser {
        return try{
            loadUser(source, id)
        } catch(e: CancellationException) {
            throw e
        } catch(e: Exception) {
            fallback
        }
    }

    /**
     * Exercise 8 — suspend lambda
     *
     * Fetch [id], then return [transform] applied to that user.
     * If the user is missing, throw [UserNotFoundException].
     *
     * Requirement: call [transform]. It is suspend, so it may delay.
     */
    suspend fun <R> mapUser(
        source: UserDataSource,
        id: String,
        transform: suspend (RemoteUser) -> R
    ): R {
        return transform(loadUser(source, id))
    }

    /**
     * Exercise 9 — coroutine context
     *
     * Return the current [CoroutineName] value, or null if none is set.
     *
     * Requirement: read `coroutineContext`. This function must stay `suspend`.
     */
    suspend fun currentCoroutineName(): String? {
        return coroutineContext[CoroutineName]?.name
    }

    /**
     * Exercise 10 — changing context
     *
     * Run [block] with [CoroutineName] set to [name], and return its result.
     *
     * Requirement: use `withContext`. Do not change dispatchers.
     */
    suspend fun <T> withName(name: String, block: suspend () -> T): T {
        return withContext(CoroutineName(name)) {
            block()
        }
    }
}

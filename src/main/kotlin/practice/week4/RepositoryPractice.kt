package practice.week4

import kotlinx.coroutines.flow.Flow

/**
 * Day 16 — Repository pattern.
 *
 * Key ideas:
 * - A **data source** talks to one origin: API or database. It does not
 *   decide source of truth for the app.
 * - A **repository** owns that decision. UI never calls Remote and Local
 *   separately.
 * - Offline-friendly observation: [UserRepository.observeUsers] is the
 *   database. Refresh **writes** the API into the database. Collectors
 *   see cache first, then updates.
 * - Remote failure must not wipe usable cache. `replaceAll` after a
 *   successful fetch is how stale rows disappear.
 * - A UseCase is useful when more than one ViewModel shares the same
 *   orchestration. A single-screen repository is enough here.
 *
 * Architecture:
 *
 * ```
 *              Repository
 *              /        \
 *    RemoteDataSource  LocalDataSource
 *           ↓                 ↓
 *          API             Database
 * ```
 */
data class CachedUser(
    val id: String,
    val name: String
)

class RemoteUsersException(reason: String) : IllegalStateException(reason)

/**
 * One remote origin. Fake of Retrofit/Ktor. No caching.
 */
interface UserRemoteDataSource {
    suspend fun fetchUsers(): List<CachedUser>
}

/**
 * One local origin. Fake of Room. Always has a current snapshot.
 */
interface UserLocalDataSource {
    fun observeUsers(): Flow<List<CachedUser>>
    suspend fun currentUsers(): List<CachedUser>
    suspend fun replaceAll(users: List<CachedUser>)
    suspend fun upsert(user: CachedUser)
}

/**
 * App-facing boundary. UI talks only to this.
 */
interface UserRepository {
    fun observeUsers(): Flow<List<CachedUser>>
    suspend fun refresh()
}

/**
 * Exercise 1 — local source of truth
 *
 * In-memory database. [observeUsers] is a StateFlow of the current table.
 * A new collector must receive the current rows immediately.
 * [replaceAll] replaces the table (not a merge). [upsert] inserts or
 * replaces by id, keeping other rows.
 *
 * Requirement: keep a private `MutableStateFlow` and expose observation
 * with `asStateFlow`. Do not talk to the network here.
 */
class InMemoryUserLocalDataSource(
    initial: List<CachedUser> = emptyList()
) : UserLocalDataSource {
    override fun observeUsers(): Flow<List<CachedUser>> {
        TODO()
    }

    override suspend fun currentUsers(): List<CachedUser> {
        TODO()
    }

    override suspend fun replaceAll(users: List<CachedUser>) {
        TODO()
    }

    override suspend fun upsert(user: CachedUser) {
        TODO()
    }
}

/**
 * Exercise 2 — remote data source
 *
 * Network fetch with delay, failure, and cancellation accounting.
 * Tests use [startedFetches] / [completedFetches] / [cancelledFetches]
 * to prove the repository does not hide work, and does not treat
 * cancellation as a fetch error.
 */
class FakeUserRemoteDataSource(
    private var users: List<CachedUser> = emptyList(),
    private val delayMillis: Long = 0L,
    private var failure: Throwable? = null
) : UserRemoteDataSource {
    val startedFetches: Int get() = TODO()
    val completedFetches: Int get() = TODO()
    val cancelledFetches: Int get() = TODO()

    fun setUsers(next: List<CachedUser>) {
        TODO()
    }

    fun setFailure(error: Throwable?) {
        TODO()
    }

    override suspend fun fetchUsers(): List<CachedUser> {
        TODO()
    }
}

/**
 * Exercise 3 — cached repository
 *
 * [observeUsers] is **only** the local table. Do not fetch on collect.
 * [refresh] loads remote, then [UserLocalDataSource.replaceAll].
 * On remote failure, leave local unchanged and rethrow.
 * Do not catch [CancellationException].
 *
 * Requirement: database is the source of truth. API is an input to that
 * database, not a second stream the UI merges by hand.
 */
class CachedUserRepository(
    private val local: UserLocalDataSource,
    private val remote: UserRemoteDataSource
) : UserRepository {
    override fun observeUsers(): Flow<List<CachedUser>> {
        TODO()
    }

    override suspend fun refresh() {
        TODO()
    }
}

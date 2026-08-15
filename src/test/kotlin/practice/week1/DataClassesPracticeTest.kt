package practice.week1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataClassesPracticeTest {

    @Test
    fun `deactivate copies the account and sets active to false`() {
        val account = account(active = true)

        val result = DataClassesPractice.deactivate(account)

        assertEquals(account.copy(active = false), result)
        assertNotSame(account, result)
        assertTrue(account.active)
    }

    @Test
    fun `withEmail copies the account and replaces only email`() {
        val account = account(email = "old@example.com")

        val result = DataClassesPractice.withEmail(account, "new@example.com")

        assertEquals(account.copy(email = "new@example.com"), result)
        assertNotSame(account, result)
        assertEquals("old@example.com", account.email)
    }

    @Test
    fun `withCity copies account and address without mutating the original`() {
        val account = account(city = "Hong Kong", country = "HK")

        val result = DataClassesPractice.withCity(account, "Tokyo")

        assertEquals("Tokyo", result.address.city)
        assertEquals("HK", result.address.country)
        assertEquals("Hong Kong", account.address.city)
        assertEquals(account.copy(address = account.address.copy(city = "Tokyo")), result)
        assertNotSame(account, result)
        assertNotSame(account.address, result.address)
    }

    @Test
    fun `replacedIfChanged returns the current instance when values are equal`() {
        val current = account(email = "thomas@example.com")
        val updated = account(email = "thomas@example.com")

        val result = DataClassesPractice.replacedIfChanged(current, updated)

        assertEquals(current, updated)
        assertNotSame(current, updated)
        assertSame(current, result)
    }

    @Test
    fun `replacedIfChanged returns the updated instance when values differ`() {
        val current = account(email = "old@example.com")
        val updated = account(email = "new@example.com")

        val result = DataClassesPractice.replacedIfChanged(current, updated)

        assertSame(updated, result)
    }

    @Test
    fun `isLoading is true only for Loading`() {
        val account = account()

        assertTrue(DataClassesPractice.isLoading(LoadResult.Loading))
        assertFalse(DataClassesPractice.isLoading(LoadResult.Success(account)))
        assertFalse(DataClassesPractice.isLoading(LoadResult.Error("offline")))
    }

    @Test
    fun `dataOrNull returns success data`() {
        val account = account()

        assertEquals(account, DataClassesPractice.dataOrNull(LoadResult.Success(account)))
    }

    @Test
    fun `dataOrNull returns null for loading and error`() {
        assertNull(DataClassesPractice.dataOrNull<Account>(LoadResult.Loading))
        assertNull(DataClassesPractice.dataOrNull<Account>(LoadResult.Error("offline")))
    }

    @Test
    fun `requireData returns success data`() {
        val account = account()

        assertEquals(account, DataClassesPractice.requireData(LoadResult.Success(account)))
    }

    @Test
    fun `requireData throws IllegalStateException for loading`() {
        assertFailsWith<IllegalStateException> {
            DataClassesPractice.requireData<Account>(LoadResult.Loading)
        }
    }

    @Test
    fun `requireData throws IllegalStateException for error`() {
        assertFailsWith<IllegalStateException> {
            DataClassesPractice.requireData<Account>(LoadResult.Error("offline"))
        }
    }

    @Test
    fun `mapData transforms success data`() {
        val account = account(displayName = "Thomas")

        assertEquals(
            LoadResult.Success("THOMAS"),
            DataClassesPractice.mapData(LoadResult.Success(account)) { it.displayName.uppercase() }
        )
    }

    @Test
    fun `mapData keeps loading as the same Loading instance`() {
        val loading: LoadResult<Account> = LoadResult.Loading

        val result = DataClassesPractice.mapData(loading) { it.id }

        assertEquals(LoadResult.Loading, result)
        assertSame(LoadResult.Loading, result)
    }

    @Test
    fun `mapData keeps the error message`() {
        val error: LoadResult<Account> = LoadResult.Error("offline")

        assertEquals(
            LoadResult.Error("offline"),
            DataClassesPractice.mapData(error) { it.id }
        )
    }

    @Test
    fun `recoverError turns error into success with the fallback`() {
        val fallback = account(id = "fallback")

        assertEquals(
            LoadResult.Success(fallback),
            DataClassesPractice.recoverError(LoadResult.Error("offline"), fallback)
        )
    }

    @Test
    fun `recoverError leaves loading and success unchanged`() {
        val account = account()
        val loading: LoadResult<Account> = LoadResult.Loading
        val success = LoadResult.Success(account)

        assertSame(LoadResult.Loading, DataClassesPractice.recoverError(loading, account()))
        assertEquals(success, DataClassesPractice.recoverError(success, account(id = "other")))
    }

    @Test
    fun `fold converts every LoadResult branch`() {
        val account = account(displayName = "Thomas")

        assertEquals(
            "wait",
            DataClassesPractice.fold(
                result = LoadResult.Loading,
                onLoading = { "wait" },
                onSuccess = { _: Account -> "ok" },
                onError = { "err" }
            )
        )
        assertEquals(
            "Thomas",
            DataClassesPractice.fold(
                result = LoadResult.Success(account),
                onLoading = { "wait" },
                onSuccess = { it.displayName },
                onError = { "err" }
            )
        )
        assertEquals(
            "offline",
            DataClassesPractice.fold(
                result = LoadResult.Error("offline"),
                onLoading = { "wait" },
                onSuccess = { _: Account -> "ok" },
                onError = { it }
            )
        )
    }

    @Test
    fun `toScreenState maps loading to the loading screen`() {
        assertEquals(
            ScreenState.Loading,
            DataClassesPractice.toScreenState(LoadResult.Loading)
        )
        assertSame(
            ScreenState.Loading,
            DataClassesPractice.toScreenState(LoadResult.Loading)
        )
    }

    @Test
    fun `toScreenState maps success to content`() {
        val account = account(displayName = "Thomas", email = "thomas@example.com")

        assertEquals(
            ScreenState.Content(title = "Thomas", subtitle = "thomas@example.com"),
            DataClassesPractice.toScreenState(LoadResult.Success(account))
        )
    }

    @Test
    fun `toScreenState maps error to failed`() {
        assertEquals(
            ScreenState.Failed("offline"),
            DataClassesPractice.toScreenState(LoadResult.Error("offline"))
        )
    }

    private fun account(
        id: String = "acc-1",
        displayName: String = "Thomas",
        email: String = "thomas@example.com",
        city: String = "Hong Kong",
        country: String = "HK",
        active: Boolean = true
    ) = Account(
        id = id,
        displayName = displayName,
        email = email,
        address = Address(city = city, country = country),
        active = active
    )
}

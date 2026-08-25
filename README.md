# Kotlin Deep Dive Practice — 1 Month Plan

A test-driven learning plan for an experienced Kotlin/Android developer who wants deeper understanding for senior-level interviews.

## How to use this repo

For every exercise:

1. Read the requirement.
2. **Do not modify the provided tests.**
3. Implement the production code containing `TODO()`.
4. Run the unit tests.
5. When tests pass, explain: **what you chose, why, alternatives, and trade-offs**.
6. Move to the next exercise only when you understand why the tests pass.

Recommended time: **60–90 minutes/day, 5 days/week**.

---

# Progress

**12 / 20 days implemented.** Week 1, Week 2, and Days 11–12 production code is in place; all **294** unit tests pass. Days 13–20 are not started.

| Day | Topic | Status | Exercises | Tests | Source |
|---|---|---|---|---|---|
| 1 | Scope Functions | Implemented | 4 | 5 | `week1/ScopeFunctionsPractice.kt` |
| 2 | Null Safety | Implemented | 5 | 13 | `week1/NullSafetyPractice.kt` |
| 3 | Collections and Sequences | Implemented | 11 | 21 | `week1/CollectionsPractice.kt` |
| 4 | Data Classes and Sealed Types | Implemented | 11 | 20 | `week1/DataClassesPractice.kt` |
| 5 | Higher-Order Functions | Implemented | 11 | 29 | `week1/HigherOrderFunctionsPractice.kt` |
| 6 | Suspend Functions | Implemented | 10 | 25 | `week2/SuspendFunctionsPractice.kt` |
| 7 | `launch` vs `async` | Implemented | 7 | 25 | `week2/LaunchAsyncPractice.kt` |
| 8 | Cancellation | Implemented | 9 | 26 | `week2/CancellationPractice.kt` |
| 9 | Exception Handling and Supervision | Implemented | 8 | 29 | `week2/ExceptionHandlingPractice.kt` |
| 10 | Dispatchers and Threading | Implemented | 9 | 32 | `week2/DispatchersPractice.kt` |
| 11 | Flow Fundamentals | Implemented | 9 | 33 | `week3/FlowFundamentalsPractice.kt` |
| 12 | Flow Operators | Implemented | 8 | 36 | `week3/FlowOperatorsPractice.kt` |
| 13 | StateFlow | Not started | — | — | — |
| 14 | SharedFlow | Not started | — | — | — |
| 15 | Channel vs SharedFlow vs StateFlow | Not started | — | — | — |
| 16 | Repository Pattern | Not started | — | — | — |
| 17 | State Management | Not started | — | — | — |
| 18 | Race Conditions and Search | Not started | — | — | — |
| 19 | Offline-First Architecture | Not started | — | — | — |
| 20 | Final Challenge: Task Manager | Not started | — | — | — |

Matching tests live under `src/test/kotlin/practice/` with the same week and file names.

---

# Week 1 — Kotlin Language Depth

**Status: implemented.** Days 1–5, 42 exercises, 88 tests passing.

## Day 1 — Scope Functions

**Status: implemented.**

Topics: `let`, `run`, `apply`, `also`, `this` vs `it`, receiver vs lambda return value.

Exercises:
- [x] `normalizedEmail` — uppercase a nullable email with `let`, fallback `"NO_EMAIL"`.
- [x] `displayName` — build `"Name <email>"` with `run`.
- [x] `activate` — trim, lowercase, and activate the same `User` with `apply`.
- [x] `addAndLog` — mutate a list, log a side effect, and return the same instance with `also`.

The original outline also mentioned `with`. There is no dedicated `with` exercise or test in this repo.

Tests verify null handling, return values, side effects, and same-object identity.

Interview targets:
- `let` vs `run`
- `apply` vs `also`
- Which functions return the receiver?
- When do scope functions hurt readability?

## Day 2 — Null Safety

**Status: implemented.**

Topics: `?.`, `?:`, nullable types, `requireNotNull`, `checkNotNull`, avoiding `!!`.

Exercises:
- [x] `lowercaseEmail` — safe call `?.`.
- [x] `displayName` — Elvis `?:` fallbacks.
- [x] `requireId` — `requireNotNull` for a required field.
- [x] `checkToken` — `checkNotNull` for a required token.
- [x] `toUserProfile` — convert a nullable API DTO into a valid domain model.

Tests verify missing optional/required fields, fallback values, and valid conversions.

## Day 3 — Collections and Sequences

**Status: implemented.**

Topics: `map`, `filter`, `mapNotNull`, `associate`, `groupBy`, `fold`, `any`, `all`, `Sequence`.

Exercises:
- [x] `activeOrders` — `filter`.
- [x] `orderIds` — `map`.
- [x] `couponCodes` — `mapNotNull`.
- [x] `itemNames` — `flatMap`.
- [x] `ordersById` — `associate`.
- [x] `ordersByCustomer` — `groupBy`.
- [x] `totalAmount` — `fold`.
- [x] `hasCancelled` — `any`.
- [x] `allFulfilled` — `all`.
- [x] `customerSummaries` — Sequence pipeline (`asSequence()`).
- [x] `firstPaidAmount` — lazy evaluation on `Sequence`.

Interview targets: List vs Sequence, `map` vs `flatMap`, `fold`, lazy evaluation.

## Day 4 — Data Classes and Sealed Types

**Status: implemented.**

Topics: `data class`, `copy`, `==` vs `===`, `sealed class`, `sealed interface`, exhaustive `when`.

Exercises:
- [x] `deactivate` — `copy`.
- [x] `withEmail` — copy one property.
- [x] `withCity` — nested copy.
- [x] `replacedIfChanged` — `==` vs `===`.
- [x] `isLoading` — `when` on sealed result.
- [x] `dataOrNull` — extract success data.
- [x] `requireData` — error on non-success.
- [x] `mapData` — exhaustive `when`.
- [x] `recoverError` — turn error into success.
- [x] `fold` — collapse sealed states.
- [x] `toScreenState` — map `Loading` / `Success` / `Error` to UI states.

Tests verify equality, copying, state conversion, and expected states.

## Day 5 — Higher-Order Functions and Extensions

**Status: implemented.**

Topics: lambdas, function parameters/references, extension functions, higher-order functions, basic `inline`.

Exercises:
- [x] `keepIf` — lambda parameter.
- [x] `transformEach` — transform lambda.
- [x] `fieldNames` — function reference.
- [x] `startsWith` — returning a function.
- [x] `isValidEmail` — extension function.
- [x] `secondOrNull` — generic extension.
- [x] `allOf` — composing validators.
- [x] `retryUntil` — retry condition.
- [x] `buildText` — lambda with receiver.
- [x] `runCatchingOr` — `inline`.
- [x] `castOrNull` — `inline reified`.

---

# Week 2 — Coroutines Deep Dive

**Status: implemented.** Days 6–10, 43 exercises, 137 tests passing.

## Day 6 — Suspend Functions

**Status: implemented.**

Topics: `suspend`, suspension vs blocking, coroutine context.

Key concept: **`suspend` does not mean background thread.**

Exercises:
- [x] `delayedValue` — `delay` vs blocking.
- [x] `fetchAfterDelay` — delayed load.
- [x] `loadUser` — load from a fake data source, throw if missing.
- [x] `loadUserOrNull` — nullable load.
- [x] `loadUserName` — calling suspend from suspend.
- [x] `loadUsers` — sequential suspend calls.
- [x] `loadUserOrFallback` — recover from failure, not from cancellation.
- [x] `mapUser` — suspend lambda.
- [x] `currentCoroutineName` — read coroutine context.
- [x] `withName` — change context with `withContext`.

Tests verify results, errors, and suspension using coroutine test tools.

## Day 7 — `launch` vs `async`

**Status: implemented.**

Topics: `launch`, `async`, `await`, `coroutineScope`, structured concurrency.

Exercises:
- [x] `loadDashboardSequential` — User then Orders, delays add up.
- [x] `loadDashboardConcurrent` — overlap independent work; child failure cancels siblings.
- [x] `loadDashboardAwaitOrdersFirst` — `async` starts immediately even if awaited later.
- [x] `loadUserAndLog` — `launch` for a fire-and-forget side effect.
- [x] `logAll` — `coroutineScope` waits for children.
- [x] `loadUsersConcurrent` — concurrent list, original order preserved.
- [x] `loadFullDashboard` — sequential user load, then concurrent orders and profile.

Interview targets: `launch` vs `async`, structured concurrency, child failure.

## Day 8 — Cancellation

**Status: implemented.**

Topics: `Job`, `cancel`, `isActive`, `ensureActive`, `CancellationException`, cooperative cancellation.

Exercises:
- [x] `delayedValue` — `delay` is a cancellation point.
- [x] `scanDocuments` — cancel stops remaining work.
- [x] `searchDocuments` — cancellable search.
- [x] `runSteps` — `ensureActive` between CPU work.
- [x] `ticksCompleted` — `isActive` and partial results.
- [x] `searchOrEmpty` — do not swallow `CancellationException`.
- [x] `searchWithTimeout` — timeout returns null instead of hanging.
- [x] `searchThenNotify` — `NonCancellable` cleanup after cancel.
- [x] `searchInBackground` — cancel a returned `Job`.

Tests verify cancellation actually prevents remaining work.

## Day 9 — Exception Handling and Supervision

**Status: implemented.**

Topics: exception propagation, `coroutineScope`, `supervisorScope`, `SupervisorJob`.

Exercises:
- [x] `loadUser` — exception propagates from a missing user.
- [x] `loadHomeSequential` — User, Messages, Weather in order.
- [x] `loadHomeStrict` — `coroutineScope` cancels siblings on failure.
- [x] `loadHomeOrNull` — catching does not supervise; siblings still cancel.
- [x] `loadHomeAllowWeatherFailure` — `supervisorScope` recovers weather failure.
- [x] `loadHomeAwaitWeatherFirst` — failing `await` still cancels remaining children.
- [x] `startIndependentLoads` — `SupervisorJob` and `CoroutineExceptionHandler`.
- [x] `weatherDeferred` — `async` holds the exception until `await`.

Tests compare normal structured-concurrency failure with supervised behaviour.

## Day 10 — Dispatchers and Threading

**Status: implemented.**

Topics: Main, IO, Default, `withContext`, dispatcher injection.

Exercises:
- [x] `currentDispatcher` — dispatcher lives in coroutine context.
- [x] `runOn` — `withContext` switches dispatcher, then restores the previous one.
- [x] `fetchUser` — network work uses IO.
- [x] `readCachedUser` — database reads use IO.
- [x] `cacheUser` — database writes use IO.
- [x] `summarizeUser` — CPU work uses Default.
- [x] `fetchThenRender` — network then UI (`Main`).
- [x] `loadAccount` — cache-first load.
- [x] `loadAndRender` — full pipeline: cache/network, CPU summary, then UI.

Interview targets: IO vs Default, why inject dispatchers, main-thread blocking.

---

# Week 3 — Flow Mastery

This is the highest-priority week.

**Status: Days 11–12 implemented.** Days 13–15 are not started.

## Day 11 — Flow Fundamentals

**Status: implemented.**

Topics: cold Flow, `flow {}`, `emit`, `collect`, `map`, `filter`.

Exercises:
- [x] `emitEach` — cold `flow { }` + `emit`.
- [x] `emitEachDelayed` — `delay` before each emission; cancel stops the rest.
- [x] `collectAll` — `collect` into a list.
- [x] `observeOrders` — emit every store snapshot; work starts only on collect.
- [x] `orderIds` — `map`.
- [x] `paidOrders` — `filter`.
- [x] `paidAmounts` — `filter` then `map`.
- [x] `loadOrdersOnce` — one-shot `suspend` counterpart of observation.
- [x] `observePaidOrders` — `map` over `observeOrders`.

Tests verify emissions, order, transformations, cancellation, and cold-flow collection behaviour.

Interview targets: Flow vs suspend function, why Flow is cold, when work actually starts.

## Day 12 — Flow Operators

**Status: implemented.**

Topics: `debounce`, `distinctUntilChanged`, `combine`, `zip`, `flatMapLatest`, `flatMapConcat`, `collectLatest`.

Practice: search while the user types `i → ip → iph → ipho → iphone`.

Exercises:
- [x] `debounceQueries` — `debounce`; rapid keystrokes collapse to the last query.
- [x] `distinctQueries` — `distinctUntilChanged`; consecutive duplicates dropped.
- [x] `combineQueryAndCategory` — `combine`; emit whenever either side changes.
- [x] `zipQueryAndPage` — `zip`; pair 1-to-1, do not skip to the latest unpaired value.
- [x] `searchLatest` — `flatMapLatest`; a new query cancels the in-flight search.
- [x] `searchAllInOrder` — `flatMapConcat`; wait for the previous search, do not cancel.
- [x] `collectLatestInto` — `collectLatest`; a new result cancels previous processing.
- [x] `searchWhileTyping` — debounce, then distinctUntilChanged, skip blanks, then `flatMapLatest`.

Tests verify debounce, duplicate suppression, combine vs zip, latest-search cancellation, concat ordering, and the composed typing pipeline.

Interview targets: `combine` vs `zip`, `flatMapLatest` vs `flatMapConcat`, `collectLatest` vs `collect`, why search uses debounce then distinctUntilChanged then `flatMapLatest`.

## Day 13 — StateFlow
Topics: hot state, initial/current value, `MutableStateFlow`, `asStateFlow`, `update`, `stateIn`.

Practice:

```kotlin
data class UiState(
    val loading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null
)
```

Implement Initial → Loading → Success/Error.

Tests verify state transitions and that a new collector can receive current state.

Interview targets: why StateFlow requires an initial value, why it fits UI state, `stateIn()`.

## Day 14 — SharedFlow
Topics: broadcast streams, `replay`, buffering, multiple collectors, `shareIn`.

Practice one-time-style application events such as Saved and ShowError.

Tests investigate:
- `replay = 0`
- `replay = 1`
- emission before/after collector
- late collector
- multiple collectors

## Day 15 — Channel vs SharedFlow vs StateFlow
Topics: `Channel`, `send`, `receive`, `receiveAsFlow`, queue vs broadcast vs state semantics.

Run the same experiments against StateFlow, SharedFlow and Channel:
- value before collector
- value after collector
- two collectors
- collector reconnects
- many rapid values
- latest/current-value requirement

Fill this yourself after the experiments:

| Behaviour | StateFlow | SharedFlow | Channel |
|---|---|---|---|
| Represents current state | ? | ? | ? |
| New collector gets current value | ? | ? | ? |
| Broadcast semantics | ? | ? | ? |
| Queue-like communication | ? | ? | ? |
| Configurable replay | ? | ? | ? |
| Typical UI-state choice | ? | ? | ? |

---

# Week 4 — Senior Android Architecture

**Status: not started.** No `week4/` source or tests yet.

## Day 16 — Repository Pattern
Architecture:

```text
             Repository
             /        \
   RemoteDataSource  LocalDataSource
          ↓                 ↓
         API             Database
```

Practice:

```kotlin
interface UserRepository {
    fun observeUsers(): Flow<List<User>>
    suspend fun refresh()
}
```

Tests verify cached observation, refresh, local updates and remote failure.

Interview targets: repository responsibilities, source of truth, repository vs data source, when UseCase is useful.

## Day 17 — State Management
Architecture:

```text
User Action → ViewModel → Repository → Result → UiState → UI
```

Practice Initial, Loading, Success, Error and Retry states.

Tests verify all transitions and that mutable state isn't publicly exposed.

## Day 18 — Race Conditions and Search
Problem: slow `cat` request starts, faster `cats` request finishes first, then old `cat` response arrives.

Practice solving stale-result problems using Flow/cancellation concepts rather than fragile flags.

Tests verify:
- latest query wins
- old request cannot replace new results
- fast typing doesn't create excessive requests
- duplicates don't cause unnecessary work

## Day 19 — Offline-First Architecture
Architecture:

```text
API → Repository → Database → Flow → UI
```

Treat the database as source of truth.

Tests verify:
1. Cached data is available immediately.
2. API success updates database.
3. Database changes emit through Flow.
4. API failure doesn't destroy usable cache.
5. New server data replaces stale data correctly.

## Day 20 — Final Challenge: Task Manager

Model:

```kotlin
data class Task(
    val id: String,
    val title: String,
    val completed: Boolean,
    val updatedAt: Long
)
```

Requirements:
- Load tasks
- Add task
- Complete task
- Search
- Refresh
- Offline/cache support
- Loading/error state
- One-time success notification

Behavioural requirements:
- UI can always obtain current task state.
- Save success can produce a notification once.
- Search doesn't run on every keystroke.
- Old search responses cannot replace newer results.
- Cached tasks remain available if refresh fails.

**The exercise should not tell you which Flow type to use. You choose it and explain why.**

---

# Suggested Repository Structure

```text
src/
├── main/kotlin/practice/
│   ├── week1/   # Days 1–5  language depth     — implemented
│   ├── week2/   # Days 6–10 coroutines         — implemented
│   ├── week3/   # Days 11–15 Flow              — Days 11–12 implemented
│   └── week4/   # Days 16–20 architecture      — not started
└── test/kotlin/practice/
    ├── week1/   # implemented
    ├── week2/   # implemented
    ├── week3/   # Days 11–12 implemented
    └── week4/   # not started
```

# Testing Philosophy

## Level 1 — Functional
Does the function return the correct result?

## Level 2 — Behaviour
Does cancellation stop work? Does StateFlow retain current state? Are operations really concurrent? Does latest search win?

## Level 3 — Design
Is mutable state hidden? Can stale work overwrite new work? Is the repository/source-of-truth boundary correct?

Level 2 and Level 3 are the most important for senior-level learning.

# Daily Routine

- **15–20 min:** concept
- **30–40 min:** coding and tests
- **15 min:** explain your solution aloud
- **10 min:** answer 2–3 interview questions without Google

# Core Learning Rule

Early exercises may say:

> Implement this using `apply`.

Later exercises should describe behaviour instead:

> The UI must always have access to the latest state.

Then **you** decide whether Flow, StateFlow, SharedFlow or Channel fits.

# Completion Checklist

Week 1, Week 2, and Days 11–12 production code and tests are done. Remaining items are Days 13–20, plus interview-style explanation of the implemented work.

- [x] `let` vs `run` vs `apply` vs `also`
- [x] Null-safety decisions
- [x] List vs Sequence
- [x] Data/sealed type design
- [x] Higher-order functions
- [x] What `suspend` actually means
- [x] `launch` vs `async`
- [x] Structured concurrency
- [x] Cancellation
- [x] `coroutineScope` vs `supervisorScope`
- [x] IO vs Default
- [ ] Cold Flow vs hot stream
- [x] Important Flow operators
- [ ] StateFlow behaviour
- [ ] SharedFlow/replay behaviour
- [ ] Channel behaviour
- [ ] StateFlow vs SharedFlow vs Channel
- [ ] Repository responsibilities
- [ ] UI-state ownership
- [ ] Race-condition handling
- [ ] Offline-first/source-of-truth design
- [x] Coroutine testing (Flow testing still pending for Days 13–15)
- [ ] Defending architecture choices in an interview

# Definition of Done

A topic is complete only when:

1. All tests pass.
2. You can explain the implementation without reading it.
3. You can explain **why** you chose it.
4. You can name at least one alternative.
5. You can explain a situation where your chosen approach would be inappropriate.

That is the progression from **knowing an API** to **senior engineering understanding**.

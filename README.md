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

# Week 1 — Kotlin Language Depth

## Day 1 — Scope Functions
Topics: `let`, `run`, `apply`, `also`, `with`, `this` vs `it`, receiver vs lambda return value.

Practice:
- Normalize nullable email with `let`.
- Configure an object with `apply`.
- Add side effects/logging with `also`.
- Calculate a value with `run`.
- Work with an existing receiver using `with`.

Tests verify null handling, return values, side effects, and same-object identity.

Interview targets:
- `let` vs `run`
- `apply` vs `also`
- Which functions return the receiver?
- When do scope functions hurt readability?

## Day 2 — Null Safety
Topics: `?.`, `?:`, nullable types, `requireNotNull`, `checkNotNull`, avoiding `!!`.

Practice: convert nullable API DTOs into valid domain models.

Tests verify missing optional/required fields, fallback values, and valid conversions.

## Day 3 — Collections and Sequences
Topics: `map`, `filter`, `mapNotNull`, `associate`, `groupBy`, `fold`, `any`, `all`, `Sequence`.

Practice: process orders, remove cancelled items, group by customer, calculate totals and summaries.

Experiment with List chains versus `asSequence()`.

Interview targets: List vs Sequence, `map` vs `flatMap`, `fold`, lazy evaluation.

## Day 4 — Data Classes and Sealed Types
Topics: `data class`, `copy`, `==` vs `===`, `sealed class`, `sealed interface`, exhaustive `when`.

Practice: model `Loading`, `Success`, and `Error` results.

Tests verify equality, copying, state conversion, and expected states.

## Day 5 — Higher-Order Functions and Extensions
Topics: lambdas, function parameters/references, extension functions, higher-order functions, basic `inline`.

Practice: reusable validators, transformations, filters, and retry conditions.

---

# Week 2 — Coroutines Deep Dive

## Day 6 — Suspend Functions
Topics: `suspend`, suspension vs blocking, coroutine context.

Practice: implement a delayed `loadUser(id)` using a fake data source.

Key concept: **`suspend` does not mean background thread.**

Tests verify results, errors, and suspension using coroutine test tools.

## Day 7 — `launch` vs `async`
Topics: `launch`, `async`, `await`, `coroutineScope`, structured concurrency.

Practice: load User and Orders for a dashboard sequentially, then concurrently.

Tests verify both results and that independent work can execute concurrently.

Interview targets: `launch` vs `async`, structured concurrency, child failure.

## Day 8 — Cancellation
Topics: `Job`, `cancel`, `isActive`, `ensureActive`, `CancellationException`, cooperative cancellation.

Practice: build cancellable long-running search/calculation work.

Tests verify cancellation actually prevents remaining work.

## Day 9 — Exception Handling and Supervision
Topics: exception propagation, `coroutineScope`, `supervisorScope`, `SupervisorJob`.

Practice: dashboard loads User, Messages and Weather; deliberately make one child fail.

Tests compare normal structured-concurrency failure with supervised behaviour.

## Day 10 — Dispatchers and Threading
Topics: Main, IO, Default, `withContext`, dispatcher injection.

Practice: classify network, database, CPU-heavy work, data processing and UI work.

Interview targets: IO vs Default, why inject dispatchers, main-thread blocking.

---

# Week 3 — Flow Mastery

This is the highest-priority week.

## Day 11 — Flow Fundamentals
Topics: cold Flow, `flow {}`, `emit`, `collect`, `map`, `filter`.

Practice: implement `observeOrders(): Flow<List<Order>>`.

Tests verify emissions, order, transformations, and cold-flow collection behaviour.

Interview target: Flow vs suspend function.

## Day 12 — Flow Operators
Topics: `debounce`, `distinctUntilChanged`, `combine`, `zip`, `flatMapLatest`, `collectLatest`.

Practice: search while the user types `i → ip → iph → ipho → iphone`.

Tests verify debounce, duplicate suppression, latest-search behaviour and cancellation.

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
│   ├── week1/   # Days 1–5  language depth
│   ├── week2/   # Days 6–10 coroutines
│   ├── week3/   # Days 11–15 Flow
│   └── week4/   # Days 16–20 architecture
└── test/kotlin/practice/
    ├── week1/
    ├── week2/
    ├── week3/
    └── week4/
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

- [ ] `let` vs `run` vs `apply` vs `also`
- [ ] Null-safety decisions
- [ ] List vs Sequence
- [ ] Data/sealed type design
- [ ] Higher-order functions
- [ ] What `suspend` actually means
- [ ] `launch` vs `async`
- [ ] Structured concurrency
- [ ] Cancellation
- [ ] `coroutineScope` vs `supervisorScope`
- [ ] IO vs Default
- [ ] Cold Flow vs hot stream
- [ ] Important Flow operators
- [ ] StateFlow behaviour
- [ ] SharedFlow/replay behaviour
- [ ] Channel behaviour
- [ ] StateFlow vs SharedFlow vs Channel
- [ ] Repository responsibilities
- [ ] UI-state ownership
- [ ] Race-condition handling
- [ ] Offline-first/source-of-truth design
- [ ] Coroutine/Flow testing
- [ ] Defending architecture choices in an interview

# Definition of Done

A topic is complete only when:

1. All tests pass.
2. You can explain the implementation without reading it.
3. You can explain **why** you chose it.
4. You can name at least one alternative.
5. You can explain a situation where your chosen approach would be inappropriate.

That is the progression from **knowing an API** to **senior engineering understanding**.

# Android follow-on — Task List

This package is the **same architecture as Week 4**, named the way an Android interview expects.

The Gradle project stays JVM-only so you keep `runTest` / virtual time. When you open a real app module, replace the stand-ins in the table. Do not rewrite the rules.

## Mapping

| This file | AndroidX / library |
|---|---|
| [TaskListViewModel](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | `androidx.lifecycle.ViewModel` + `viewModelScope` |
| `scope` constructor arg | `viewModelScope` (cancelled on `onCleared`) |
| [TaskDao](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | Room `@Dao`, `Flow` / `StateFlow` from `@Query` |
| [InMemoryTaskDao](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | Room database + DAO implementation |
| [TaskApi](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | Retrofit interface, OkHttp timeouts/auth |
| [FakeTaskApi](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | MockWebServer or a fake in instrumented tests |
| [TaskListRepository](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | Hilt `@Singleton` repository |
| [TaskListAction](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | UI events: clicks, IME, pull-to-refresh |
| [TaskListScreenSpec](../../src/main/kotlin/practice/androidapp/TaskListApp.kt) | `@Composable TaskListScreen(state, onAction)` |
| `TaskUiState` (Week 4) | `collectAsStateWithLifecycle()` |
| `TaskEvent` SharedFlow | snackbar / one-shot; `replay = 0` |

## Compose rules to say in an interview

1. **Hoist state.** The screen is a function of `TaskUiState`. Leaves do not hold the ViewModel.
2. **Lifecycle.** Collect with `collectAsStateWithLifecycle`, not raw `collect` in `LaunchedEffect`, unless you are collecting one-shot events.
3. **Process death vs rotation.** Rotation: ViewModel survives, StateFlow still has `.value`. Process death: you need `SavedStateHandle` (query text, scroll) plus the Room cache (the list).
4. **Side effects.** `LaunchedEffect(Unit)` to start a one-time refresh is easy to get wrong (refires). Prefer `viewModel.refresh()` from init / `SharingStarted`.
5. **Stability.** Pass immutable `TaskUiState` and lambdas remembered in the ViewModel (`onAction`), not lambdas allocated in the composable on every keystroke if you can avoid it.

## What to build next in a real module

Keep this repository’s Week 4 tests. Add an Android app that:

1. Puts `TaskListViewModel` in `:app` with Hilt.
2. Uses Room with a real `@Query("SELECT * FROM tasks") fun observe(): Flow<List<TaskEntity>>`.
3. Uses Retrofit for `GET/PUT /tasks`.
4. Uses `NavHost` for list + add.
5. Uses `Paging 3` only if the list is large; do not add it for 20 rows.

Until that app exists, run:

```text
./gradlew test --tests "practice.androidapp.*"
```

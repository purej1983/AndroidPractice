# Spoken interview quiz — Weeks 2 and 3

Use this **out loud**, no IDE. 10 minutes: pick 3 questions, answer, then check the notes.

A senior answer is not an API name. It is **what it does, why you chose it, and when it is the wrong tool**.

---

## How to drill

1. Cover the notes.
2. Speak for 60–90 seconds.
3. Uncover. If you missed a "must say", repeat tomorrow.

---

## Coroutines (Week 2)

### 1. What does `suspend` actually mean?

**Must say:** Suspension is a pause point, not a thread. The dispatcher in context picks the thread. A suspend function can run on Main the whole time.

**Trap:** "`suspend` means background work."

### 2. `launch` vs `async`?

**Must say:** `launch` is fire-and-forget: a `Job`, no result. `async` is a `Deferred` you `await`. Start `async` immediately if you want overlap; `await` does not start the work.

**When wrong:** `async` for a side effect you never await (lost exceptions). `launch` when the caller needs a value.

### 3. What is structured concurrency?

**Must say:** A parent does not complete until children complete. Cancelling the parent cancels children. A child failure in `coroutineScope` cancels siblings.

**Must say:** That is why you do not use `GlobalScope`.

### 4. Why must you not swallow `CancellationException`?

**Must say:** Cancellation is cooperative. `delay`, `await`, and `ensureActive` throw `CancellationException`. Catching `Exception` and ignoring it turns cancel into "keep running". Re-throw it. Cleanup goes in `finally` or `NonCancellable`.

**Interview story:** "A `catch (e: Exception)` around a repository call marked a cancelled screen as a load error."

### 5. `coroutineScope` vs `supervisorScope`?

**Must say:** `coroutineScope`: one child failure cancels the others and fails the scope. `supervisorScope`: siblings keep running; you handle each `await` / child yourself. `SupervisorJob` is the Job version for a long-lived scope.

**When wrong:** Supervising a pipeline that must roll back together (payment + inventory). Not supervising independent widgets on a home screen.

**Trap:** `try/catch` around `coroutineScope` does **not** supervise. Siblings still cancel; you only catch after the scope has failed.

### 6. IO vs Default vs Main?

**Must say:** IO: blocking disk/network. Default: CPU. Main: UI only. `withContext` switches, then restores. Inject dispatchers so tests use `TestDispatcher`.

**Trap:** `Dispatchers.IO` for sorting a large list. `Dispatchers.Default` for Room. Hardcoding dispatchers in a ViewModel.

---

## Flow (Week 3)

### 7. Flow vs a suspend function?

**Must say:** Suspend is **one shot**. Flow is a **stream**. Cold Flow: work starts on collect, restarts per collector. Use suspend for "load user once". Use Flow for "observe the table".

### 8. Why is Flow cold?

**Must say:** The producer lambda runs when you collect. Two collectors mean two producers. That is why a repository `observeUsers()` is cheap to return and expensive to collect.

### 9. `combine` vs `zip`?

**Must say:** `combine`: after every input has emitted once, emit whenever **any** side emits, using the latest of each. `zip`: pair **1-to-1**, extra values wait. Search form (query + category) is combine. Pairing request N with response N is zip.

### 10. `flatMapLatest` vs `flatMapConcat` vs `collectLatest`?

**Must say:** `flatMapLatest`: new inner cancels the previous (latest search wins). `flatMapConcat`: wait for the previous inner; no cancel. `collectLatest`: same cancel idea on the **collector** side, not mapping to a new Flow.

**When wrong:** Concat for typeahead (stale `cat` overwrites `cats`). Latest for "process every event exactly once".

### 11. Say the search-while-typing pipeline.

**Must say, in order:** debounce → distinctUntilChanged → skip blanks → `flatMapLatest(search)`.

**Why that order:** Debounce first or you start a search per key. Distinct next or a settled duplicate refetches. Skip blanks or you hammer the API with `""`. Latest last or a slow old response wins.

### 12. StateFlow vs SharedFlow vs Channel?

| Say this | StateFlow | SharedFlow (`replay = 0`) | Channel |
|---|---|---|---|
| What it is | Current state | Broadcast events | Queue |
| New collector | Gets **now**, not history | Gets nothing past | Gets unconsumed items |
| Broadcast | Yes | Yes | No — one receiver |
| UI state | Yes | No | No |
| Snackbar / Saved | No (replay + conflation) | Yes | Only if one consumer |

**Must say:** StateFlow always has `.value` and conflates equals. SharedFlow has no `.value`; `replay = 0` is why a snackbar does not come back after rotation. Channel is work distribution, not a screen model.

### 13. `stateIn` Eagerly vs Lazily?

**Must say:** Eagerly starts without a collector (good for a ViewModel that must be warm). Lazily starts on first collector. Both share **one** upstream among collectors. StateFlow never completes; do not `toList()` it.

---

## Architecture glue (use after Week 4)

### 14. Who is the source of truth?

**Must say:** The database (or equivalent local snapshot), not the last network response. UI collects local Flow. Refresh **writes** remote into local. API failure must not wipe cache.

### 15. Why not a "request id" flag for search races?

**Must say:** You can, but then you still waste work and you can forget to ignore a stale callback. `flatMapLatest` **cancels** the stale call. Prefer cancel over "ignore the result".

---

## 3-night rotation

| Night | Questions |
|---|---|
| A | 1, 4, 12 |
| B | 3, 5, 11 |
| C | 2, 6, 10, 14 |

If you cannot answer 4, 11, and 12 without the notes, do not start new DSA yet. Those three show up in almost every senior Android loop.

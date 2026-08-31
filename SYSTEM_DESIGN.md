# Mobile system design drills

Senior loops ask you to **design an app**, not recite operators. Timebox **30–40 minutes** each. Speak. Draw boxes. Use Week 2–4 language (source of truth, cancel, Flow type, process death).

For every drill, force yourself to pick:

1. Source of truth
2. Flow type per surface (state / event / queue)
3. How stale work is cancelled
4. Error UX vs empty vs offline cache
5. Config change vs process death

Sample talking points are **after** the prompt. Cover them, then check.

---

## Drill 1 — Offline-first feed / timeline

**Prompt:** Instagram-like home feed. Paginated. Works in the subway. Pull-to-refresh. New posts appear later without jumping the scroll position. Conflicts: you liked a post offline, the server says the post was deleted.

**Must decide:** DB as SoT; `observeFeed(): Flow` from Room; refresh writes pages into DB; `RemoteMediator`-style paging vs manual `nextKey`; like is a local row + outbox; deleted server row must `replace` not merge-only; WorkManager for outbox if the process dies.

**Flow types:** StateFlow for `FeedUiState` (items, loading, endOfPagination). SharedFlow replay 0 for "post deleted" snackbar. Channel only if a single writer drains an upload queue.

**Process death:** Room survives. Scroll position needs SavedStateHandle. In-flight refresh does not.

---

## Drill 2 — Search-as-you-type (scale the Day 12/18 pipeline)

**Prompt:** Play Store search. Suggest, ranked results, abort in-flight, empty query, rate limits, analytics.

**Must decide:** debounce → distinctUntilChanged → skip blank → `flatMapLatest`. Suggest and results can be two APIs; `combine` latest query with filters. Do not `flatMapConcat`. Cache last successful results so a failed newer query does not blank the list unless you intend that. Analytics: Channel or fire-and-forget `launch` on IO, never block UI state.

**Scale:** OkHttp cancel via coroutine cancel. Server-side ranking is not your problem; **stale response is**. LRU for recent queries (see DSA cache). Rate limiter if the API bills per request.

---

## Drill 3 — Chat / messaging

**Prompt:** 1:1 chat. Send, fail, retry. Message order. Push when backgrounded. "Delivered" / "read". Duplicate sends.

**Must decide:** Room messages table keyed by client `uuid` (idempotent). Status: sending → sent → delivered. Observe `Flow<List<Message>>` ordered by `createdAt`. Send: upsert local first (offline), then API; SharedFlow for "failed to send" on the composer. Incoming: FCM writes DB, UI collects DB — do not push UI from the FCM service. Ordering: server timestamp + local clock fallback; never reorder a sending row under the user's fingers.

**Sync:** WorkManager for unsent outbox. Do not use a forever-running coroutine in Application.

**Cancellation:** leaving the chat screen must not cancel an in-flight send (use a scope wider than the ViewModel, or WorkManager).

---

## Drill 4 — Auth + session

**Prompt:** Login, token refresh, 401, logout everywhere, biometric unlock, multi-process (FCM).

**Must decide:** Encrypted storage / DataStore for tokens, **not** StateFlow alone. Authenticator interceptor: one refresh at a time (`Mutex` / single-flight). 401 during refresh → logout. Logout: cancel scopes, clear DB, navigate; SharedFlow `LoggedOut` replay 0 with a session StateFlow so a late screen still sees logged-out **state**.

**Process death:** tokens on disk. In-memory "isRefreshing" does not survive; refresh must be safe to restart.

**Trap:** putting the access token in a singleton without a mutex. Two parallel 401s, two refreshes, one invalidated token.

---

## Drill 5 — Downloads / sync engine

**Prompt:** Podcast app. Download episodes on Wi-Fi, pause, resume, constraints, backoff, "only 3 at a time".

**Must decide:** WorkManager is the process-death answer; coroutines are the in-process answer. Unique work per episode id. Constraints: `NetworkType.UNMETERED`. Backoff on 5xx. Concurrency: `maxParallel` via a queue (Channel) or WM `existingWorkPolicy`. Progress: DB row `bytes` observed as Flow — not a callback into Compose.

**When coroutines instead:** short sync while the app is open (refresh). When WM: anything that must finish after swipe-away.

**Flow types:** StateFlow of `DownloadStatus` per id. Channel of `DownloadJob` if a single worker loop takes work.

---

## Drill 6 — Image-heavy gallery

**Prompt:** Camera roll / Unsplash grid. Fast scroll, memory, disk cache, eviction, jank, content descriptions.

**Must decide:** Coil/Glide LRU memory + disk. Do not decode full-res on the main thread (`Dispatchers.Default` for CPU, IO for disk). Compose: `LazyGrid` + stable keys (`image.id`). Recycled requests must cancel (`flatMapLatest` per cell / Coil's own cancel). Downsample to view size. Baseline profile for scroll jank if they push performance.

**Process death:** disk cache survives; memory LRU does not. First frame after death can show placeholders then disk.

**Accessibility:** contentDescription from metadata, not empty.

---

## How to run a drill

```text
0:00  Restate users, scale, offline, failure
5:00  Boxes: UI / ViewModel / Repository / Local / Remote / Workers
15:00 Source of truth + Flow types + cancel
25:00 Process death, pagination, security
35:00 Trade-offs, what you would not build in v1
```

If you skip Flow-type choice, you failed the drill even if the boxes look good.

---

## Two full mocks (Week D)

### Mock A — timed DSA (45 min)

Pick **two** from [src/main/kotlin/practice/dsa/DsaPractice.kt](src/main/kotlin/practice/dsa/DsaPractice.kt):

1. LRU cache (talk Coil while you code)
2. Course schedule **or** merge intervals
3. If time: longest substring / rate limiter

Speak complexity. Write Kotlin, not Java. Tests in [src/test/kotlin/practice/dsa/DsaPracticeTest.kt](src/test/kotlin/practice/dsa/DsaPracticeTest.kt) are the hidden cases; try the problem on paper first.

### Mock B — design this screen + data layer (45 min)

"Design Task List with search, offline, and a save snackbar."

You already implemented it: Week 4 Day 20 + `practice.androidapp`. The mock is to **draw it without opening the code**:

- Room SoT, Retrofit writes DB
- StateFlow UI, SharedFlow Saved
- debounce + `flatMapLatest` search
- refresh failure keeps cache
- ViewModel + hoisted Compose
- process death: DB + SavedStateHandle for the query

Then open Day 20 and grade yourself against the tests.

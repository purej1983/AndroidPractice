# Lean DSA — 3 to 4 weeks

You do not need 200 LeetCode problems. You need **pattern fluency in Kotlin** and two timed mocks.

Target: **4–5 problems per week**. Implementations live in [src/main/kotlin/practice/dsa/DsaPractice.kt](src/main/kotlin/practice/dsa/DsaPractice.kt). Hide the file, solve, then run tests.

```text
./gradlew test --tests "practice.dsa.*"
```

## Weekly set

| Week | Problems | Pattern | Android story to say |
|---|---|---|---|
| 1 | LRU get/put/evict; rate limiter window | HashMap + list / queue | Coil memory cache; OkHttp throttle |
| 2 | mergeK; twoSumSorted | Heap; two pointers | Merge paged APIs; pair IDs |
| 3 | longest substring; maxSumOfK | Sliding window | Debounce is time-window, this is index-window |
| 4 | canFinish; shortestPath; merge intervals | BFS / topo / sort+scan | Nav graph cycles; calendar |

Re-do any problem you cannot explain in 90 seconds.

## Mock A (coding)

45 minutes, two problems, no IDE autocomplete if you can. Speak:

- brute force first
- complexity
- Kotlin collections / `ArrayDeque` / `PriorityQueue`
- one test case you would add

## Mock B (screen + data)

See [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md). Draw Task List without opening Week 4.

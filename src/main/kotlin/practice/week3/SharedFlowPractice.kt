package practice.week3

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

/**
 * Day 14 — SharedFlow.
 *
 * Key ideas:
 * - SharedFlow is a **hot** broadcast stream. It does not need an initial
 *   value, and it has no `.value`. Use `replayCache` for replayed items.
 * - `replay = 0` (the default): a new collector does not get past
 *   emissions. That fits one-time UI events (Saved, ShowError, snackbar).
 * - `replay = 1`: a new collector gets the last emission. Similar to
 *   StateFlow, but SharedFlow does **not** conflate equal values.
 * - `extraBufferCapacity` lets **slow collectors** lag without blocking emitters.
 *   It is unused when there are no collectors. Absent collectors only keep the
 *   replay cache, so `replay = 0` drops the value immediately.
 * - `emit` returns immediately when there is no collector. It suspends only
 *   when collectors exist and there is no buffer room. `tryEmit` returns
 *   false in that same case instead of suspending.
 * - Expose `SharedFlow`, keep `MutableSharedFlow` private (`asSharedFlow`).
 * - `shareIn` turns a cold Flow into a SharedFlow. `Eagerly` starts at
 *   once; `Lazily` starts on the first collector.
 * - SharedFlow never completes. Do not call `toList()` on it.
 * - Every collector receives the same emissions (broadcast). That is the
 *   difference from a Channel.
 */
sealed interface UiEvent {
    data object Saved : UiEvent
    data class ShowError(val message: String) : UiEvent
}

data class Note(
    val id: String,
    val title: String
)

/**
 * Cold note feed for tests.
 *
 * [observeNotes] is a cold Flow. [collectCount] proves `shareIn(Eagerly)`
 * starts without a SharedFlow collector, and that two SharedFlow
 * collectors share one upstream instead of replaying it.
 */
class FakeNoteFeed(
    private val notes: List<Note> = emptyList(),
    private val delayMillis: Long = 0L
) {
    private var _collectCount = 0
    private val _readIndexes = mutableListOf<Int>()

    val collectCount: Int get() = _collectCount
    val readIndexes: List<Int> get() = _readIndexes.toList()

    fun observeNotes(): Flow<Note> {
        return flow {
            _collectCount += 1
            for (index in notes.indices) {
                _readIndexes += index
                delay(delayMillis.milliseconds)
                emit(notes[index])
            }
        }
    }
}

/**
 * Exercise 6 — one-time UI events
 *
 * Expose a read-only SharedFlow of [UiEvent] with `replay = 0`.
 * [notifySaved] and [notifyError] emit to that flow.
 *
 * A late collector must not receive a past event (a snackbar does not
 * reappear after rotation). Two Saved events in a row must both be
 * delivered (SharedFlow does not conflate equals).
 *
 * Requirement: keep a private `MutableSharedFlow(replay = 0)` and expose
 * [events] with `asSharedFlow`. Emit with `emit`. Do not add extra buffer
 * capacity. Do not use StateFlow.
 */
class EventNotifier {
    val events: SharedFlow<UiEvent> = TODO()

    suspend fun notifySaved() {
        TODO()
    }

    suspend fun notifyError(message: String) {
        TODO()
    }
}

object SharedFlowPractice {

    /**
     * Exercise 1 — replay = 0
     *
     * Return a MutableSharedFlow with `replay = 0` and no extra buffer.
     * A collector that subscribes after an emission must not receive it.
     * `tryEmit` without a collector must return true and drop the value.
     *
     * Requirement: use `MutableSharedFlow` with `replay = 0`.
     * Do not set `extraBufferCapacity`. Do not use StateFlow.
     */
    fun <T> noReplay(): MutableSharedFlow<T> {
        return MutableSharedFlow(replay =0)
    }

    /**
     * Exercise 2 — asSharedFlow
     *
     * Return a read-only SharedFlow backed by [mutable].
     * Emissions on [mutable] must be visible through the returned flow.
     * The returned type must not be `MutableSharedFlow`.
     *
     * Requirement: use `asSharedFlow`.
     */
    fun <T> readOnly(mutable: MutableSharedFlow<T>): SharedFlow<T> {
        return  mutable.asSharedFlow()
    }

    /**
     * Exercise 3 — replay = 1
     *
     * Return a MutableSharedFlow with `replay = 1`.
     * A late collector must receive the last emission, even if it
     * happened before subscribe. Only the last value is replayed.
     * Equal consecutive values must not be conflated.
     *
     * Requirement: use `MutableSharedFlow` with `replay = 1`.
     * Do not use StateFlow.
     */
    fun <T> replayLast(): MutableSharedFlow<T> {
        return MutableSharedFlow(replay = 1)
    }

    /**
     * Exercise 4 — extra buffer
     *
     * Return a MutableSharedFlow with `replay = 0` and
     * `extraBufferCapacity = [extraBufferCapacity]`.
     *
     * Extra buffer helps a **slow collector**, not a missing one. Without a
     * collector, `tryEmit` still succeeds and the value is dropped. With a
     * collector that is busy, `tryEmit` succeeds while extra buffer has room
     * and returns false when that room is full.
     *
     * Requirement: use `MutableSharedFlow(replay = 0, extraBufferCapacity = extraBufferCapacity)`.
     * Do not set `replay` to 1.
     */
    fun <T> buffered(extraBufferCapacity: Int): MutableSharedFlow<T> {
        TODO()
    }

    /**
     * Exercise 5 — emit
     *
     * Emit [value] to [events]. If there is no collector, return immediately
     * and drop the value. If collectors exist and there is no buffer room,
     * suspend until they receive it.
     *
     * Requirement: use `emit`. Do not use `tryEmit`.
     */
    suspend fun <T> emitEvent(events: MutableSharedFlow<T>, value: T) {
        TODO()
    }

    /**
     * Exercise 7 — shareIn Eagerly
     *
     * Convert cold [upstream] into a hot SharedFlow that starts
     * immediately, even with no collectors. Replay the last [replay]
     * values to new collectors. After [upstream] completes, keep the
     * replay cache and do not complete the SharedFlow.
     *
     * Requirement: use `shareIn` with `SharingStarted.Eagerly`.
     * Do not collect into a `MutableSharedFlow` yourself.
     */
    fun <T> shareEagerly(
        upstream: Flow<T>,
        scope: CoroutineScope,
        replay: Int
    ): SharedFlow<T> {
        TODO()
    }

    /**
     * Exercise 8 — shareIn Lazily
     *
     * Convert cold [upstream] into a hot SharedFlow that starts on the
     * first collector. Later collectors must share that producer.
     * Replay the last [replay] values to new collectors.
     *
     * Requirement: use `shareIn` with `SharingStarted.Lazily`.
     * Do not use `SharingStarted.Eagerly`.
     */
    fun <T> shareLazily(
        upstream: Flow<T>,
        scope: CoroutineScope,
        replay: Int
    ): SharedFlow<T> {
        TODO()
    }
}

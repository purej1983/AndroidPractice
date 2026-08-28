package practice.week3

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Day 15 — Channel vs SharedFlow vs StateFlow.
 *
 * Key ideas:
 * - StateFlow is **state**. It always has a current value, broadcasts to
 *   every collector, conflates equals, and a new collector gets **now**,
 *   not history.
 * - SharedFlow is a **broadcast stream**. No `.value`. `replay = 0` drops
 *   emissions when nobody is listening. Every current collector gets the
 *   same values. Equal values are not conflated.
 * - Channel is a **queue**. Each value is consumed by **exactly one**
 *   receiver. It is not a broadcast. `receiveAsFlow` still shares that
 *   queue: two collectors compete, they do not each get a copy.
 * - Rendezvous (`capacity = 0`): `send` waits for a receiver. `trySend`
 *   fails if nobody is waiting.
 * - Unlimited: `send` never waits. Values sit in the queue until taken.
 * - Conflated: buffer of one, newest wins. Not the same as StateFlow:
 *   no initial value, no `.value`, still one consumer.
 * - `receiveAsFlow` completes when the Channel is closed. SharedFlow and
 *   StateFlow never complete.
 *
 * Run the same experiments on all three, then fill the README table.
 */
data class Ticket(
    val id: String,
    val title: String
)

data class BoardState(
    val title: String = "Inbox",
    val tickets: List<Ticket> = emptyList()
)

sealed interface Toast {
    data class Shown(val text: String) : Toast
}

/**
 * Hot stream used to run the same experiments on StateFlow, SharedFlow,
 * and Channel.
 *
 * [emit] publishes a value.
 * [observe] returns a Flow of values.
 * [latestOrNull] is the current value if this stream is a state holder,
 * otherwise null — even if values are sitting in a queue or replay cache
 * that is not "current UI state" unless the stream is StateFlow.
 *
 * For SharedFlow, [latestOrNull] is the last replayed value, or null when
 * `replay = 0`. For Channel it is always null: a queue is not state.
 */
interface MessageStream<T> {
    suspend fun emit(value: T)
    fun observe(): Flow<T>
    fun latestOrNull(): T?
}

/**
 * Exercise 6 — UI always has current board state
 *
 * The screen must always be able to read the latest [BoardState] without
 * collecting. A new collector (rotation) must receive the current state,
 * not a history of titles. Equal consecutive states must be conflated.
 *
 * Requirement: keep a private `MutableStateFlow` and expose [state] with
 * `asStateFlow`. Update with `update`. Do not use SharedFlow or Channel.
 */
class ScreenStore(initial: BoardState = BoardState()) {
    val mState = MutableStateFlow(initial)
    val state: StateFlow<BoardState> = mState.asStateFlow()

    fun setTitle(title: String) {
        mState.value = mState.value.copy(title = title)
    }

    fun setTickets(tickets: List<Ticket>) {
        mState.value = mState.value.copy(tickets = tickets)
    }
}

/**
 * Exercise 7 — one-time toast events
 *
 * Expose a read-only SharedFlow of [Toast] with `replay = 0`.
 * [show] emits `Toast.Shown(text)`.
 *
 * A late collector must not receive a past toast. Two identical toasts in
 * a row must both be delivered. Two current collectors must both receive
 * the same toast (broadcast).
 *
 * Requirement: keep a private `MutableSharedFlow(replay = 0)` and expose
 * [toasts] with `asSharedFlow`. Emit with `emit`. Do not add extra buffer
 * capacity. Do not use StateFlow or Channel.
 */
class ToastBus {
    val mToasts = MutableSharedFlow<Toast>()
    val toasts: SharedFlow<Toast> = mToasts.asSharedFlow()

    suspend fun show(text: String) {
        mToasts.emit(Toast.Shown(text))
    }
}

/**
 * Exercise 8 — work queue, one consumer per ticket
 *
 * Each submitted [Ticket] must be processed by **exactly one** worker.
 * Two workers must not both receive the same ticket. Submit must not
 * wait for a worker (queue the work). [tickets] is a Flow over the same
 * queue: two collectors compete, they do not each get a copy.
 * [close] completes [tickets].
 *
 * Requirement: use `Channel(Channel.UNLIMITED)` and `receiveAsFlow`.
 * Do not use SharedFlow or StateFlow.
 */
class JobQueue {
    suspend fun submit(ticket: Ticket) {
        TODO()
    }

    suspend fun take(): Ticket {
        TODO()
    }

    fun tickets(): Flow<Ticket> {
        TODO()
    }

    fun close() {
        TODO()
    }
}

object ChannelVsFlowPractice {

    /**
     * Exercise 1 — rendezvous Channel
     *
     * Return a Channel with capacity 0 (rendezvous).
     * `trySend` without a waiting receiver must fail.
     * `send` must suspend until a receiver is ready.
     *
     * Requirement: use `Channel()` or `Channel(Channel.RENDEZVOUS)`.
     * Do not set a positive capacity.
     */
    fun <T> rendezvous(): Channel<T> {
        return  Channel(Channel.RENDEZVOUS)
    }

    /**
     * Exercise 2 — unlimited Channel
     *
     * Return a Channel with unlimited capacity.
     * `trySend` without a receiver must succeed.
     * Values sent before a receiver must stay in the queue, in order.
     *
     * Requirement: use `Channel(Channel.UNLIMITED)`.
     */
    fun <T> unlimited(): Channel<T> {
        return Channel(Channel.UNLIMITED)
    }

    /**
     * Exercise 3 — conflated Channel
     *
     * Return a conflated Channel. `trySend` never fails for lack of a
     * receiver. If two values are sent before a receive, only the last
     * one is kept. This is not StateFlow: there is no initial value and
     * still only one consumer.
     *
     * Requirement: use `Channel(Channel.CONFLATED)`.
     * Do not use StateFlow.
     */
    fun <T> conflated(): Channel<T> {
        return Channel(Channel.CONFLATED)
    }

    /**
     * Exercise 4 — send, receive, receiveAsFlow
     *
     * [sendTo] sends [value] to [channel], suspending if there is no room.
     * [receiveFrom] receives one value, suspending until one is available.
     * [asFlow] exposes the Channel as a Flow. Two collectors must compete
     * for values (not broadcast). Closing the Channel must complete the Flow.
     *
     * Requirement: use `send`, `receive`, and `receiveAsFlow`.
     * Do not use `consumeAsFlow` (that allows only one collector).
     */
    suspend fun <T> sendTo(channel: Channel<T>, value: T) {
        channel.send(value)
    }

    suspend fun <T> receiveFrom(channel: Channel<T>): T {
        return channel.receive()
    }

    fun <T> asFlow(channel: Channel<T>): Flow<T> {
        return channel.receiveAsFlow()
    }

    /**
     * Exercise 5 — the same experiments, three implementations
     *
     * Return a [MessageStream] backed by StateFlow with [initial] as the
     * current value. A new collector must receive [initial] immediately.
     * Equal consecutive values must be conflated. [latestOrNull] is always
     * the current value.
     *
     * Requirement: use `MutableStateFlow`. Do not use SharedFlow or Channel.
     */
    fun <T> stateStream(initial: T): MessageStream<T> {
        return object : MessageStream<T> {
            private val state = MutableStateFlow(initial)
            override suspend fun emit(value: T) {
                state.value = value
            }
            override fun observe(): Flow<T> = state.asStateFlow()
            override fun latestOrNull(): T? = state.value
        }
    }

    /**
     * Return a [MessageStream] backed by SharedFlow with the given [replay].
     * With `replay = 0`, a late collector must not receive past values.
     * With `replay = 1`, a late collector receives only the last value.
     * Equal values must not be conflated. [latestOrNull] is the last
     * replayed value, or null when the replay cache is empty.
     *
     * Requirement: use `MutableSharedFlow(replay = replay)`.
     * Do not use StateFlow or Channel.
     */
    fun <T> sharedStream(replay: Int): MessageStream<T> {
        return object : MessageStream<T> {
            private val events = MutableSharedFlow<T>(replay = replay)
            override suspend fun emit(value: T) {
                events.emit(value)
            }
            override fun observe(): Flow<T> = events.asSharedFlow()
            override fun latestOrNull(): T? = events.replayCache.lastOrNull()
        }
    }

    /**
     * Return a [MessageStream] backed by an unlimited Channel.
     * Emit must not wait for a collector. Values wait in the queue.
     * Two collectors compete; each value is delivered once.
     * [latestOrNull] is always null — a queue is not current state.
     *
     * Requirement: use `Channel(Channel.UNLIMITED)` and `receiveAsFlow`.
     * Do not use StateFlow or SharedFlow.
     */
    fun <T> channelStream(): MessageStream<T> {
        return object : MessageStream<T> {
            private val channel = Channel<T>(Channel.UNLIMITED)
            override suspend fun emit(value: T) {
                channel.send(value)
            }
            override fun observe(): Flow<T> = channel.receiveAsFlow()
            override fun latestOrNull(): T? = null
        }
    }
}

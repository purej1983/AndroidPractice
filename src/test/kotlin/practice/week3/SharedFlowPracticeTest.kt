package practice.week3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SharedFlowPracticeTest {

    private val buyMilk = Note("n1", "Buy milk")
    private val walkDog = Note("n2", "Walk the dog")
    private val diskFull = UiEvent.ShowError("disk full")

    @Test
    fun `noReplay replay cache is empty`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()

        assertEquals(emptyList(), events.replayCache)
    }

    @Test
    fun `noReplay tryEmit without a collector returns true and drops the value`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()

        assertTrue(events.tryEmit(UiEvent.Saved))
        assertEquals(emptyList(), events.replayCache)
    }

    @Test
    fun `noReplay tryEmit without a collector is dropped for a later collector`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        events.tryEmit(UiEvent.Saved)

        val late = mutableListOf<UiEvent>()
        val job = launch { events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), late)
        job.cancelAndJoin()
    }

    @Test
    fun `noReplay collector receives an emission after it subscribes`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { events.collect { emitted += it } }
        testScheduler.runCurrent()
        events.emit(UiEvent.Saved)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `noReplay two collectors both receive a new emission`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        val first = mutableListOf<UiEvent>()
        val second = mutableListOf<UiEvent>()

        val job1 = launch { events.collect { first += it } }
        val job2 = launch { events.collect { second += it } }
        testScheduler.runCurrent()
        events.emit(diskFull)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(diskFull), first)
        assertEquals(listOf<UiEvent>(diskFull), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `noReplay late collector does not receive a past emission`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        val first = mutableListOf<UiEvent>()

        val job1 = launch { events.collect { first += it } }
        testScheduler.runCurrent()
        events.emit(UiEvent.Saved)
        testScheduler.runCurrent()
        job1.cancelAndJoin()

        val late = mutableListOf<UiEvent>()
        val job2 = launch { events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), first)
        assertEquals(emptyList(), late)
        job2.cancelAndJoin()
    }

    @Test
    fun `noReplay does not complete after an emission`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { events.collect { emitted += it } }
        testScheduler.runCurrent()
        events.emit(UiEvent.Saved)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), emitted)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `readOnly later emission is visible`() = runTest {
        val mutable = SharedFlowPractice.noReplay<UiEvent>()
        val events = SharedFlowPractice.readOnly(mutable)
        val emitted = mutableListOf<UiEvent>()

        val job = launch { events.collect { emitted += it } }
        testScheduler.runCurrent()
        mutable.emit(UiEvent.Saved)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `readOnly is not a MutableSharedFlow`() = runTest {
        val mutable = SharedFlowPractice.noReplay<UiEvent>()

        assertTrue(SharedFlowPractice.readOnly(mutable) !is MutableSharedFlow)
    }

    @Test
    fun `readOnly two collectors both receive an update`() = runTest {
        val mutable = SharedFlowPractice.noReplay<String>()
        val events = SharedFlowPractice.readOnly(mutable)
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { events.collect { first += it } }
        val job2 = launch { events.collect { second += it } }
        testScheduler.runCurrent()
        mutable.emit("saved")
        testScheduler.runCurrent()

        assertEquals(listOf("saved"), first)
        assertEquals(listOf("saved"), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `replayLast emit before collector is received`() = runTest {
        val events = SharedFlowPractice.replayLast<UiEvent>()
        events.emit(UiEvent.Saved)

        val late = mutableListOf<UiEvent>()
        val job = launch { events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), late)
        job.cancelAndJoin()
    }

    @Test
    fun `replayLast replay cache holds the last value`() = runTest {
        val events = SharedFlowPractice.replayLast<String>()

        events.emit("saved")
        events.emit("error")

        assertEquals(listOf("error"), events.replayCache)
    }

    @Test
    fun `replayLast late collector receives only the last of two emissions`() = runTest {
        val events = SharedFlowPractice.replayLast<UiEvent>()
        events.emit(UiEvent.Saved)
        events.emit(diskFull)

        val late = mutableListOf<UiEvent>()
        val job = launch { events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(diskFull), late)
        job.cancelAndJoin()
    }

    @Test
    fun `replayLast does not conflate equal consecutive values`() = runTest {
        val events = SharedFlowPractice.replayLast<UiEvent>()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { events.collect { emitted += it } }
        testScheduler.runCurrent()
        events.emit(UiEvent.Saved)
        events.emit(UiEvent.Saved)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved, UiEvent.Saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `replayLast two collectors both receive the replayed value`() = runTest {
        val events = SharedFlowPractice.replayLast<String>()
        events.emit("saved")

        val first = mutableListOf<String>()
        val second = mutableListOf<String>()
        val job1 = launch { events.collect { first += it } }
        val job2 = launch { events.collect { second += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("saved"), first)
        assertEquals(listOf("saved"), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `buffered tryEmit without a collector returns true and still drops the value`() = runTest {
        val events = SharedFlowPractice.buffered<UiEvent>(extraBufferCapacity = 1)

        assertTrue(events.tryEmit(UiEvent.Saved))
        assertEquals(emptyList(), events.replayCache)

        val late = mutableListOf<UiEvent>()
        val job = launch { events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), late)
        job.cancelAndJoin()
    }

    @Test
    fun `buffered tryEmit succeeds while a collector is busy`() = runTest {
        val events = SharedFlowPractice.buffered<String>(extraBufferCapacity = 1)
        val emitted = mutableListOf<String>()

        val job = launch {
            events.collect {
                emitted += it
                delay(1_000)
            }
        }
        testScheduler.runCurrent()
        assertTrue(events.tryEmit("saved"))
        testScheduler.runCurrent()
        assertEquals(listOf("saved"), emitted)

        assertTrue(events.tryEmit("error"))
        testScheduler.runCurrent()
        assertEquals(listOf("saved"), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(listOf("saved", "error"), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `buffered tryEmit returns false when the extra buffer is full`() = runTest {
        val events = SharedFlowPractice.buffered<String>(extraBufferCapacity = 1)
        val emitted = mutableListOf<String>()

        val job = launch {
            events.collect {
                emitted += it
                delay(1_000)
            }
        }
        testScheduler.runCurrent()
        assertTrue(events.tryEmit("saved"))
        testScheduler.runCurrent()
        assertTrue(events.tryEmit("error"))
        assertFalse(events.tryEmit("ignored"))

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()
        assertEquals(listOf("saved", "error"), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `emitEvent delivers to an active collector`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { events.collect { emitted += it } }
        testScheduler.runCurrent()
        SharedFlowPractice.emitEvent(events, diskFull)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(diskFull), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `emitEvent without a collector returns immediately and drops the value`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()

        SharedFlowPractice.emitEvent(events, UiEvent.Saved)

        val late = mutableListOf<UiEvent>()
        val job = launch { events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), late)
        job.cancelAndJoin()
    }

    @Test
    fun `emitEvent suspends while a collector is busy`() = runTest {
        val events = SharedFlowPractice.noReplay<String>()
        val emitted = mutableListOf<String>()

        val collector = launch {
            events.collect {
                emitted += it
                delay(1_000)
            }
        }
        testScheduler.runCurrent()
        SharedFlowPractice.emitEvent(events, "saved")
        testScheduler.runCurrent()
        assertEquals(listOf("saved"), emitted)

        val emitter = launch { SharedFlowPractice.emitEvent(events, "error") }
        testScheduler.runCurrent()
        assertTrue(emitter.isActive)
        assertEquals(listOf("saved"), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        emitter.join()
        assertEquals(listOf("saved", "error"), emitted)
        collector.cancelAndJoin()
    }

    @Test
    fun `emitEvent broadcasts to two collectors`() = runTest {
        val events = SharedFlowPractice.noReplay<UiEvent>()
        val first = mutableListOf<UiEvent>()
        val second = mutableListOf<UiEvent>()

        val job1 = launch { events.collect { first += it } }
        val job2 = launch { events.collect { second += it } }
        testScheduler.runCurrent()
        SharedFlowPractice.emitEvent(events, UiEvent.Saved)
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), first)
        assertEquals(listOf<UiEvent>(UiEvent.Saved), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `EventNotifier notifySaved delivers Saved`() = runTest {
        val notifier = EventNotifier()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { notifier.events.collect { emitted += it } }
        testScheduler.runCurrent()
        notifier.notifySaved()
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `EventNotifier two Saved events are both delivered`() = runTest {
        val notifier = EventNotifier()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { notifier.events.collect { emitted += it } }
        testScheduler.runCurrent()
        notifier.notifySaved()
        notifier.notifySaved()
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved, UiEvent.Saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `EventNotifier late collector after Saved receives nothing`() = runTest {
        val notifier = EventNotifier()
        val first = mutableListOf<UiEvent>()

        val job1 = launch { notifier.events.collect { first += it } }
        testScheduler.runCurrent()
        notifier.notifySaved()
        testScheduler.runCurrent()
        job1.cancelAndJoin()

        val late = mutableListOf<UiEvent>()
        val job2 = launch { notifier.events.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(UiEvent.Saved), first)
        assertEquals(emptyList(), late)
        job2.cancelAndJoin()
    }

    @Test
    fun `EventNotifier notifyError delivers ShowError`() = runTest {
        val notifier = EventNotifier()
        val emitted = mutableListOf<UiEvent>()

        val job = launch { notifier.events.collect { emitted += it } }
        testScheduler.runCurrent()
        notifier.notifyError("disk full")
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(diskFull), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `EventNotifier two collectors both receive ShowError`() = runTest {
        val notifier = EventNotifier()
        val first = mutableListOf<UiEvent>()
        val second = mutableListOf<UiEvent>()

        val job1 = launch { notifier.events.collect { first += it } }
        val job2 = launch { notifier.events.collect { second += it } }
        testScheduler.runCurrent()
        notifier.notifyError("disk full")
        testScheduler.runCurrent()

        assertEquals(listOf<UiEvent>(diskFull), first)
        assertEquals(listOf<UiEvent>(diskFull), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `EventNotifier events is not a MutableSharedFlow`() = runTest {
        val events: SharedFlow<UiEvent> = EventNotifier().events

        assertTrue(events !is MutableSharedFlow)
    }

    @Test
    fun `shareEagerly starts without a SharedFlow collector`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk),
            delayMillis = 1_000
        )

        SharedFlowPractice.shareEagerly(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        assertEquals(0, currentTime)
    }

    @Test
    fun `shareEagerly collector from the start receives emissions with no initial value`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk, walkDog),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareEagerly(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )
        val emitted = mutableListOf<Note>()

        val job = launch { shared.collect { emitted += it } }
        testScheduler.runCurrent()
        assertEquals(emptyList(), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(listOf(buyMilk), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(listOf(buyMilk, walkDog), emitted)
        assertEquals(2_000, currentTime)
        job.cancelAndJoin()
    }

    @Test
    fun `shareEagerly late collector with replay 0 receives nothing`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk, walkDog),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareEagerly(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        val late = mutableListOf<Note>()
        val job = launch { shared.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), late)
        job.cancelAndJoin()
    }

    @Test
    fun `shareEagerly late collector with replay 1 receives the last value`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk, walkDog),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareEagerly(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 1
        )

        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        val late = mutableListOf<Note>()
        val job = launch { shared.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(walkDog), late)
        job.cancelAndJoin()
    }

    @Test
    fun `shareEagerly two collectors share one upstream`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareEagerly(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )
        val first = mutableListOf<Note>()
        val second = mutableListOf<Note>()

        val job1 = launch { shared.collect { first += it } }
        val job2 = launch { shared.collect { second += it } }
        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        assertEquals(listOf(buyMilk), first)
        assertEquals(listOf(buyMilk), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `shareEagerly keeps the last value after upstream completes when replay is 1`() = runTest {
        val feed = FakeNoteFeed(notes = listOf(buyMilk, walkDog))
        val shared = SharedFlowPractice.shareEagerly(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 1
        )

        testScheduler.runCurrent()
        assertEquals(listOf(walkDog), shared.replayCache)

        val late = mutableListOf<Note>()
        val job = launch { shared.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(walkDog), late)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `shareLazily does not start without a collector`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareLazily(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )
        testScheduler.runCurrent()

        assertEquals(0, feed.collectCount)
        assertEquals(emptyList(), feed.readIndexes)
        assertEquals(emptyList(), shared.replayCache)
    }

    @Test
    fun `shareLazily starts when the first collector appears`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareLazily(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )
        val emitted = mutableListOf<Note>()

        val job = launch { shared.collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        assertEquals(emptyList(), emitted)

        testScheduler.advanceTimeBy(1_000)
        testScheduler.runCurrent()
        assertEquals(listOf(buyMilk), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `shareLazily second collector does not restart upstream`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareLazily(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 0
        )

        val job1 = launch { shared.collect { } }
        testScheduler.runCurrent()
        val job2 = launch { shared.collect { } }
        testScheduler.runCurrent()

        assertEquals(1, feed.collectCount)
        assertEquals(listOf(0), feed.readIndexes)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `shareLazily late collector with replay 1 receives the last value`() = runTest {
        val feed = FakeNoteFeed(
            notes = listOf(buyMilk, walkDog),
            delayMillis = 1_000
        )
        val shared = SharedFlowPractice.shareLazily(
            feed.observeNotes(),
            scope = backgroundScope,
            replay = 1
        )

        val first = launch { shared.collect { } }
        testScheduler.advanceTimeBy(2_000)
        testScheduler.runCurrent()

        val late = mutableListOf<Note>()
        val second = launch { shared.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(walkDog), late)
        assertEquals(1, feed.collectCount)
        first.cancelAndJoin()
        second.cancelAndJoin()
    }
}

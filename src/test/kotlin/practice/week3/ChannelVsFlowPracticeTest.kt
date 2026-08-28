package practice.week3

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelVsFlowPracticeTest {

    private val fixLogin = Ticket("t1", "Fix login")
    private val addDarkMode = Ticket("t2", "Add dark mode")
    private val writeTests = Ticket("t3", "Write tests")
    private val saved = Toast.Shown("saved")
    private val diskFull = Toast.Shown("disk full")

    @Test
    fun `rendezvous trySend without a receiver fails`() = runTest {
        val channel = ChannelVsFlowPractice.rendezvous<Ticket>()

        assertTrue(channel.trySend(fixLogin).isFailure)
    }

    @Test
    fun `rendezvous send suspends until a receiver is ready`() = runTest {
        val channel = ChannelVsFlowPractice.rendezvous<Ticket>()

        val sender = launch { ChannelVsFlowPractice.sendTo(channel, fixLogin) }
        testScheduler.runCurrent()

        assertTrue(sender.isActive)

        val received = ChannelVsFlowPractice.receiveFrom(channel)
        sender.join()

        assertEquals(fixLogin, received)
    }

    @Test
    fun `rendezvous receiver waiting then send delivers the value`() = runTest {
        val channel = ChannelVsFlowPractice.rendezvous<Ticket>()
        val received = mutableListOf<Ticket>()

        val receiver = launch { received += ChannelVsFlowPractice.receiveFrom(channel) }
        testScheduler.runCurrent()
        ChannelVsFlowPractice.sendTo(channel, addDarkMode)
        testScheduler.runCurrent()

        assertEquals(listOf(addDarkMode), received)
        receiver.join()
    }

    @Test
    fun `unlimited trySend without a receiver succeeds`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()

        assertTrue(channel.trySend(fixLogin).isSuccess)
    }

    @Test
    fun `unlimited send before receive is queued in order`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()

        ChannelVsFlowPractice.sendTo(channel, fixLogin)
        ChannelVsFlowPractice.sendTo(channel, addDarkMode)

        assertEquals(fixLogin, ChannelVsFlowPractice.receiveFrom(channel))
        assertEquals(addDarkMode, ChannelVsFlowPractice.receiveFrom(channel))
    }

    @Test
    fun `unlimited late receiver gets every queued value`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()
        ChannelVsFlowPractice.sendTo(channel, fixLogin)
        ChannelVsFlowPractice.sendTo(channel, writeTests)

        val late = mutableListOf<Ticket>()
        val job = launch {
            late += ChannelVsFlowPractice.receiveFrom(channel)
            late += ChannelVsFlowPractice.receiveFrom(channel)
        }
        testScheduler.runCurrent()

        assertEquals(listOf(fixLogin, writeTests), late)
        job.join()
    }

    @Test
    fun `conflated trySend without a receiver succeeds`() = runTest {
        val channel = ChannelVsFlowPractice.conflated<Ticket>()

        assertTrue(channel.trySend(fixLogin).isSuccess)
    }

    @Test
    fun `conflated two sends before receive keep only the last value`() = runTest {
        val channel = ChannelVsFlowPractice.conflated<Ticket>()

        ChannelVsFlowPractice.sendTo(channel, fixLogin)
        ChannelVsFlowPractice.sendTo(channel, addDarkMode)

        assertEquals(addDarkMode, ChannelVsFlowPractice.receiveFrom(channel))
        assertTrue(channel.tryReceive().isFailure)
    }

    @Test
    fun `conflated is not broadcast to two receivers`() = runTest {
        val channel = ChannelVsFlowPractice.conflated<Ticket>()
        ChannelVsFlowPractice.sendTo(channel, fixLogin)

        val first = ChannelVsFlowPractice.receiveFrom(channel)
        val secondWaiting = launch { ChannelVsFlowPractice.receiveFrom(channel) }
        testScheduler.runCurrent()

        assertEquals(fixLogin, first)
        assertTrue(secondWaiting.isActive)
        secondWaiting.cancelAndJoin()
    }

    @Test
    fun `asFlow collector receives a sent value`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()
        val emitted = mutableListOf<Ticket>()

        val job = launch {
            ChannelVsFlowPractice.asFlow(channel).collect { emitted += it }
        }
        testScheduler.runCurrent()
        ChannelVsFlowPractice.sendTo(channel, fixLogin)
        testScheduler.runCurrent()

        assertEquals(listOf(fixLogin), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `asFlow two collectors split values instead of broadcasting`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()
        val first = mutableListOf<Ticket>()
        val second = mutableListOf<Ticket>()

        val job1 = launch {
            ChannelVsFlowPractice.asFlow(channel).collect { first += it }
        }
        val job2 = launch {
            ChannelVsFlowPractice.asFlow(channel).collect { second += it }
        }
        testScheduler.runCurrent()
        ChannelVsFlowPractice.sendTo(channel, fixLogin)
        testScheduler.runCurrent()

        assertEquals(1, first.size + second.size)
        assertEquals(setOf(fixLogin), (first + second).toSet())
        assertTrue(first.isEmpty() || second.isEmpty())
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `asFlow completes when the channel is closed`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()
        val emitted = mutableListOf<Ticket>()

        val job = launch {
            ChannelVsFlowPractice.asFlow(channel).collect { emitted += it }
        }
        testScheduler.runCurrent()
        ChannelVsFlowPractice.sendTo(channel, fixLogin)
        channel.close()
        job.join()

        assertEquals(listOf(fixLogin), emitted)
        assertFalse(job.isActive)
    }

    @Test
    fun `asFlow does not complete while the channel is open`() = runTest {
        val channel = ChannelVsFlowPractice.unlimited<Ticket>()
        val emitted = mutableListOf<Ticket>()

        val job = launch {
            ChannelVsFlowPractice.asFlow(channel).collect { emitted += it }
        }
        testScheduler.runCurrent()
        ChannelVsFlowPractice.sendTo(channel, addDarkMode)
        testScheduler.runCurrent()

        assertEquals(listOf(addDarkMode), emitted)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `stateStream latestOrNull is the initial value`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")

        assertEquals("idle", stream.latestOrNull())
    }

    @Test
    fun `stateStream value before collector is delivered as current state`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")
        stream.emit("ready")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("ready"), late)
        assertEquals("ready", stream.latestOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun `stateStream value after collector is delivered to every collector`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        val job2 = launch { stream.observe().collect { second += it } }
        testScheduler.runCurrent()
        stream.emit("ready")
        testScheduler.runCurrent()

        assertEquals(listOf("idle", "ready"), first)
        assertEquals(listOf("idle", "ready"), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `stateStream reconnecting collector receives current value not history`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")
        val first = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        testScheduler.runCurrent()
        stream.emit("loading")
        stream.emit("ready")
        testScheduler.runCurrent()
        job1.cancelAndJoin()

        val late = mutableListOf<String>()
        val job2 = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("ready"), late)
        job2.cancelAndJoin()
    }

    @Test
    fun `stateStream many rapid values before a collector keep only the last`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")
        stream.emit("a")
        stream.emit("b")
        stream.emit("c")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("c"), late)
        assertEquals("c", stream.latestOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun `stateStream many rapid values with a collector skip intermediates`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")
        val emitted = mutableListOf<String>()

        val job = launch { stream.observe().collect { emitted += it } }
        testScheduler.runCurrent()
        stream.emit("a")
        stream.emit("b")
        stream.emit("c")
        testScheduler.runCurrent()

        assertEquals(listOf("idle", "c"), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `stateStream equal consecutive values are conflated`() = runTest {
        val stream = ChannelVsFlowPractice.stateStream("idle")
        val emitted = mutableListOf<String>()

        val job = launch { stream.observe().collect { emitted += it } }
        testScheduler.runCurrent()
        stream.emit("ready")
        stream.emit("ready")
        testScheduler.runCurrent()

        assertEquals(listOf("idle", "ready"), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `sharedStream replay 0 latestOrNull is null even after emit`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 0)
        stream.emit("saved")

        assertNull(stream.latestOrNull())
    }

    @Test
    fun `sharedStream replay 0 value before collector is dropped`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 0)
        stream.emit("saved")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), late)
        job.cancelAndJoin()
    }

    @Test
    fun `sharedStream value after collector is broadcast to every collector`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 0)
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        val job2 = launch { stream.observe().collect { second += it } }
        testScheduler.runCurrent()
        stream.emit("saved")
        testScheduler.runCurrent()

        assertEquals(listOf("saved"), first)
        assertEquals(listOf("saved"), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `sharedStream replay 0 reconnecting collector does not receive a past value`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 0)
        val first = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        testScheduler.runCurrent()
        stream.emit("saved")
        testScheduler.runCurrent()
        job1.cancelAndJoin()

        val late = mutableListOf<String>()
        val job2 = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("saved"), first)
        assertEquals(emptyList(), late)
        job2.cancelAndJoin()
    }

    @Test
    fun `sharedStream replay 0 many rapid values before a collector are all dropped`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 0)
        stream.emit("a")
        stream.emit("b")
        stream.emit("c")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(emptyList(), late)
        assertNull(stream.latestOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun `sharedStream replay 1 late collector receives only the last value`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 1)
        stream.emit("a")
        stream.emit("b")
        stream.emit("c")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("c"), late)
        assertEquals("c", stream.latestOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun `sharedStream does not conflate equal consecutive values`() = runTest {
        val stream = ChannelVsFlowPractice.sharedStream<String>(replay = 0)
        val emitted = mutableListOf<String>()

        val job = launch { stream.observe().collect { emitted += it } }
        testScheduler.runCurrent()
        stream.emit("saved")
        stream.emit("saved")
        testScheduler.runCurrent()

        assertEquals(listOf("saved", "saved"), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `channelStream latestOrNull is always null`() = runTest {
        val stream = ChannelVsFlowPractice.channelStream<String>()
        stream.emit("queued")

        assertNull(stream.latestOrNull())
    }

    @Test
    fun `channelStream value before collector stays in the queue`() = runTest {
        val stream = ChannelVsFlowPractice.channelStream<String>()
        stream.emit("queued")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("queued"), late)
        assertNull(stream.latestOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun `channelStream value after collector is not broadcast`() = runTest {
        val stream = ChannelVsFlowPractice.channelStream<String>()
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        val job2 = launch { stream.observe().collect { second += it } }
        testScheduler.runCurrent()
        stream.emit("work")
        testScheduler.runCurrent()

        assertEquals(1, first.size + second.size)
        assertEquals(setOf("work"), (first + second).toSet())
        assertTrue(first.isEmpty() || second.isEmpty())
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `channelStream reconnecting collector does not receive a consumed value`() = runTest {
        val stream = ChannelVsFlowPractice.channelStream<String>()
        val first = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        testScheduler.runCurrent()
        stream.emit("work")
        testScheduler.runCurrent()
        job1.cancelAndJoin()

        val late = mutableListOf<String>()
        val job2 = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("work"), first)
        assertEquals(emptyList(), late)
        job2.cancelAndJoin()
    }

    @Test
    fun `channelStream many rapid values before a collector are all queued`() = runTest {
        val stream = ChannelVsFlowPractice.channelStream<String>()
        stream.emit("a")
        stream.emit("b")
        stream.emit("c")

        val late = mutableListOf<String>()
        val job = launch { stream.observe().collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf("a", "b", "c"), late)
        assertNull(stream.latestOrNull())
        job.cancelAndJoin()
    }

    @Test
    fun `channelStream two collectors together receive each queued value once`() = runTest {
        val stream = ChannelVsFlowPractice.channelStream<String>()
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()

        val job1 = launch { stream.observe().collect { first += it } }
        val job2 = launch { stream.observe().collect { second += it } }
        testScheduler.runCurrent()
        stream.emit("a")
        stream.emit("b")
        testScheduler.runCurrent()

        assertEquals(2, first.size + second.size)
        assertEquals(setOf("a", "b"), (first + second).toSet())
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `ScreenStore current value is the initial state`() = runTest {
        val store = ScreenStore(BoardState(title = "Inbox"))

        assertEquals(BoardState(title = "Inbox"), store.state.value)
    }

    @Test
    fun `ScreenStore collector receives the initial state immediately`() = runTest {
        val store = ScreenStore()
        val emitted = mutableListOf<BoardState>()

        val job = launch { store.state.collect { emitted += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(BoardState()), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `ScreenStore setTitle updates current value and keeps tickets`() = runTest {
        val store = ScreenStore(BoardState(tickets = listOf(fixLogin)))

        store.setTitle("Today")

        assertEquals(
            BoardState(title = "Today", tickets = listOf(fixLogin)),
            store.state.value
        )
    }

    @Test
    fun `ScreenStore setTickets updates current value and keeps title`() = runTest {
        val store = ScreenStore(BoardState(title = "Today"))

        store.setTickets(listOf(addDarkMode))

        assertEquals(
            BoardState(title = "Today", tickets = listOf(addDarkMode)),
            store.state.value
        )
    }

    @Test
    fun `ScreenStore reconnecting collector receives current state not history`() = runTest {
        val store = ScreenStore()
        store.setTitle("Loading")
        store.setTitle("Ready")

        val late = mutableListOf<BoardState>()
        val job = launch { store.state.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf(BoardState(title = "Ready")), late)
        job.cancelAndJoin()
    }

    @Test
    fun `ScreenStore equal consecutive titles are conflated`() = runTest {
        val store = ScreenStore()
        val emitted = mutableListOf<BoardState>()

        val job = launch { store.state.collect { emitted += it } }
        testScheduler.runCurrent()
        store.setTitle("Ready")
        store.setTitle("Ready")
        testScheduler.runCurrent()

        assertEquals(
            listOf(BoardState(), BoardState(title = "Ready")),
            emitted
        )
        job.cancelAndJoin()
    }

    @Test
    fun `ScreenStore state is not a MutableStateFlow`() = runTest {
        val state: StateFlow<BoardState> = ScreenStore().state

        assertTrue(state !is MutableStateFlow)
    }

    @Test
    fun `ToastBus show delivers Shown`() = runTest {
        val bus = ToastBus()
        val emitted = mutableListOf<Toast>()

        val job = launch { bus.toasts.collect { emitted += it } }
        testScheduler.runCurrent()
        bus.show("saved")
        testScheduler.runCurrent()

        assertEquals(listOf<Toast>(saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `ToastBus two identical toasts are both delivered`() = runTest {
        val bus = ToastBus()
        val emitted = mutableListOf<Toast>()

        val job = launch { bus.toasts.collect { emitted += it } }
        testScheduler.runCurrent()
        bus.show("saved")
        bus.show("saved")
        testScheduler.runCurrent()

        assertEquals(listOf<Toast>(saved, saved), emitted)
        job.cancelAndJoin()
    }

    @Test
    fun `ToastBus late collector after show receives nothing`() = runTest {
        val bus = ToastBus()
        val first = mutableListOf<Toast>()

        val job1 = launch { bus.toasts.collect { first += it } }
        testScheduler.runCurrent()
        bus.show("saved")
        testScheduler.runCurrent()
        job1.cancelAndJoin()

        val late = mutableListOf<Toast>()
        val job2 = launch { bus.toasts.collect { late += it } }
        testScheduler.runCurrent()

        assertEquals(listOf<Toast>(saved), first)
        assertEquals(emptyList(), late)
        job2.cancelAndJoin()
    }

    @Test
    fun `ToastBus two collectors both receive Show`() = runTest {
        val bus = ToastBus()
        val first = mutableListOf<Toast>()
        val second = mutableListOf<Toast>()

        val job1 = launch { bus.toasts.collect { first += it } }
        val job2 = launch { bus.toasts.collect { second += it } }
        testScheduler.runCurrent()
        bus.show("disk full")
        testScheduler.runCurrent()

        assertEquals(listOf<Toast>(diskFull), first)
        assertEquals(listOf<Toast>(diskFull), second)
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `ToastBus toasts is not a MutableSharedFlow`() = runTest {
        val toasts: SharedFlow<Toast> = ToastBus().toasts

        assertTrue(toasts !is MutableSharedFlow)
    }

    @Test
    fun `JobQueue submit then take delivers the ticket`() = runTest {
        val queue = JobQueue()

        queue.submit(fixLogin)

        assertEquals(fixLogin, queue.take())
    }

    @Test
    fun `JobQueue take suspends until submit`() = runTest {
        val queue = JobQueue()
        val taken = mutableListOf<Ticket>()

        val taker = launch { taken += queue.take() }
        testScheduler.runCurrent()

        assertTrue(taker.isActive)
        assertEquals(emptyList(), taken)

        queue.submit(addDarkMode)
        testScheduler.runCurrent()

        assertEquals(listOf(addDarkMode), taken)
        taker.join()
    }

    @Test
    fun `JobQueue two workers each receive a different ticket`() = runTest {
        val queue = JobQueue()
        val first = mutableListOf<Ticket>()
        val second = mutableListOf<Ticket>()

        val worker1 = launch { first += queue.take() }
        val worker2 = launch { second += queue.take() }
        testScheduler.runCurrent()
        queue.submit(fixLogin)
        queue.submit(addDarkMode)
        testScheduler.runCurrent()

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertEquals(setOf(fixLogin, addDarkMode), (first + second).toSet())
        worker1.join()
        worker2.join()
    }

    @Test
    fun `JobQueue tickets flow two collectors split work`() = runTest {
        val queue = JobQueue()
        val first = mutableListOf<Ticket>()
        val second = mutableListOf<Ticket>()

        val job1 = launch { queue.tickets().collect { first += it } }
        val job2 = launch { queue.tickets().collect { second += it } }
        testScheduler.runCurrent()
        queue.submit(fixLogin)
        testScheduler.runCurrent()

        assertEquals(1, first.size + second.size)
        assertEquals(setOf(fixLogin), (first + second).toSet())
        job1.cancelAndJoin()
        job2.cancelAndJoin()
    }

    @Test
    fun `JobQueue close completes the tickets flow`() = runTest {
        val queue = JobQueue()
        val emitted = mutableListOf<Ticket>()

        val job = launch { queue.tickets().collect { emitted += it } }
        testScheduler.runCurrent()
        queue.submit(writeTests)
        queue.close()
        job.join()

        assertEquals(listOf(writeTests), emitted)
        assertFalse(job.isActive)
    }
}

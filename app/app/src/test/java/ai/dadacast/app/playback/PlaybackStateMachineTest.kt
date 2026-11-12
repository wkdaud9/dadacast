package ai.dadacast.app.playback

import ai.dadacast.app.content.Chunk
import ai.dadacast.app.content.Episode
import ai.dadacast.app.content.Sentence
import ai.dadacast.app.playback.PlayerEffect.CancelTimer
import ai.dadacast.app.playback.PlayerEffect.Speak
import ai.dadacast.app.playback.PlayerEffect.StartTimer
import ai.dadacast.app.playback.PlayerEffect.StopSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackStateMachineTest {
    private val pauseMs = 2_000L

    private fun chunk(id: String, count: Int, pause: Long = pauseMs) = Chunk(
        id = id,
        title = id,
        estSeconds = 30,
        sentences = List(count) { Sentence("$id-s$it", "ko-KR", pause) },
    )

    private val episodes = listOf(
        Episode("e0", "E0", "x", listOf(chunk("c0", 2), chunk("c1", 2), chunk("c2", 1))),
        Episode("e1", "E1", "x", listOf(chunk("d0", 1))),
    )

    private lateinit var machine: PlaybackStateMachine
    private var lastEffects: List<PlayerEffect> = emptyList()
    private var now = 0L

    @Before
    fun setUp() {
        machine = PlaybackStateMachine(episodes, settleDelayMs = 500, clock = { now })
    }

    private var spoken: Speak? = null
    private var started: StartTimer? = null

    private fun send(event: PlayerEvent): List<PlayerEffect> = machine.handle(event).also { effects ->
        lastEffects = effects
        effects.filterIsInstance<Speak>().lastOrNull()?.let { spoken = it }
        effects.filterIsInstance<StartTimer>().lastOrNull()?.let { started = it }
    }

    private fun lastSpeak(): Speak = spoken!!

    private fun timer(): StartTimer = started!!

    private fun finishSentence() = send(PlayerEvent.SpeechDone(lastSpeak().token))

    private fun fireTimer() = send(PlayerEvent.TimerFired(timer().token))

    private fun on(signal: DrivingSignal) = send(PlayerEvent.SignalOn(signal))

    private fun off(signal: DrivingSignal) = send(PlayerEvent.SignalOff(signal))

    /** Plays until the given sentence text has just started. */
    private fun playUntil(text: String) {
        send(PlayerEvent.Play(0))
        while (lastSpeak().text != text) {
            finishSentence()
            fireTimer()
        }
    }

    private val status get() = machine.snapshot.status

    @Test
    fun `plays sentence by sentence and waits pause_after_sec between them`() {
        send(PlayerEvent.Play(0))
        assertEquals("c0-s0", lastSpeak().text)
        assertEquals(PlayerStatus.PLAYING, status)

        finishSentence()
        assertEquals(pauseMs, timer().delayMs)
        assertTrue(lastEffects.none { it is Speak })

        fireTimer()
        assertEquals("c0-s1", lastSpeak().text)
    }

    @Test
    fun `no silence is added when pause is zero`() {
        val noPause = listOf(Episode("e", "E", "x", listOf(chunk("c", 2, pause = 0))))
        machine = PlaybackStateMachine(noPause, settleDelayMs = 500)
        send(PlayerEvent.Play(0))
        finishSentence()
        assertEquals("c-s1", lastSpeak().text)
    }

    @Test
    fun `episode ends after the last chunk`() {
        send(PlayerEvent.Play(1))
        finishSentence()
        fireTimer()
        assertEquals(PlayerStatus.FINISHED, status)
    }

    @Test
    fun `sudden signal stops at sentence end and resumes with the next sentence`() {
        playUntil("c1-s1")
        on(DrivingSignal.TURN_SIGNAL)
        assertTrue(lastEffects.isEmpty())
        assertEquals(PlayerStatus.ENDING_SENTENCE, status)

        finishSentence()
        assertEquals(PlayerStatus.HELD, status)
        assertEquals(DrivingSignal.TURN_SIGNAL, machine.snapshot.holdReason)

        off(DrivingSignal.TURN_SIGNAL)
        assertEquals(PlayerStatus.RESUMING, status)
        assertEquals(500L, timer().delayMs)

        fireTimer()
        assertEquals("c2-s0", lastSpeak().text)
        assertEquals(PlayerStatus.PLAYING, status)
    }

    @Test
    fun `sudden signal during a gap holds at once and cancels the gap`() {
        send(PlayerEvent.Play(0))
        finishSentence()
        on(DrivingSignal.TURN_SIGNAL)
        assertEquals(listOf(CancelTimer), lastEffects)
        assertEquals(PlayerStatus.HELD, status)
    }

    @Test
    fun `a hold after the last sentence of a chunk continues with the next chunk`() {
        playUntil("c0-s1")
        on(DrivingSignal.TURN_SIGNAL)
        finishSentence()
        off(DrivingSignal.TURN_SIGNAL)
        fireTimer()
        assertEquals("c1-s0", lastSpeak().text)
    }

    @Test
    fun `sudden signal that clears before the sentence ends does not stop playback`() {
        send(PlayerEvent.Play(0))
        on(DrivingSignal.TURN_SIGNAL)
        off(DrivingSignal.TURN_SIGNAL)
        assertEquals(PlayerStatus.PLAYING, status)
        finishSentence()
        fireTimer()
        assertEquals("c0-s1", lastSpeak().text)
    }

    @Test
    fun `advance signal lets the chunk finish and resumes with the next chunk`() {
        send(PlayerEvent.Play(0))
        on(DrivingSignal.TURN_GUIDANCE)
        assertEquals(PlayerStatus.PLAYING, status)

        finishSentence()
        fireTimer()
        assertEquals("c0-s1", lastSpeak().text)
        finishSentence()
        fireTimer()
        assertEquals(PlayerStatus.HELD, status)
        assertEquals(1, machine.snapshot.chunkIndex)
        assertEquals(DrivingSignal.TURN_GUIDANCE, machine.snapshot.holdReason)

        off(DrivingSignal.TURN_GUIDANCE)
        fireTimer()
        assertEquals("c1-s0", lastSpeak().text)
    }

    @Test
    fun `advance signal does not hold when the episode is ending`() {
        send(PlayerEvent.Play(1))
        on(DrivingSignal.CAUTION_POINT)
        finishSentence()
        fireTimer()
        assertEquals(PlayerStatus.FINISHED, status)
    }

    @Test
    fun `sudden signal overrides a pending advance signal`() {
        send(PlayerEvent.Play(0))
        on(DrivingSignal.TURN_GUIDANCE)
        on(DrivingSignal.TURN_SIGNAL)
        assertEquals(PlayerStatus.ENDING_SENTENCE, status)
        finishSentence()
        assertEquals(PlayerStatus.HELD, status)
        assertEquals(1, machine.snapshot.sentenceIndex)
    }

    @Test
    fun `playback waits until every signal clears`() {
        send(PlayerEvent.Play(0))
        on(DrivingSignal.TURN_SIGNAL)
        on(DrivingSignal.OTHER_AUDIO)
        off(DrivingSignal.TURN_SIGNAL)
        assertEquals(PlayerStatus.HELD, status)
        assertEquals(DrivingSignal.OTHER_AUDIO, machine.snapshot.holdReason)
        assertTrue(lastEffects.isEmpty())
        off(DrivingSignal.OTHER_AUDIO)
        assertEquals(PlayerStatus.RESUMING, status)
    }

    @Test
    fun `a new signal during the settle delay cancels the resume`() {
        send(PlayerEvent.Play(0))
        on(DrivingSignal.TURN_SIGNAL)
        finishSentence()
        off(DrivingSignal.TURN_SIGNAL)
        val staleToken = timer().token

        on(DrivingSignal.TURN_SIGNAL)
        assertEquals(listOf(CancelTimer), lastEffects)
        assertEquals(PlayerStatus.HELD, status)
        assertTrue(send(PlayerEvent.TimerFired(staleToken)).isEmpty())
        assertEquals(PlayerStatus.HELD, status)
    }

    @Test
    fun `other audio cuts speech immediately and the cut sentence is said again`() {
        playUntil("c1-s1")
        on(DrivingSignal.OTHER_AUDIO)
        assertEquals(listOf(StopSpeech), lastEffects)
        assertEquals(PlayerStatus.HELD, status)

        off(DrivingSignal.OTHER_AUDIO)
        fireTimer()
        assertEquals("c1-s1", lastSpeak().text)
    }

    @Test
    fun `repeated interruptions in one sentence never go further back`() {
        playUntil("c1-s1")
        repeat(3) {
            on(DrivingSignal.OTHER_AUDIO)
            off(DrivingSignal.OTHER_AUDIO)
            fireTimer()
            assertEquals("c1-s1", lastSpeak().text)
            assertEquals(1, machine.snapshot.chunkIndex)
        }
        finishSentence()
        fireTimer()
        assertEquals("c2-s0", lastSpeak().text)
    }

    @Test
    fun `an interruption during the silence after a sentence continues with the next sentence`() {
        playUntil("c1-s0")
        finishSentence()
        on(DrivingSignal.OTHER_AUDIO)
        assertEquals(listOf(CancelTimer), lastEffects)
        off(DrivingSignal.OTHER_AUDIO)
        fireTimer()
        assertEquals("c1-s1", lastSpeak().text)
    }

    @Test
    fun `interruptions in a later chunk stay in that chunk`() {
        val long = listOf(
            Episode("e", "E", "x", listOf(chunk("a", 2), chunk("b", 2), chunk("c", 4))),
        )
        machine = PlaybackStateMachine(long, settleDelayMs = 500)
        send(PlayerEvent.Play(0))
        while (lastSpeak().text != "c-s2") {
            finishSentence()
            fireTimer()
        }
        on(DrivingSignal.OTHER_AUDIO)
        off(DrivingSignal.OTHER_AUDIO)
        fireTimer()
        assertEquals("c-s2", lastSpeak().text)
        assertEquals(2, machine.snapshot.chunkIndex)
        on(DrivingSignal.TURN_SIGNAL)
        finishSentence()
        off(DrivingSignal.TURN_SIGNAL)
        fireTimer()
        assertEquals("c-s3", lastSpeak().text)
    }

    @Test
    fun `an advance signal does not keep a mid-chunk resume waiting`() {
        playUntil("c1-s1")
        on(DrivingSignal.OTHER_AUDIO)
        on(DrivingSignal.TURN_GUIDANCE)
        assertEquals(PlayerStatus.HELD, status)
        // Only the turn is left; it waits for the chunk boundary, so the cut sentence goes on.
        off(DrivingSignal.OTHER_AUDIO)
        assertEquals(PlayerStatus.RESUMING, status)
        fireTimer()
        assertEquals("c1-s1", lastSpeak().text)
        finishSentence()
        fireTimer()
        assertEquals(PlayerStatus.HELD, status)
        assertEquals(2, machine.snapshot.chunkIndex)
    }

    @Test
    fun `speech completion from a cancelled sentence is ignored`() {
        send(PlayerEvent.Play(0))
        val stale = lastSpeak().token
        on(DrivingSignal.OTHER_AUDIO)
        assertTrue(send(PlayerEvent.SpeechDone(stale)).isEmpty())
        assertEquals(PlayerStatus.HELD, status)
    }

    @Test
    fun `no chunk starts while a signal is active`() {
        on(DrivingSignal.TURN_SIGNAL)
        send(PlayerEvent.Play(0))
        assertTrue(lastEffects.none { it is Speak })
        assertEquals(PlayerStatus.HELD, status)
        off(DrivingSignal.TURN_SIGNAL)
        fireTimer()
        assertEquals("c0-s0", lastSpeak().text)
    }

    @Test
    fun `pausing during a hold cancels the automatic resume`() {
        send(PlayerEvent.Play(0))
        on(DrivingSignal.TURN_SIGNAL)
        finishSentence()
        send(PlayerEvent.Pause)
        assertEquals(PlayerStatus.PAUSED, status)
        off(DrivingSignal.TURN_SIGNAL)
        assertTrue(lastEffects.isEmpty())
        assertEquals(PlayerStatus.PAUSED, status)
    }

    @Test
    fun `pause stops speech and play says the cut sentence again`() {
        playUntil("c1-s1")
        send(PlayerEvent.Pause)
        assertEquals(listOf(StopSpeech), lastEffects)
        assertFalse(machine.snapshot.wantsAudio)
        send(PlayerEvent.Play())
        assertEquals("c1-s1", lastSpeak().text)
    }

    @Test
    fun `next moves between episodes`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.Next)
        assertTrue(lastEffects.contains(StopSpeech))
        assertEquals("d0-s0", lastSpeak().text)
        assertTrue(send(PlayerEvent.Next).isEmpty())
    }

    @Test
    fun `previous says the current sentence again once it has been playing a while`() {
        playUntil("c0-s1")
        now += 3_000
        send(PlayerEvent.Previous)
        assertTrue(lastEffects.contains(StopSpeech))
        assertEquals("c0-s1", lastSpeak().text)
        assertEquals(0, machine.snapshot.episodeIndex)
    }

    @Test
    fun `previous right after a sentence starts goes to the sentence before`() {
        playUntil("c0-s1")
        now += 1_000
        send(PlayerEvent.Previous)
        assertEquals("c0-s0", lastSpeak().text)
        // At the very first sentence it only starts again.
        send(PlayerEvent.Previous)
        assertEquals("c0-s0", lastSpeak().text)
        assertEquals(PlayerStatus.PLAYING, status)
    }

    @Test
    fun `previous crosses back into the earlier chunk`() {
        playUntil("c1-s0")
        send(PlayerEvent.Previous)
        assertEquals("c0-s1", lastSpeak().text)
        assertEquals(0, machine.snapshot.chunkIndex)
    }

    @Test
    fun `previous in the silence after a sentence says that sentence again`() {
        playUntil("c0-s1")
        now += 3_000
        finishSentence()
        send(PlayerEvent.Previous)
        assertTrue(lastEffects.contains(CancelTimer))
        assertEquals("c0-s1", lastSpeak().text)
    }

    @Test
    fun `previous while paused moves back without playing`() {
        playUntil("c1-s1")
        send(PlayerEvent.Pause)
        send(PlayerEvent.Previous)
        assertTrue(lastEffects.none { it is Speak })
        assertEquals(PlayerStatus.PAUSED, status)
        send(PlayerEvent.Play())
        assertEquals("c1-s0", lastSpeak().text)
    }

    @Test
    fun `previous during a hold keeps holding`() {
        playUntil("c0-s1")
        on(DrivingSignal.OTHER_AUDIO)
        send(PlayerEvent.Previous)
        assertTrue(lastEffects.none { it is Speak })
        assertEquals(PlayerStatus.HELD, status)
        off(DrivingSignal.OTHER_AUDIO)
        fireTimer()
        assertEquals("c0-s0", lastSpeak().text)
    }

    private fun fail() = send(PlayerEvent.SpeechFailed(lastSpeak().token))

    @Test
    fun `a failed sentence is tried once more`() {
        send(PlayerEvent.Play(0))
        fail()
        assertEquals(PlayerStatus.PLAYING, status)
        assertTrue(machine.snapshot.wantsAudio)
        assertEquals(PlaybackStateMachine.RETRY_DELAY_MS, timer().delayMs)
        fireTimer()
        assertEquals("c0-s0", lastSpeak().text)
        finishSentence()
        fireTimer()
        assertEquals("c0-s1", lastSpeak().text)
    }

    @Test
    fun `a sentence that fails twice is skipped`() {
        send(PlayerEvent.Play(0))
        fail()
        fireTimer()
        fail()
        fireTimer()
        assertEquals("c0-s1", lastSpeak().text)
        assertEquals(PlayerStatus.PLAYING, status)
    }

    @Test
    fun `several sentences failing in a row stop with an error and play tries again`() {
        send(PlayerEvent.Play(0))
        repeat(PlaybackStateMachine.MAX_FAILED_SENTENCES - 1) {
            fail(); fireTimer(); fail(); fireTimer()
        }
        assertEquals("c1-s0", lastSpeak().text)
        fail(); fireTimer(); fail()
        assertEquals(PlayerStatus.ERROR, status)
        assertFalse(machine.snapshot.wantsAudio)
        send(PlayerEvent.Play())
        assertEquals("c1-s0", lastSpeak().text)
    }

    @Test
    fun `a success in between resets the failure count`() {
        send(PlayerEvent.Play(0))
        fail(); fireTimer(); fail(); fireTimer() // c0-s0 skipped
        finishSentence(); fireTimer() // c0-s1 fine
        fail(); fireTimer(); fail(); fireTimer() // c1-s0 skipped
        fail(); fireTimer(); fail(); fireTimer() // c1-s1 skipped
        assertEquals("c2-s0", lastSpeak().text)
        assertEquals(PlayerStatus.PLAYING, status)
    }

    @Test
    fun `pausing while waiting to retry does not resume on its own`() {
        send(PlayerEvent.Play(0))
        fail()
        val retry = timer()
        send(PlayerEvent.Pause)
        assertTrue(lastEffects.contains(CancelTimer))
        assertEquals(PlayerStatus.PAUSED, status)
        send(PlayerEvent.TimerFired(retry.token))
        assertTrue(lastEffects.isEmpty())
        assertEquals(PlayerStatus.PAUSED, status)
    }

    @Test
    fun `a retry waits out an active hold`() {
        playUntil("c0-s1")
        on(DrivingSignal.TURN_SIGNAL)
        fail()
        fireTimer()
        assertTrue(lastEffects.none { it is Speak })
        assertEquals(PlayerStatus.HELD, status)
        off(DrivingSignal.TURN_SIGNAL)
        fireTimer()
        assertEquals("c0-s1", lastSpeak().text)
    }

    @Test
    fun `other audio during the retry wait holds at once`() {
        send(PlayerEvent.Play(0))
        fail()
        on(DrivingSignal.OTHER_AUDIO)
        assertTrue(lastEffects.contains(CancelTimer))
        assertEquals(PlayerStatus.HELD, status)
    }

    @Test
    fun `a saved position comes back paused and plays from that sentence`() {
        send(PlayerEvent.Restore(0, 1, 1))
        assertEquals(PlayerStatus.PAUSED, status)
        assertTrue(lastEffects.isEmpty())
        send(PlayerEvent.Play())
        assertEquals("c1-s1", lastSpeak().text)
    }

    @Test
    fun `restore is ignored once something has played or the position is invalid`() {
        send(PlayerEvent.Restore(0, 9, 0))
        assertEquals(PlayerStatus.IDLE, status)
        send(PlayerEvent.Play(1))
        send(PlayerEvent.Restore(0, 1, 1))
        assertEquals(1, machine.snapshot.episodeIndex)
    }

    @Test
    fun `stop ends playback and keeps the position`() {
        playUntil("c1-s1")
        send(PlayerEvent.Stop)
        assertEquals(listOf(StopSpeech), lastEffects)
        assertEquals(PlayerStatus.IDLE, status)
        assertFalse(machine.snapshot.wantsAudio)
    }
}

package ai.dadacast.app.playback

import ai.dadacast.app.content.Chunk
import ai.dadacast.app.content.Episode
import ai.dadacast.app.content.RedLightPractice
import ai.dadacast.app.content.Roleplay
import ai.dadacast.app.content.RoleplayTurn
import ai.dadacast.app.content.Sentence
import ai.dadacast.app.practice.PracticeKind
import ai.dadacast.app.practice.PracticeScripts
import ai.dadacast.app.practice.Verdict
import ai.dadacast.app.speech.VoiceRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PracticeFlowTest {
    private val practice = RedLightPractice(
        targetExpression = "I have a reservation under the name Kim.",
        prompt = "Do you have a reservation?",
        promptLanguage = "en-US",
        sampleAnswer = "Yes, I have a reservation under the name Kim.",
        answerLanguage = "en-US",
        followup = "Perfect.",
        maxSeconds = 30,
    )

    private val roleplay = Roleplay(
        id = "rp",
        title = "rp",
        intro = listOf(Sentence("소개", "ko-KR", 0)),
        turns = listOf(
            RoleplayTurn(true, "How can I help you?", "en-US"),
            RoleplayTurn(false, "I have a reservation.", "en-US"),
            RoleplayTurn(true, "Your ID, please?", "en-US"),
            RoleplayTurn(false, "Here you go.", "en-US"),
            RoleplayTurn(true, "Thank you.", "en-US"),
        ),
    )

    private val episode = Episode(
        id = "e",
        title = "E",
        category = "x",
        chunks = listOf(
            Chunk("c0", "c0", 30, listOf(Sentence("c0-s0", "ko-KR", 0)), practice),
            Chunk("c1", "c1", 30, listOf(Sentence("c1-s0", "ko-KR", 0)), practice),
            Chunk("c2", "c2", 30, listOf(Sentence("c2-s0", "ko-KR", 0))),
        ),
        roleplays = listOf(roleplay),
    )

    private lateinit var machine: PlaybackStateMachine
    private var effects: List<PlayerEffect> = emptyList()
    private val spoken = mutableListOf<String>()
    private var lastToken = 0L

    @Before
    fun setUp() {
        machine = PlaybackStateMachine(listOf(episode), settleDelayMs = 500)
    }

    private fun send(event: PlayerEvent) {
        effects = machine.handle(event)
        effects.forEach {
            when (it) {
                is PlayerEffect.Speak -> { spoken += it.text; lastToken = it.token }
                is PlayerEffect.Listen -> lastToken = it.token
                else -> Unit
            }
        }
    }

    private fun done() = send(PlayerEvent.SpeechDone(lastToken))

    private fun listening() = effects.any { it is PlayerEffect.Listen }

    /** Finishes spoken lines until the machine asks to listen or leaves the practice. */
    private fun talkUntilListenOrEnd() {
        var guard = 0
        while (!listening() && machine.snapshot.status == PlayerStatus.PRACTICE && guard++ < 20) done()
    }

    private val status get() = machine.snapshot.status

    @Test
    fun `nothing is offered while the car moves`() {
        send(PlayerEvent.Play(0))
        done()
        assertEquals(PlayerStatus.PLAYING, status)
        assertEquals("c1-s0", spoken.last())
    }

    @Test
    fun `a red light stop offers the chunk practice at the boundary and a good answer gets praise`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        assertEquals(PlayerStatus.PLAYING, status)
        done()
        assertEquals(PlayerStatus.PRACTICE, status)
        assertEquals(PracticeScripts.OFFER_RED_LIGHT, spoken.last())
        talkUntilListenOrEnd()
        assertTrue(listening())
        assertTrue(machine.snapshot.practice!!.listening)
        assertEquals(PracticeKind.RED_LIGHT, machine.snapshot.practice!!.kind)

        // Speech ended: the answer is being checked before the reply.
        send(PlayerEvent.SpeechEnded(lastToken))
        assertTrue(machine.snapshot.practice!!.checking)
        assertFalse(machine.snapshot.practice!!.listening)
        send(PlayerEvent.Heard(lastToken, "yes i have a reservation under the name kim"))
        assertFalse(machine.snapshot.practice!!.checking)
        assertEquals(PracticeScripts.GOOD, spoken.last())
        assertEquals(Verdict.GOOD, machine.snapshot.practice!!.verdict)
        talkUntilListenOrEnd()
        // Practice over: the next chunk plays from its start.
        assertEquals(PlayerStatus.PLAYING, status)
        assertEquals("c1-s0", spoken.last())
        assertTrue(spoken.containsAll(listOf("Perfect.", PracticeScripts.DONE)))
    }

    @Test
    fun `a wrong answer hears the model phrase once`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        talkUntilListenOrEnd()
        send(PlayerEvent.Heard(lastToken, "a large pizza please"))
        assertEquals(PracticeScripts.MISS, spoken.last())
        done()
        assertEquals(practice.sampleAnswer, spoken.last())
        talkUntilListenOrEnd()
        assertEquals("c1-s0", spoken.last())
        assertEquals(1, spoken.count { it == practice.sampleAnswer })
    }

    @Test
    fun `no answer in time lets the episode go on`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        talkUntilListenOrEnd()
        val timer = effects.filterIsInstance<PlayerEffect.StartTimer>().single()
        assertEquals(PracticeScripts.LISTEN_MS, timer.delayMs)
        send(PlayerEvent.TimerFired(timer.token))
        assertTrue(effects.contains(PlayerEffect.StopListening))
        assertEquals(PracticeScripts.SILENT, spoken.last())
        done()
        assertEquals("c1-s0", spoken.last())
    }

    @Test
    fun `moving while listening stops listening and plays the next chunk`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        talkUntilListenOrEnd()
        val listenToken = lastToken
        send(PlayerEvent.StopChanged(StopContext.NONE))
        assertTrue(effects.contains(PlayerEffect.StopListening))
        assertEquals("c1-s0", spoken.last())
        assertNull(machine.snapshot.practice)
        // A late result from the cancelled listen changes nothing.
        send(PlayerEvent.Heard(listenToken, "yes i have a reservation"))
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `moving while the practice talks stops speech at once`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        send(PlayerEvent.StopChanged(StopContext.NONE))
        assertEquals(PlayerEffect.StopSpeech, effects.first())
        assertEquals("c1-s0", spoken.last())
    }

    @Test
    fun `parked offers the roleplay once and a missed answer ends it`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.PARKED))
        done()
        assertEquals(PracticeScripts.OFFER_ROLEPLAY, spoken.last())
        assertEquals(PracticeKind.ROLEPLAY, machine.snapshot.practice!!.kind)
        talkUntilListenOrEnd()
        assertEquals("How can I help you?", spoken.last())
        send(PlayerEvent.Heard(lastToken, "i have a reservation"))
        talkUntilListenOrEnd()
        assertEquals("Your ID, please?", spoken.last())
        // The phrase shown in the media app follows the answer being asked for now.
        assertEquals("Here you go.", machine.snapshot.practice!!.targetPhrase)
        assertTrue(machine.snapshot.practice!!.listening)
        send(PlayerEvent.Heard(lastToken, "what time is it"))
        talkUntilListenOrEnd()
        assertEquals("c1-s0", spoken.last())
        assertFalse(spoken.contains("Thank you."))
        // Next boundary while still parked: the roleplay is done, the chunk practice is offered.
        done()
        assertEquals(PracticeScripts.OFFER_RED_LIGHT, spoken.last())
    }

    @Test
    fun `navigation audio during a practice ends it and holds`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        talkUntilListenOrEnd()
        send(PlayerEvent.SignalOn(DrivingSignal.OTHER_AUDIO))
        assertTrue(effects.contains(PlayerEffect.StopListening))
        assertEquals(PlayerStatus.HELD, status)
        send(PlayerEvent.SignalOff(DrivingSignal.OTHER_AUDIO))
        send(PlayerEvent.TimerFired(lastTimer()))
        assertEquals("c1-s0", spoken.last())
    }

    private fun lastTimer(): Long = effects.filterIsInstance<PlayerEffect.StartTimer>().last().token

    @Test
    fun `wipers running skip the red light practice but not the parked roleplay`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.WiperChanged(true))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        assertEquals(PlayerStatus.PLAYING, status)
        assertEquals("c1-s0", spoken.last())
        send(PlayerEvent.StopChanged(StopContext.PARKED))
        done()
        assertEquals(PracticeScripts.OFFER_ROLEPLAY, spoken.last())
    }

    @Test
    fun `wipers off again bring the red light practice back`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.WiperChanged(true))
        send(PlayerEvent.WiperChanged(false))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        assertEquals(PracticeScripts.OFFER_RED_LIGHT, spoken.last())
    }

    @Test
    fun `two silent listening windows in a row show the no microphone hint until sound arrives`() {
        val many = Episode(
            id = "m",
            title = "M",
            category = "x",
            chunks = List(4) { Chunk("k$it", "k$it", 30, listOf(Sentence("k$it-s0", "ko-KR", 0)), practice) },
        )
        machine = PlaybackStateMachine(listOf(many), settleDelayMs = 500)
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        repeat(2) { window ->
            done() // chunk ends, practice starts
            talkUntilListenOrEnd()
            assertFalse("window $window", machine.snapshot.practice!!.noMicInput)
            val timer = effects.filterIsInstance<PlayerEffect.StartTimer>().last()
            send(PlayerEvent.TimerFired(timer.token))
            talkUntilListenOrEnd() // back to the episode
        }
        done()
        talkUntilListenOrEnd()
        assertTrue(machine.snapshot.practice!!.listening)
        assertTrue(machine.snapshot.practice!!.noMicInput)
        send(PlayerEvent.InputDetected(lastToken))
        assertFalse(machine.snapshot.practice!!.noMicInput)
    }

    @Test
    fun `practice lines use the partner voice and model phrases the dialogue voice`() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        done()
        assertEquals(VoiceRole.NARRATOR, (effects.single { it is PlayerEffect.Speak } as PlayerEffect.Speak).voice)
        done()
        assertEquals(VoiceRole.PARTNER, (effects.single { it is PlayerEffect.Speak } as PlayerEffect.Speak).voice)
        done()
        send(PlayerEvent.Heard(lastToken, "a large pizza please"))
        done()
        val model = effects.single { it is PlayerEffect.Speak } as PlayerEffect.Speak
        assertEquals(practice.sampleAnswer, model.text)
        assertEquals(VoiceRole.DIALOGUE, model.voice)
    }
}

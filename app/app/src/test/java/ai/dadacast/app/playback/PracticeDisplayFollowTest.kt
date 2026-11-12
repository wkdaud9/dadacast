package ai.dadacast.app.playback

import ai.dadacast.app.R
import ai.dadacast.app.content.Chunk
import ai.dadacast.app.content.Episode
import ai.dadacast.app.content.Roleplay
import ai.dadacast.app.content.RoleplayTurn
import ai.dadacast.app.content.Sentence
import ai.dadacast.app.practice.PracticeScripts
import ai.dadacast.app.practice.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A phrase longer than two lines: no timer, the rolling lines follow what the speaker says. */
class PracticeDisplayFollowTest {
    private val answer = "I'm sorry for the short notice, but could we push the meeting back to eleven?"
    private val lines = PhraseSegments.lines(answer)

    private val roleplay = Roleplay(
        id = "rp",
        title = "rp",
        intro = listOf(Sentence("소개", "ko-KR", 0)),
        turns = listOf(
            RoleplayTurn(true, "Are we still on for ten?", "en-US"),
            RoleplayTurn(false, answer, "en-US"),
            RoleplayTurn(true, "Sure.", "en-US"),
        ),
    )
    private val episode = Episode(
        id = "e",
        title = "E",
        category = "x",
        chunks = listOf(
            Chunk("c0", "c0", 30, listOf(Sentence("c0-s0", "ko-KR", 0))),
            Chunk("c1", "c1", 30, listOf(Sentence("c1-s0", "ko-KR", 0))),
        ),
        roleplays = listOf(roleplay),
    )

    private val machine = PlaybackStateMachine(listOf(episode), settleDelayMs = 500)
    private var token = 0L
    private var timer = 0L
    private var effects: List<PlayerEffect> = emptyList()
    private val spoken = mutableListOf<String>()

    private fun send(event: PlayerEvent) {
        effects = machine.handle(event)
        effects.forEach {
            when (it) {
                is PlayerEffect.Speak -> { token = it.token; spoken += it.text }
                is PlayerEffect.Listen -> token = it.token
                is PlayerEffect.StartTimer -> timer = it.token
                else -> Unit
            }
        }
    }

    private val line get() = nowPlayingLines(machine.snapshot) as NowPlayingLines.Phrase

    /** Parked roleplay, spoken lines finished one by one until the listening window opens. */
    private fun untilListening(): PlayerEffect.Listen {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.PARKED))
        send(PlayerEvent.SpeechDone(token))
        var guard = 0
        while (effects.none { it is PlayerEffect.Listen } && guard++ < 10) {
            // Before the microphone opens, however long the intro, line 1 (and line 2 below) stay.
            assertEquals(0, line.index)
            assertEquals(lines[0], line.title)
            assertEquals(lines[1], line.next)
            send(PlayerEvent.SpeechDone(token))
        }
        return effects.filterIsInstance<PlayerEffect.Listen>().single()
    }

    @Test
    fun `the test phrase is three lines`() {
        assertEquals(listOf("I'm sorry for the short", "notice, but could we push", "the meeting back to eleven?"), lines)
    }

    private val p = listOf("one two three four", "five six seven eight", "nine ten eleven twelve")
    private fun follow(heard: String?) = PhraseSegments.followLine(p, heard)

    @Test
    fun `a line is passed at half its words`() {
        assertEquals(0, follow(null))
        assertEquals(0, follow("one"))
        assertEquals(1, follow("one two"))
        // Skipped words are fine.
        assertEquals(1, follow("one three"))
    }

    @Test
    fun `a line is passed once one of its last two words is said`() {
        assertEquals(1, follow("three"))
        assertEquals(1, follow("four"))
        assertEquals(2, follow("one two three four seven"))
    }

    @Test
    fun `a line is passed once a word of the next line is said`() {
        assertEquals(1, follow("five"))
        assertEquals(2, follow("one five nine"))
        // The last line is never passed.
        assertEquals(2, follow("one two three four five six seven eight nine ten eleven twelve"))
    }

    @Test
    fun `words are matched loosely`() {
        // "fouer" for "four", "sevin" for "seven".
        assertEquals(1, follow("fouer"))
        assertEquals(2, follow("one too sevin"))
        // An unrelated word moves nothing.
        assertEquals(0, follow("banana"))
    }

    @Test
    fun `the real misheard log still rolls to the last line`() {
        // Logged on the emulator for "... could we push the meeting back to eleven?".
        val heard = "i'm sorry for the short notice but could you pussie the meeting back goodle"
        assertEquals(lines.size - 1, PhraseSegments.followLine(lines, heard))
        // Lagging partial: line 1 read, line 2 only begun, the display is already on line 2.
        assertEquals(1, PhraseSegments.followLine(lines, "i'm sorry for the"))
        assertEquals(2, PhraseSegments.followLine(lines, "i'm sorry for the short notice but could you pussie"))
    }

    @Test
    fun `a common word said once is used on one line only`() {
        val q = listOf("the cat sat down", "on the mat today", "the end of it")
        // "the" goes to the first line (a quarter of it), not to the later lines.
        assertEquals(0, PhraseSegments.followLine(q, "the"))
    }

    @Test
    fun `the listen request carries the expected answer and the window`() {
        val listen = untilListening()
        assertEquals(answer, listen.sampleAnswer)
        // Three lines: 12 s + 3 s.
        assertEquals(15_000L, listen.windowMs)
        assertEquals(15_000L, PracticeScripts.listenWindowMs(answer))
    }

    @Test
    fun `while listening the lines roll with the live transcript, never back, then start over`() {
        untilListening()
        assertEquals(R.string.status_listening, line.statusRes)
        assertEquals(0, line.index)
        send(PlayerEvent.Partial(token, "i'm"))
        assertEquals(0, line.index)
        send(PlayerEvent.Partial(token, "i'm sorry for the"))
        assertEquals(1, line.index)
        assertEquals(lines[1], line.title)
        assertEquals(lines[2], line.next)
        // The recogniser revises its partial to fewer words: the display does not go back.
        send(PlayerEvent.Partial(token, "i'm sorry"))
        assertEquals(1, line.index)
        send(PlayerEvent.Partial(token, "i'm sorry for the short notice but could you pussie"))
        assertEquals(2, line.index)
        assertEquals(lines[2], line.title)
        // Last line: the second line shows the status.
        assertNull(line.next)
        // A partial from an older window is ignored.
        send(PlayerEvent.Partial(token - 1, "nothing"))
        assertEquals(2, line.index)
        // Answer done: feedback and model answer show line 1 again.
        send(PlayerEvent.Heard(token, answer))
        assertEquals(Verdict.GOOD, machine.snapshot.practice!!.verdict)
        assertEquals(0, line.index)
        assertNull(machine.snapshot.practice!!.heardSoFar)
    }

    @Test
    fun `new words after a speech-end notice mean listening again, not checking`() {
        untilListening()
        send(PlayerEvent.SpeechEnded(token))
        assertTrue(machine.snapshot.practice!!.checking)
        send(PlayerEvent.Partial(token, "i'm sorry for the short"))
        assertFalse(machine.snapshot.practice!!.checking)
        assertTrue(machine.snapshot.practice!!.listening)
    }

    @Test
    fun `when the window runs out the words heard so far are scored, not silence`() {
        untilListening()
        send(PlayerEvent.Partial(token, "i'm sorry for the short notice but could"))
        send(PlayerEvent.TimerFired(timer))
        assertTrue(effects.contains(PlayerEffect.StopListening))
        assertNotEquals(Verdict.SILENT, machine.snapshot.practice!!.verdict)
        assertEquals(Verdict.CLOSE, machine.snapshot.practice!!.verdict)
    }

    @Test
    fun `a final with no text falls back to the live transcript`() {
        untilListening()
        send(PlayerEvent.Partial(token, answer))
        send(PlayerEvent.Heard(token, null))
        assertEquals(Verdict.GOOD, machine.snapshot.practice!!.verdict)
    }

    @Test
    fun `the car moving while listening stops the recogniser and the phrase at once`() {
        untilListening()
        send(PlayerEvent.Partial(token, answer))
        assertEquals(2, line.index)
        val old = token
        send(PlayerEvent.StopChanged(StopContext.NONE))
        assertTrue(effects.contains(PlayerEffect.StopListening))
        assertTrue(effects.contains(PlayerEffect.CancelTimer))
        assertTrue(nowPlayingLines(machine.snapshot) is NowPlayingLines.Status)
        assertNull(machine.snapshot.practice)
        // Late results of the stopped turn change nothing.
        val before = spoken.size
        send(PlayerEvent.Partial(old, "more words"))
        send(PlayerEvent.Heard(old, answer))
        assertTrue(effects.isEmpty())
        assertEquals(before, spoken.size)
        assertEquals(PlayerStatus.PLAYING, machine.snapshot.status)
    }
}

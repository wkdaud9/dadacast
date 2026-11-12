package ai.dadacast.app.practice

import ai.dadacast.app.playback.PhraseSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One answer turn across the recogniser's short-pause cut-offs (times in ms). */
class ListenTurnTest {
    /** Three display lines: "I'm sorry, but I'm stuck" / "in traffic. I'm running" / "about thirty minutes late." */
    private val target = "I'm sorry, but I'm stuck in traffic. I'm running about thirty minutes late."
    /** One display line: quiet after speech ends the turn as before. */
    private val short = "I have a reservation."

    private fun turn(phrase: String = target, canContinue: Boolean = true) =
        ListenTurn(phrase, phrase, PracticeScripts.listenWindowMs(phrase), startedAt = 0L, canContinue = canContinue)

    private val voice = 500.0
    private val breath = 15.0

    @Test
    fun `the phrases have the line counts the tests rely on`() {
        assertEquals(3, PhraseSegments.lines(target).size)
        assertEquals(1, PhraseSegments.lines(short).size)
    }

    @Test
    fun `the logged cut-off after a breath is continued and the words add up`() {
        // As on the emulator: speech at 1.2-1.8 s, a breath at 2.1 s, recognition ended at 3.1 s.
        val t = turn()
        t.onLevel(voice, 1_200); t.onLevel(voice, 1_800); t.onLevel(breath, 2_100)
        t.onText("i'"); t.onText("i'm sorry"); t.onText("i'm sorry but")
        assertTrue(t.wouldContinue(3_100))
        assertTrue(t.recognitionEnded(3_100))
        assertEquals(2, t.recognitions)
        // Second recognition picks up the rest.
        t.onLevel(voice, 3_500)
        t.onText("i'm stuck in traffic i'm running about thirty minutes late")
        assertEquals("i'm sorry but i'm stuck in traffic i'm running about thirty minutes late", t.transcript)
        assertTrue(t.covered())
        // Covered: the turn ends with the next recognition.
        assertFalse(t.recognitionEnded(6_000))
        assertEquals(Verdict.GOOD, AnswerScorer.verdict(t.transcript, target, target))
    }

    @Test
    fun `a covered answer is not continued`() {
        val t = turn()
        t.onLevel(voice, 1_000)
        t.onText("i'm sorry but i'm stuck in traffic i'm running about thirty minutes")
        assertFalse(t.recognitionEnded(1_500))
    }

    @Test
    fun `a misheard answer counts as covered with loose matching`() {
        val t = turn()
        t.onLevel(voice, 1_000)
        // "stack", "traffik", "runnin", "thirdy" are recogniser slips.
        t.onText("i'm sorry but i'm stack in traffik i'm runnin about thirdy minutes")
        assertTrue(t.covered())
    }

    @Test
    fun `before the last line a pause does not end the turn`() {
        val t = turn()
        t.onLevel(voice, 1_000)
        t.onText("i'm sorry")
        assertEquals(0, t.line)
        // Recognition ends 5 s after the last voice: still on line 1, so listen on.
        assertTrue(t.recognitionEnded(6_000))
        assertFalse(t.silentTooLong(9_000))
        // Line 2 reached, still not the last: quiet still does not end it.
        t.onText("i'm stuck")
        assertEquals(1, t.line)
        assertFalse(t.silentTooLong(10_000))
        assertTrue(t.recognitionEnded(10_000))
        // Last line reached: now 2 s of quiet ends the turn.
        t.onLevel(voice, 10_500)
        t.onText("in traffic i'm running")
        assertEquals(2, t.line)
        assertFalse(t.silentTooLong(12_000))
        assertTrue(t.silentTooLong(12_500))
        assertFalse(t.recognitionEnded(12_600))
    }

    @Test
    fun `before the last line only the window ends the turn`() {
        val t = turn()
        assertEquals(15_000L, PracticeScripts.listenWindowMs(target))
        t.onLevel(voice, 1_000)
        t.onText("i'm sorry")
        assertTrue(t.recognitionEnded(13_900))
        // Less than a second of the 15 s window left: no new recognition.
        assertFalse(t.recognitionEnded(14_100))
    }

    @Test
    fun `the line reached never goes back`() {
        val t = turn()
        t.onText("i'm sorry but i'm stuck")
        assertEquals(1, t.line)
        t.onText("i'm")
        assertEquals(1, t.line)
    }

    @Test
    fun `two seconds of quiet after speech ends a one-line answer`() {
        val t = turn(short)
        t.onLevel(voice, 1_000)
        t.onText("i have")
        // Recognition ends 2.5 s after the last voice: the speaker has stopped.
        assertFalse(t.recognitionEnded(3_500))
        assertEquals("i have", t.transcript)
    }

    @Test
    fun `room noise is not voice, so silence ends the turn`() {
        val t = turn()
        t.onLevel(40.0, 1_000)
        assertFalse(t.recognitionEnded(1_200))
        assertNull(t.transcript)
    }

    @Test
    fun `no new recognition near the end of the window`() {
        val t = turn(short)
        t.onLevel(voice, 11_000)
        t.onText("i have")
        assertTrue(t.wouldContinue(10_900))
        assertFalse(t.recognitionEnded(11_200))
    }

    @Test
    fun `in a follow-on recognition quiet after speech ends a one-line answer`() {
        val t = turn(short)
        t.onLevel(voice, 1_000)
        t.onText("i have")
        // Only follow-on recognitions are timed out here; the first ends on its own.
        assertFalse(t.silentTooLong(5_000))
        assertTrue(t.recognitionEnded(2_000))
        assertFalse(t.silentTooLong(2_500))
        t.onLevel(voice, 2_800)
        assertFalse(t.silentTooLong(4_700))
        assertTrue(t.silentTooLong(4_800))
        assertEquals("i have", t.transcript)
    }

    @Test
    fun `recorded test speech is fed once and never re-requested`() {
        val t = turn(canContinue = false)
        t.onLevel(voice, 1_000)
        t.onText("i'm sorry")
        assertFalse(t.recognitionEnded(1_200))
    }

    @Test
    fun `an empty recognition keeps earlier words`() {
        val t = turn()
        t.onLevel(voice, 1_000)
        t.onText("i'm sorry but")
        assertTrue(t.recognitionEnded(1_500))
        t.onText("   ")
        assertEquals("i'm sorry but", t.transcript)
    }

    @Test
    fun `the window grows 3 s per line beyond two, up to 24 s`() {
        assertEquals(12_000L, PracticeScripts.listenWindowMs(short))
        assertEquals(12_000L, PracticeScripts.listenWindowMs("Yes, I have a reservation under the name Kim."))
        assertEquals(15_000L, PracticeScripts.listenWindowMs(target))
        assertEquals(24_000L, PracticeScripts.listenWindowMs("word ".repeat(200)))
        assertEquals(12_000L, PracticeScripts.listenWindowMs("   "))
    }
}

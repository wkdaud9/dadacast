package ai.dadacast.app.playback

import ai.dadacast.app.R
import ai.dadacast.app.content.Chunk
import ai.dadacast.app.content.Episode
import ai.dadacast.app.content.RedLightPractice
import ai.dadacast.app.content.Sentence
import ai.dadacast.app.practice.PracticeKind
import ai.dadacast.app.speech.VoiceRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondLineTest {
    // --- laying the phrase out as rolling lines ---

    private fun fits(line: String) {
        assertTrue("'$line' too wide", PhraseSegments.width(line) <= PhraseSegments.TITLE_MAX_WIDTH)
    }

    @Test
    fun `a phrase that fits the first line is one line`() {
        assertEquals(listOf("I have a reservation."), PhraseSegments.lines("I have a reservation."))
        assertEquals(listOf("예약했어요"), PhraseSegments.lines("  예약했어요 "))
    }

    @Test
    fun `a longer phrase is cut into evened-out lines that each fit the first line`() {
        val samples = listOf(
            "Yes, I have a reservation under the name Kim.",
            "Could you tell me how long it takes to get to the airport?",
            "I'd like to book a table for four people at seven.",
            "저는 지난 프로젝트에서 데이터 파이프라인을 다시 설계했습니다.",
            "I'm sorry for the short notice, but could we push the meeting back to eleven?",
            "가장 어려웠던 순간은 팀원과 의견이 달랐을 때였고, 저는 먼저 상대의 이야기를 끝까지 들은 뒤 함께 기준을 정했습니다.",
        )
        for (text in samples) {
            val lines = PhraseSegments.lines(text)
            assertTrue(text, lines.size >= 2)
            lines.forEach(::fits)
            // Every word in order, never cut; no line is a lone word.
            assertEquals(text, lines.joinToString(" "))
            assertTrue(text, lines.all { it.contains(' ') })
        }
        assertEquals(listOf("Yes, I have a reservation", "under the name Kim."), PhraseSegments.lines("Yes, I have a reservation under the name Kim."))
    }

    @Test
    fun `the first line takes about 18 Hangul or 28 Latin characters`() {
        fun oneLine(text: String) = PhraseSegments.lines(text).size == 1
        assertTrue(oneLine("가".repeat(9) + " " + "나".repeat(8)))
        assertTrue(!oneLine("가".repeat(9) + " " + "나".repeat(9)))
        assertTrue(oneLine("a".repeat(14) + " " + "b".repeat(14)))
        assertTrue(!oneLine("a".repeat(15) + " " + "b".repeat(14)))
    }

    @Test
    fun `a single word wider than the line is kept whole`() {
        val word = "a".repeat(40)
        assertEquals(listOf("go", word, "now"), PhraseSegments.lines("go $word now").flatMap { it.split(" ") })
    }

    @Test
    fun `blank phrase gives no lines`() {
        assertEquals(emptyList<String>(), PhraseSegments.lines("   "))
    }

    // --- what the media app's second line may show ---

    private val practice = RedLightPractice(
        targetExpression = "under the name ~",
        prompt = "Do you have a reservation?",
        promptLanguage = "en-US",
        sampleAnswer = "Yes, I have a reservation under the name Kim.",
        answerLanguage = "en-US",
        followup = "Perfect.",
        maxSeconds = 30,
    )

    private val episode = Episode(
        id = "e",
        title = "E",
        category = "x",
        chunks = listOf(
            Chunk(
                "c0", "c0", 30,
                listOf(
                    Sentence("설명", "ko-KR", 1_000),
                    Sentence("I have a reservation under the name Kim.", "en-US", 3_000, VoiceRole.DIALOGUE, shadow = true),
                ),
                practice,
            ),
            Chunk("c1", "c1", 30, listOf(Sentence("c1-s0", "ko-KR", 0))),
        ),
    )

    private val machine = PlaybackStateMachine(listOf(episode), settleDelayMs = 500)
    private var lastToken = 0L
    private var lastTimer = 0L

    private fun send(event: PlayerEvent) {
        machine.handle(event).forEach {
            when (it) {
                is PlayerEffect.Speak -> lastToken = it.token
                is PlayerEffect.Listen -> lastToken = it.token
                is PlayerEffect.StartTimer -> lastTimer = it.token
                else -> Unit
            }
        }
    }

    private val line get() = nowPlayingLines(machine.snapshot)

    private val twoLines = listOf("Yes, I have a reservation", "under the name Kim.")

    @Test
    fun `while driving the second line never holds sentence text`() {
        send(PlayerEvent.Play(0))
        assertEquals(NowPlayingLines.Status(R.string.status_playing), line)
        send(PlayerEvent.SpeechDone(lastToken))
        send(PlayerEvent.TimerFired(lastTimer))
        // The repeat-after-me line and its silence: a short cue, not the sentence.
        assertEquals(NowPlayingLines.Status(R.string.status_shadow), line)
        send(PlayerEvent.SpeechDone(lastToken))
        assertEquals(NowPlayingLines.Status(R.string.status_shadow), line)
        send(PlayerEvent.TimerFired(lastTimer))
        // Car moving: no practice at the boundary, plain status again.
        assertEquals(NowPlayingLines.Status(R.string.status_playing), line)
    }

    private fun startPractice() {
        send(PlayerEvent.Play(0))
        send(PlayerEvent.StopChanged(StopContext.RED_LIGHT))
        send(PlayerEvent.SpeechDone(lastToken))
        send(PlayerEvent.TimerFired(lastTimer))
        send(PlayerEvent.SpeechDone(lastToken))
        send(PlayerEvent.TimerFired(lastTimer))
        assertEquals(PlayerStatus.PRACTICE, machine.snapshot.status)
    }

    @Test
    fun `the phrase stays the same over the whole practice`() {
        startPractice()
        // Offer, partner question, listening: the same lines each time.
        val seen = mutableListOf<List<String>>()
        repeat(3) {
            seen += (line as NowPlayingLines.Phrase).lines
            if (machine.snapshot.practice?.listening != true) send(PlayerEvent.SpeechDone(lastToken))
        }
        assertTrue(machine.snapshot.practice!!.listening)
        assertEquals(NowPlayingLines.Phrase(twoLines, R.string.status_listening), line)
        seen.forEach { assertEquals(twoLines, it) }
    }

    @Test
    fun `moving or the practice ending gives the episode title and status back at once`() {
        startPractice()
        assertEquals(twoLines, (line as NowPlayingLines.Phrase).lines)
        send(PlayerEvent.StopChanged(StopContext.NONE))
        assertEquals(NowPlayingLines.Status(R.string.status_playing), line)

    }

    @Test
    fun `a practice that runs out on its own gives the episode title back`() {
        startPractice()
        var guard = 0
        while (machine.snapshot.practice != null && guard++ < 20) {
            assertEquals(twoLines, (line as NowPlayingLines.Phrase).lines)
            if (machine.snapshot.practice!!.listening) send(PlayerEvent.TimerFired(lastTimer)) else send(PlayerEvent.SpeechDone(lastToken))
        }
        assertEquals(null, machine.snapshot.practice)
        assertTrue(line is NowPlayingLines.Status)
    }

    @Test
    fun `a phrase that fits the first line leaves the second for the status`() {
        val short = PracticeView(PracticeKind.RED_LIGHT, "I have a reservation.", listening = false, transcript = null, verdict = null)
        val snap = machine.snapshot.copy(practice = short)
        assertEquals(NowPlayingLines.Phrase(listOf("I have a reservation."), R.string.status_shadow), nowPlayingLines(snap))
        assertEquals(null, (nowPlayingLines(snap) as NowPlayingLines.Phrase).next)
        assertEquals(R.string.status_listening, (nowPlayingLines(snap.copy(practice = short.copy(listening = true))) as NowPlayingLines.Phrase).statusRes)
        assertEquals(R.string.status_checking, (nowPlayingLines(snap.copy(practice = short.copy(checking = true))) as NowPlayingLines.Phrase).statusRes)
        assertEquals(R.string.status_no_mic, (nowPlayingLines(snap.copy(practice = short.copy(listening = true, noMicInput = true))) as NowPlayingLines.Phrase).statusRes)
    }
}

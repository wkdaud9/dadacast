package ai.dadacast.app.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerScorerTest {
    private val sample = "Yes, I have a reservation under the name Kim."
    private val target = "I have a reservation under the name Kim."

    @Test
    fun `recognizer output without punctuation or capitals is a good answer`() {
        assertEquals(Verdict.GOOD, AnswerScorer.verdict("yes i have a reservation under the name kim", sample, target))
    }

    @Test
    fun `a partial answer is close`() {
        assertEquals(Verdict.CLOSE, AnswerScorer.verdict("i have a reservation", sample, target))
    }

    @Test
    fun `an unrelated answer misses and silence is silent`() {
        assertEquals(Verdict.MISS, AnswerScorer.verdict("i would like a large pizza with extra cheese", sample, target))
        assertEquals(Verdict.SILENT, AnswerScorer.verdict(null, sample, target))
        assertEquals(Verdict.SILENT, AnswerScorer.verdict("  ", sample, target))
    }

    @Test
    fun `korean is compared without caring about spaces`() {
        val koSample = "저는 고객 데이터를 바탕으로 서비스를 개선해 온 기획자입니다."
        val koTarget = "저는 ~한 경험을 통해 ~ 역량을 쌓아 온 ~입니다."
        assertEquals(Verdict.GOOD, AnswerScorer.verdict("저는 고객데이터를 바탕으로 서비스를 개선해온 기획자 입니다", koSample, koTarget))
        assertEquals(Verdict.MISS, AnswerScorer.verdict("오늘 날씨가 좋네요", koSample, koTarget))
    }
}

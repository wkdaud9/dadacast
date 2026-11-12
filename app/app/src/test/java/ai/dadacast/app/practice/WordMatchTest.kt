package ai.dadacast.app.practice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordMatchTest {
    @Test
    fun `near misses from the recogniser match, unrelated words do not`() {
        assertTrue(WordMatch.similar("push", "pussie"))
        assertTrue(WordMatch.similar("meeting", "meeting"))
        assertTrue(WordMatch.similar("two", "too"))
        assertFalse(WordMatch.similar("eleven", "goodle"))
        assertFalse(WordMatch.similar("we", "you"))
        assertFalse(WordMatch.similar("a", "i"))
        // Korean two-letter pieces only match exactly.
        assertFalse(WordMatch.similar("가나", "가다"))
    }

    @Test
    fun `alignment is in order and tolerates skipped and extra words`() {
        val target = AnswerScorer.units("Could we push the meeting back to eleven?")
        val heard = AnswerScorer.units("could you pussie the meeting back goodle")
        val said = WordMatch.aligned(target, heard)
        assertEquals(listOf("could", "push", "the", "meeting", "back"), target.filterIndexed { i, _ -> said[i] })
        // Out of order: only the in-order part counts.
        assertEquals(1, WordMatch.aligned(listOf("a", "b"), listOf("b", "a")).count { it })
    }

    @Test
    fun `the real log scores higher with loose matching`() {
        val sample = "Could we push the meeting back to eleven?"
        // Exactly: could, the, meeting, back = 4 of 8. Loosely also push: 5 of 8.
        assertEquals(5.0 / 8, AnswerScorer.score("could you pussie the meeting back goodle", sample, sample), 1e-9)
        assertEquals(Verdict.CLOSE, AnswerScorer.verdict("could you pussie the meeting back goodle", sample, sample))
    }
}

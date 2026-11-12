package ai.dadacast.app.practice

import ai.dadacast.app.playback.PhraseSegments

/**
 * One answer turn, which may span several recognitions. The on-device recogniser ends a
 * recognition at the first short pause (about a second), so "I'm sorry, but … I'm stuck in
 * traffic" came back as just "i'm sorry but". The Pleos STT has no documented setting for that
 * end-of-speech silence, so the turn is held open here instead: when a recognition ends while
 *  - less than [COVERED_AT] of the expected answer has been heard (loosely matched),
 *  - the speaker's voice was heard at some point in the turn, and
 *  - at least [MIN_LEFT_MS] of the [windowMs] window is left,
 * the recogniser is asked again and the new words are added to what was heard, provided either
 * the speaker has not yet reached the last display line of the phrase ([PhraseSegments.lines],
 * the lines the media app shows), or their voice was heard within the last [SILENCE_MS]. So a
 * pause in the middle of a long phrase (or a recogniser lagging behind) never ends the turn; only
 * once the last line is reached does [SILENCE_MS] of quiet end it. Times are in ms from any fixed
 * point. No Android dependency; the recogniser feeds it.
 */
class ListenTurn(
    private val sampleAnswer: String,
    private val targetExpression: String,
    private val windowMs: Long,
    private val startedAt: Long,
    /** False for recorded test speech, which is fed once and never re-requested. */
    private val canContinue: Boolean = true,
) {
    private val segments = mutableListOf<String>()
    private var current: String? = null
    private var lastVoiceAt: Long? = null
    private val lines = PhraseSegments.lines(sampleAnswer)
    /** Furthest display line reached so far; never goes back. */
    var line = 0
        private set

    /** Recognitions in this turn so far; above 1 once the turn was continued. */
    var recognitions = 1
        private set

    /** Everything heard in the turn, earlier recognitions first; null when nothing was heard. */
    val transcript: String?
        get() = (segments + listOfNotNull(current)).joinToString(" ").trim().ifEmpty { null }

    /** Input level from the recogniser; at or above [VOICE_RMS] counts as the speaker talking. */
    fun onLevel(rms: Double, now: Long) {
        if (rms >= VOICE_RMS) lastVoiceAt = now
    }

    /** Latest text of the current recognition (each update replaces the previous one). */
    fun onText(text: String) {
        if (text.isBlank()) return
        current = text.trim()
        line = maxOf(line, PhraseSegments.followLine(lines, transcript))
    }

    /** The speaker is on the last display line or has said most of the answer: quiet may end the turn. */
    fun nearEnd(): Boolean = line >= lines.size - 1 || covered()

    /** The expected answer is mostly there. */
    fun covered(): Boolean {
        val heard = transcript ?: return false
        if (sampleAnswer.isBlank() && targetExpression.isBlank()) return true
        return AnswerScorer.score(heard, sampleAnswer, targetExpression) >= COVERED_AT
    }

    /** Whether a recognition ending now should be followed by another one. */
    fun wouldContinue(now: Long): Boolean {
        if (!canContinue || covered()) return false
        if (now - startedAt > windowMs - MIN_LEFT_MS) return false
        val voice = lastVoiceAt ?: return false
        return !nearEnd() || now - voice < SILENCE_MS
    }

    /** One recognition is over. True means: ask the recogniser again and keep adding to the turn. */
    fun recognitionEnded(now: Long): Boolean {
        current?.let { segments += it }
        current = null
        val again = wouldContinue(now)
        if (again) recognitions++
        return again
    }

    /**
     * In a follow-on recognition, the speaker has been quiet long enough to end the turn. Never
     * before the last display line is reached: then only the window ends it.
     */
    fun silentTooLong(now: Long): Boolean {
        if (recognitions < 2 || !nearEnd()) return false
        val voice = lastVoiceAt ?: return true
        return now - voice >= SILENCE_MS
    }

    companion object {
        /** Share of the expected answer that ends the turn; same bar as a good answer. */
        const val COVERED_AT = AnswerScorer.GOOD_AT
        /** Quiet after speech that ends the turn, once the last display line is reached. */
        const val SILENCE_MS = 2_000L
        /** No new recognition is started this close to the end of the window. */
        const val MIN_LEFT_MS = 1_000L
        /**
         * Level that counts as voice, not room noise. Measured on the emulator: a quiet room
         * reads 10–50 and a breath between words ~15, while speech reaches 400–800 and more.
         */
        const val VOICE_RMS = 100.0
    }
}

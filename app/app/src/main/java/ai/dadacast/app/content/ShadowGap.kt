package ai.dadacast.app.content

/**
 * The silence after a repeat-after-me (`shadow: true`) line must be long enough to say the line
 * back. episodes.json gives every line a fixed 2–4 s `pause_after_sec`, which is too short for a
 * long sentence, so the app stretches it: the gap is the content gap or the time a learner needs
 * to repeat the whole line plus a margin, whichever is longer, never more than [MAX_GAP_MS].
 *
 * All tuning lives here.
 */
object ShadowGap {
    /** A learner repeating English speaks slower than the native TTS voice (about 2.5 words/s). */
    const val ENGLISH_WORDS_PER_SECOND = 2.0
    /** Korean repeat speed, counted in Hangul syllables. */
    const val KOREAN_SYLLABLES_PER_SECOND = 4.0
    /** Time to start speaking after the line ends, and to finish without being cut off. */
    const val MARGIN_MS = 1_500L
    /** A longer silence in a car sounds like the app stopped. */
    const val MAX_GAP_MS = 12_000L

    private val englishWord = Regex("""[A-Za-z0-9]+(?:['’][A-Za-z]+)*""")

    /** Estimated time to say [text] back, in [languageTag] (Korean when it starts with "ko"). */
    fun repeatMs(text: String, languageTag: String): Long {
        val seconds = if (languageTag.startsWith("ko")) {
            text.count { it in '가'..'힣' } / KOREAN_SYLLABLES_PER_SECOND
        } else {
            englishWord.findAll(text).count() / ENGLISH_WORDS_PER_SECOND
        }
        return (seconds * 1000).toLong()
    }

    /**
     * Silence after a line. Non-shadow lines keep [contentGapMs]; shadow lines get at least the
     * repeat time plus [MARGIN_MS], capped at [MAX_GAP_MS] (a longer content gap is kept as is).
     */
    fun gapMs(text: String, languageTag: String, shadow: Boolean, contentGapMs: Long): Long {
        if (!shadow) return contentGapMs
        val needed = (repeatMs(text, languageTag) + MARGIN_MS).coerceAtMost(MAX_GAP_MS)
        return maxOf(contentGapMs, needed)
    }
}

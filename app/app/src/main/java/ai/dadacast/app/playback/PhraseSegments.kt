package ai.dadacast.app.playback

import ai.dadacast.app.practice.AnswerScorer
import ai.dadacast.app.practice.WordMatch

/**
 * Lays a practice phrase over the media app's now-playing view as rolling lines: the first (big)
 * line shows the line to say now, the second previews the next one, and the lines shift up one
 * at a time as the speaker reads (like karaoke, never on a timer). Each line must therefore fit
 * the first line's room, since every line gets there in turn. Widths are in Hangul-syllable
 * units: a Latin letter, digit or punctuation mark counts [NARROW], a space [SPACE].
 *
 * Measured on the emulator's Media Center now-playing screenshot (text column ~1030 px of the
 * 2560 px screen, from the art's right edge to the progress bar's end): a second-line Hangul
 * syllable is ~46 px, a first-line one ~55 px. So the second line takes about 22 Hangul and the
 * first about 18.7; [MAX_WIDTH] and [TITLE_MAX_WIDTH] keep a margin below that. Breaks fall only
 * between words; a single word wider than the line is kept whole.
 */
object PhraseSegments {
    /** Second line (DISPLAY_SUBTITLE): about 20 Hangul or 32 Latin characters. */
    const val MAX_WIDTH = 20.0
    /** First line (TITLE / DISPLAY_TITLE), bigger font: about 18 Hangul or 28 Latin characters. */
    const val TITLE_MAX_WIDTH = 18.0
    /** Latin letters, digits and punctuation: 32 of them per second line. */
    const val NARROW = MAX_WIDTH / 32
    /** A space is narrower than a Latin letter in the media app's font. */
    const val SPACE = 0.35
    /** The line shown moves on once the live transcript holds this share of its words. */
    const val ADVANCE_AT = 0.5
    /** ... or any of its last this-many words (the speaker is at the end of the line). */
    const val LINE_END_WORDS = 2

    fun width(text: String): Double = text.sumOf {
        when {
            it == ' ' -> SPACE
            isWide(it) -> 1.0
            else -> NARROW
        }
    }

    /**
     * The phrase as display lines, each fitting the first line's room, as few as possible and
     * evened out so the last line is not a lone word. Empty for a blank phrase.
     */
    fun lines(text: String): List<String> {
        val words = text.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val greedy = pack(words, 1.0)
        if (greedy.size == 1) return greedy
        var best = greedy
        var scale = 1.0 - STEP
        while (scale > 0.4) {
            val even = pack(words, scale)
            if (even.size != greedy.size) break
            best = even
            scale -= STEP
        }
        return best
    }

    /**
     * The line the speaker is on, given everything [heard] so far. Moves past a line once
     * - [ADVANCE_AT] of its words were said, or
     * - one of its last [LINE_END_WORDS] words was said, or
     * - a word of the next line was said.
     * Words are matched in order and loosely ([WordMatch.aligned]), so skipped or misheard words
     * do not hold it back. Never passes the last line. The caller keeps the highest value seen
     * so the display never goes back while a recognition revises its partial text.
     */
    fun followLine(lines: List<String>, heard: String?): Int {
        if (lines.size < 2 || heard.isNullOrBlank()) return 0
        val perLine = lines.map(AnswerScorer::units)
        val starts = perLine.runningFold(0) { at, units -> at + units.size }
        val said = WordMatch.aligned(perLine.flatten(), AnswerScorer.units(heard))
        fun saidIn(from: Int, to: Int) = (from until to).count { said[it] }
        var index = 0
        while (index < lines.size - 1) {
            val start = starts[index]
            val end = starts[index + 1]
            val size = end - start
            val passed = size == 0 ||
                saidIn(start, end) >= size * ADVANCE_AT - EPSILON ||
                saidIn(maxOf(start, end - LINE_END_WORDS), end) > 0 ||
                saidIn(end, starts[index + 2]) > 0
            if (!passed) break
            index++
        }
        return index
    }

    /** Lines in reading order, each within the first line's room scaled by [scale]. */
    private fun pack(words: List<String>, scale: Double): List<String> {
        val lines = mutableListOf<String>()
        var line = ""
        for (word in words) {
            val room = TITLE_MAX_WIDTH * scale
            val joined = if (line.isEmpty()) word else "$line $word"
            if (line.isEmpty() || width(joined) <= room + EPSILON) {
                line = joined
            } else {
                lines += line
                line = word
            }
        }
        lines += line
        return lines
    }

    private fun isWide(c: Char): Boolean =
        c in 'ᄀ'..'ᇿ' || c in '㄰'..'㆏' || c in '가'..'힣' ||
            c in '一'..'鿿' || c in '぀'..'ヿ' || c in '！'..'｠'

    private val WHITESPACE = Regex("\\s+")
    private const val EPSILON = 1e-9
    private const val STEP = 0.02
}

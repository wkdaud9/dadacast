package ai.dadacast.app.practice

import kotlin.math.max

/**
 * Forgiving word comparison for live speech-to-text, which often gets a word nearly right
 * ("pussie" for "push", "goodle" for "google"). Works on [AnswerScorer.units]: English words, or
 * two-letter pieces of Korean (those only ever match exactly).
 */
object WordMatch {
    /** Normalised edit similarity at or above which two words count as the same. */
    const val SIMILAR_AT = 0.6
    /** Words at least this long that share their first [PREFIX] letters count as the same. */
    const val PREFIX_MIN_LENGTH = 4
    const val PREFIX = 3

    fun similar(a: String, b: String): Boolean {
        if (a == b) return true
        if (a.isEmpty() || b.isEmpty()) return false
        if (a.length >= PREFIX_MIN_LENGTH && b.length >= PREFIX_MIN_LENGTH && a.take(PREFIX) == b.take(PREFIX)) return true
        return 1.0 - levenshtein(a, b).toDouble() / max(a.length, b.length) >= SIMILAR_AT - EPSILON
    }

    /**
     * Which [target] units the [heard] units say, in order: the longest in-order fuzzy match
     * (skipped and extra words are fine). On a tie the earlier target word is used, so a stray
     * common word does not jump ahead.
     */
    fun aligned(target: List<String>, heard: List<String>): BooleanArray {
        val n = target.size
        val m = heard.size
        val out = BooleanArray(n)
        if (n == 0 || m == 0) return out
        val same = Array(n) { i -> BooleanArray(m) { j -> similar(target[i], heard[j]) } }
        // best[i][j]: longest match of target[i..] with heard[j..].
        val best = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            best[i][j] = if (same[i][j]) 1 + best[i + 1][j + 1] else max(best[i + 1][j], best[i][j + 1])
        }
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                same[i][j] && best[i][j] == 1 + best[i + 1][j + 1] -> { out[i] = true; i++; j++ }
                best[i][j + 1] == best[i][j] -> j++
                else -> i++
            }
        }
        return out
    }

    /**
     * How many of [expected] are found in [pool] in any order, each heard unit used once: exact
     * matches first, then near ones. The units found are taken out of [pool].
     */
    fun take(expected: List<String>, pool: MutableList<String>): Int {
        var hits = 0
        val missing = mutableListOf<String>()
        for (unit in expected) {
            if (pool.remove(unit)) hits++ else missing += unit
        }
        for (unit in missing) {
            val k = pool.indexOfFirst { similar(unit, it) }
            if (k >= 0) {
                pool.removeAt(k)
                hits++
            }
        }
        return hits
    }

    private fun levenshtein(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    private const val EPSILON = 1e-9
}

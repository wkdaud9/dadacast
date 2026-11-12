package ai.dadacast.app.practice

import kotlin.math.max

enum class Verdict { GOOD, CLOSE, MISS, SILENT }

/**
 * On-device answer check, no model involved. English is compared word by word, Korean by
 * two-letter pieces so spacing and particles matter less. `~` in a target expression marks a slot
 * the speaker fills freely; only the fixed parts are required.
 */
object AnswerScorer {
    const val GOOD_AT = 0.7
    const val CLOSE_AT = 0.4

    fun verdict(transcript: String?, sampleAnswer: String, targetExpression: String): Verdict {
        if (transcript.isNullOrBlank()) return Verdict.SILENT
        val score = score(transcript, sampleAnswer, targetExpression)
        return when {
            score >= GOOD_AT -> Verdict.GOOD
            score >= CLOSE_AT -> Verdict.CLOSE
            else -> Verdict.MISS
        }
    }

    /** How much of the expected phrase the transcript contains, from 0 to 1. */
    fun score(transcript: String, sampleAnswer: String, targetExpression: String): Double {
        val heard = units(transcript)
        if (heard.isEmpty()) return 0.0
        val target = targetExpression.split('~').flatMap(::units)
        return max(coverage(units(sampleAnswer), heard), coverage(target, heard))
    }

    fun normalize(text: String): String =
        text.lowercase()
            .replace("’", "'")
            .replace(Regex("[^\\p{L}\\p{N}' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun units(text: String): List<String> {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return emptyList()
        return if (normalized.any { it in '가'..'힣' }) {
            val compact = normalized.replace(" ", "")
            if (compact.length < 2) listOf(compact) else compact.windowed(2)
        } else {
            normalized.split(' ')
        }
    }

    private fun coverage(expected: List<String>, heard: List<String>): Double {
        if (expected.isEmpty()) return 0.0
        val pool = heard.groupingBy { it }.eachCount().toMutableMap()
        var hits = 0
        for (unit in expected) {
            val left = pool[unit] ?: 0
            if (left > 0) {
                hits++
                pool[unit] = left - 1
            }
        }
        return hits.toDouble() / expected.size
    }
}

package ai.dadacast.app.playback

import ai.dadacast.app.content.EpisodeParser
import ai.dadacast.app.practice.PracticeScripts
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** How the shipped practice phrases land on the media app's two lines. */
class PracticePhrasePagesTest {
    private val episodes = listOf("src/main/assets/episodes.json", "app/src/main/assets/episodes.json")
        .map(::File).first { it.exists() }.readText().let(EpisodeParser::parse)

    /** Red-light sample answers, plus the (at most two) answers of each parked roleplay. */
    private val phrases = episodes.flatMap { episode ->
        episode.chunks.mapNotNull { it.practice?.sampleAnswer } +
            episode.roleplays.flatMap { rp -> rp.turns.filter { !it.partner }.take(2).map { it.text } }
    }

    @Test
    fun `shipped practice phrases become rolling lines that fit the first line`() {
        val byLines = phrases.groupBy { PhraseSegments.lines(it).size }.toSortedMap()
        println("practice phrases: ${phrases.size}, by line count: ${byLines.mapValues { it.value.size }}")
        phrases.forEach { phrase ->
            PhraseSegments.lines(phrase).forEach {
                assert(PhraseSegments.width(it) <= PhraseSegments.TITLE_MAX_WIDTH || !it.contains(' ')) { "'$it' too wide" }
            }
        }
        byLines.filterKeys { it > 2 }.values.flatten().forEach {
            println("${PhraseSegments.lines(it).size} lines, ${PracticeScripts.listenWindowMs(it) / 1000} s: ${PhraseSegments.lines(it)}")
        }
        assertEquals(LONGER_THAN_TWO_LINES, byLines.filterKeys { it > 2 }.values.sumOf { it.size })
    }

    private companion object {
        /** Phrases (of 92) longer than two lines: they roll and get a longer listening window. */
        const val LONGER_THAN_TWO_LINES = 20
    }
}

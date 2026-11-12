package ai.dadacast.app.content

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeLengthTest {
    @Test
    fun `minutes round down to whole minutes`() {
        assertEquals(32, EpisodeLength.minutes(1953_000L))
        assertEquals(6, EpisodeLength.minutes(392_000L))
        assertEquals(6, EpisodeLength.minutes(363_000L))
        assertEquals(1, EpisodeLength.minutes(60_000L))
    }

    @Test
    fun `short or unknown length still shows one minute`() {
        assertEquals(1, EpisodeLength.minutes(0L))
        assertEquals(1, EpisodeLength.minutes(59_999L))
    }

    @Test
    fun `label reads in plain Korean`() {
        assertEquals("약 32분", EpisodeLength.label(1953_000L))
        assertEquals("약 6분", EpisodeLength.label(374_000L))
    }

    @Test
    fun `bundled episodes, with the stretched shadow silences, come out at about 35 and 7 minutes`() {
        val json = listOf("src/main/assets/episodes.json", "app/src/main/assets/episodes.json")
            .map { java.io.File(it) }.first { it.exists() }.readText()
        val byId = EpisodeParser.parse(json).associate { it.id to EpisodeLength.label(it.estTotalSeconds * 1000L) }
        // Content est_seconds: 1953 s (약 32분) and 392 s (약 6분), before ShadowGap.
        assertEquals("약 35분", byId["ep06_commute_running_late"])
        assertEquals("약 7분", byId["ep01_hotel_checkin"])
    }
}

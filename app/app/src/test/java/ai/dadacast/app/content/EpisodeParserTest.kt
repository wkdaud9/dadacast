package ai.dadacast.app.content

import ai.dadacast.app.speech.VoiceRole
import ai.dadacast.app.speech.Voices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EpisodeParserTest {
    @Test
    fun `splits a line into sentences without breaking abbreviations`() {
        assertEquals(listOf("Good evening.", "Welcome to the hotel."), SentenceSplitter.split("Good evening. Welcome to the hotel."))
        assertEquals(
            listOf("Let me check what's available.", "There's a seat on the eight p.m. flight."),
            SentenceSplitter.split("Let me check what's available. There's a seat on the eight p.m. flight."),
        )
        assertEquals(listOf("첫째, 하나.", "둘째, 둘."), SentenceSplitter.split("첫째, 하나. 둘째, 둘."))
    }

    @Test
    fun `parses the bundled episodes`() {
        val episodes = EpisodeParser.parse(asset().readText())
        assertEquals(6, episodes.size)
        assertEquals(88, episodes.sumOf { it.chunks.size })
        val first = episodes.first()
        assertEquals("ep01_hotel_checkin", first.id)
        assertEquals(9, first.chunks.size)
        assertEquals(392, first.estTotalSeconds)
        val languages = episodes.flatMap { e -> e.chunks.flatMap { c -> c.sentences.map { it.languageTag } } }.toSet()
        assertEquals(setOf("ko-KR", "en-US"), languages)
        assertEquals(43, episodes.single { it.id == "ep06_commute_running_late" }.chunks.size)
    }

    @Test
    fun `only the last sentence of a line carries the pause`() {
        val json = """
            {"episodes":[{"id":"e","title":"T","category":"c","chunks":[
              {"chunk_id":"c1","title":"t","mode":"drive","est_seconds":3,"script":[
                {"speaker":"en_voice","text":"One. Two.","pause_after_sec":3}]},
              {"chunk_id":"c2","title":"t","mode":"stop","est_seconds":3,"script":[
                {"speaker":"narrator_ko","text":"제외.","pause_after_sec":1}]}
            ]}]}
        """
        val chunks = EpisodeParser.parse(json).single().chunks
        assertEquals(listOf("c1"), chunks.map { it.id })
        assertEquals(listOf(0L, 3000L), chunks.single().sentences.map { it.pauseAfterMs })
        assertTrue(chunks.single().sentences.all { it.languageTag == "en-US" })
    }

    private fun asset(): File = listOf("src/main/assets/episodes.json", "app/src/main/assets/episodes.json")
        .map(::File)
        .first { it.exists() }

    @Test
    fun `speakers map to voice roles`() {
        assertEquals(ai.dadacast.app.speech.VoiceRole.DIALOGUE, EpisodeParser.voiceOf("en_voice"))
        assertEquals(ai.dadacast.app.speech.VoiceRole.PARTNER, EpisodeParser.voiceOf("ai_partner"))
        assertEquals(ai.dadacast.app.speech.VoiceRole.NARRATOR, EpisodeParser.voiceOf("narrator_ko"))
        val episodes = EpisodeParser.parse(asset().readText())
        val voices = episodes.flatMap { e -> e.chunks.flatMap { c -> c.sentences.map { it.voice } } }.toSet()
        assertEquals(VoiceRole.values().toSet(), voices)
    }

    @Test
    fun `an English partner line in a drive chunk gets the English partner voice`() {
        val json = """
            {"episodes":[{"id":"e","title":"T","category":"c","chunks":[
              {"chunk_id":"c1","title":"t","mode":"drive","est_seconds":3,"script":[
                {"speaker":"en_voice","text":"Hi, Grace.","pause_after_sec":1},
                {"speaker":"ai_partner","text":"Sure, Minho. What's up?","pause_after_sec":1},
                {"speaker":"ai_partner","text":"좋아요, 시작할게요.","pause_after_sec":1}]}
            ]}]}
        """
        val sentences = EpisodeParser.parse(json).single().chunks.single().sentences
        val speakers = sentences.map { Voices.speakerFor(it.voice, it.languageTag) }
        assertEquals(
            listOf(Voices.DIALOGUE_EN, Voices.PARTNER_EN, Voices.PARTNER_EN, Voices.PARTNER_KO),
            speakers,
        )
        assertEquals(listOf("en-US", "en-US", "en-US", "ko-KR"), sentences.map { it.languageTag })
    }

    @Test
    fun `partner language follows the text, other speakers follow their name`() {
        assertEquals("en-US", EpisodeParser.languageOf("ai_partner", "No worries. Drive safely.", "ko-KR"))
        assertEquals("ko-KR", EpisodeParser.languageOf("ai_partner", "네, 좋습니다.", "ko-KR"))
        assertEquals("en-US", EpisodeParser.languageOf("en_voice", "Hi.", "ko-KR"))
        assertEquals("ko-KR", EpisodeParser.languageOf("narrator_ko", "Pleos", "en-US"))
    }

    @Test
    fun `in the long commute episode the other party and Minho speak with different voices`() {
        val episode = EpisodeParser.parse(asset().readText()).single { it.id == "ep06_commute_running_late" }
        val scene = episode.chunks.single { it.id == "ep06_c05" }.sentences
        val byText = scene.associate { it.text to Voices.speakerFor(it.voice, it.languageTag) }
        assertEquals(Voices.DIALOGUE_EN, byText["Is now a good time?"])
        assertEquals(Voices.PARTNER_EN, byText["What's up?"])
        val english = episode.chunks.flatMap { it.sentences }.filter { it.languageTag == "en-US" }
        assertTrue(english.all { it.voice != VoiceRole.NARRATOR })
        assertTrue(episode.chunks.flatMap { it.sentences }.filter { it.languageTag == "ko-KR" }.all { it.voice == VoiceRole.NARRATOR })
    }

    @Test
    fun `each role has its own speaker and Korean narration differs from the partner`() {
        val v = ai.dadacast.app.speech.Voices
        assertEquals(v.DIALOGUE_EN, v.speakerFor(ai.dadacast.app.speech.VoiceRole.DIALOGUE, "en-US"))
        assertEquals(v.PARTNER_EN, v.speakerFor(ai.dadacast.app.speech.VoiceRole.PARTNER, "en-US"))
        assertEquals(v.NARRATOR_KO, v.speakerFor(ai.dadacast.app.speech.VoiceRole.NARRATOR, "ko-KR"))
        assertTrue(
            v.speakerFor(ai.dadacast.app.speech.VoiceRole.NARRATOR, "ko-KR") !=
                v.speakerFor(ai.dadacast.app.speech.VoiceRole.PARTNER, "ko-KR")
        )
        assertTrue(v.DIALOGUE_EN != v.PARTNER_EN)
    }
}

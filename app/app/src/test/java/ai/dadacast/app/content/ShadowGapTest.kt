package ai.dadacast.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadowGapTest {
    @Test
    fun `short English line keeps a content gap that is already long enough`() {
        // 3 words: 1.5 s to repeat + 1.5 s margin = 3.0 s, under the 4 s in the content.
        assertEquals(4_000L, ShadowGap.gapMs("Is breakfast included?", "en-US", shadow = true, contentGapMs = 4_000L))
        // Same line with a 2 s content gap is stretched to 3 s.
        assertEquals(3_000L, ShadowGap.gapMs("Is breakfast included?", "en-US", shadow = true, contentGapMs = 2_000L))
    }

    @Test
    fun `long English line gets time to say it back`() {
        val line = "Could you tell me what time breakfast starts and whether it is included in my room rate?"
        // 17 words / 2.0 = 8.5 s + 1.5 s.
        assertEquals(10_000L, ShadowGap.gapMs(line, "en-US", shadow = true, contentGapMs = 4_000L))
    }

    @Test
    fun `contractions count as one English word`() {
        assertEquals(1_500L, ShadowGap.repeatMs("I'd like that.", "en-US"))
    }

    @Test
    fun `Korean line is timed by Hangul syllables`() {
        // 12 syllables / 4.0 = 3.0 s + 1.5 s.
        assertEquals(4_500L, ShadowGap.gapMs("제가 이 직무에 지원한 이유는", "ko-KR", shadow = true, contentGapMs = 3_000L))
        // Short Korean line: 5 syllables -> 1.25 + 1.5 = 2.75 s, content 3 s wins.
        assertEquals(3_000L, ShadowGap.gapMs("안녕하세요.", "ko-KR", shadow = true, contentGapMs = 3_000L))
    }

    @Test
    fun `very long line is capped`() {
        val line = "가".repeat(80) // 20 s to repeat
        assertEquals(ShadowGap.MAX_GAP_MS, ShadowGap.gapMs(line, "ko-KR", shadow = true, contentGapMs = 4_000L))
        val words = List(40) { "word" }.joinToString(" ")
        assertEquals(ShadowGap.MAX_GAP_MS, ShadowGap.gapMs(words, "en-US", shadow = true, contentGapMs = 2_000L))
    }

    @Test
    fun `non-shadow lines keep the content gap`() {
        val line = "Could you tell me what time breakfast starts and whether it is included in my room rate?"
        assertEquals(1_000L, ShadowGap.gapMs(line, "en-US", shadow = false, contentGapMs = 1_000L))
        assertEquals(0L, ShadowGap.gapMs("가".repeat(80), "ko-KR", shadow = false, contentGapMs = 0L))
    }

    @Test
    fun `parser stretches only the shadow gap and adds the extra time to the chunk estimate`() {
        val json = """
            {"episodes":[{"id":"e","title":"T","category":"c","chunks":[
              {"chunk_id":"c1","title":"t","mode":"drive","est_seconds":30,"script":[
                {"speaker":"narrator_ko","text":"따라 해 보세요.","pause_after_sec":1},
                {"speaker":"en_voice","text":"Hi. Could you tell me what time breakfast starts and whether it is included in my room rate?","pause_after_sec":4,"shadow":true},
                {"speaker":"en_voice","text":"Could you tell me what time breakfast starts and whether it is included in my room rate?","pause_after_sec":1},
                {"speaker":"narrator_ko","text":"제가 이 직무에 지원한 이유는","pause_after_sec":3,"shadow":true}]},
              {"chunk_id":"c2","title":"t","mode":"drive","est_seconds":20,"script":[
                {"speaker":"en_voice","text":"Is breakfast included?","pause_after_sec":4,"shadow":true}]}
            ]}]}
        """
        val episode = EpisodeParser.parse(json).single()
        val first = episode.chunks[0]
        // "Hi." + 17 words = 18 words: 9.0 + 1.5 = 10.5 s, only on the line's last sentence.
        assertEquals(listOf(1_000L, 0L, 10_500L, 1_000L, 4_500L), first.sentences.map { it.pauseAfterMs })
        // +6.5 s and +1.5 s = 8 s more than the content's 30 s.
        assertEquals(38, first.estSeconds)
        assertEquals(20, episode.chunks[1].estSeconds)
        assertEquals(58, episode.estTotalSeconds)
    }

    @Test
    fun `bundled episodes grow by the added shadow silence`() {
        val json = listOf("src/main/assets/episodes.json", "app/src/main/assets/episodes.json")
            .map { java.io.File(it) }.first { it.exists() }.readText()
        val contentTotals = org.json.JSONObject(json).getJSONArray("episodes").let { arr ->
            (0 until arr.length()).associate { i ->
                val ep = arr.getJSONObject(i)
                val chunks = ep.getJSONArray("chunks")
                ep.getString("id") to (0 until chunks.length()).map { chunks.getJSONObject(it) }
                    .filter { it.getString("mode") == "drive" }.sumOf { it.getInt("est_seconds") }
            }
        }
        val episodes = EpisodeParser.parse(json)
        for (episode in episodes) {
            val hasShadow = episode.chunks.any { c -> c.sentences.any { it.shadow } }
            val content = contentTotals.getValue(episode.id)
            if (hasShadow) {
                assertTrue("${episode.id} did not grow", episode.estTotalSeconds > content)
            } else {
                assertEquals(content, episode.estTotalSeconds)
            }
        }
        // Every shadow gap is at least its repeat time plus margin, or the cap.
        episodes.flatMap { e -> e.chunks.flatMap { it.sentences } }.filter { it.shadow && it.pauseAfterMs > 0 }
            .forEach { assertTrue(it.text, it.pauseAfterMs in 2_000L..ShadowGap.MAX_GAP_MS) }
    }
}

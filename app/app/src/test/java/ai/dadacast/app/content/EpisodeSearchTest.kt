package ai.dadacast.app.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeSearchTest {
    private fun ep(id: String, title: String, category: String) =
        Episode(id, title, category, listOf(Chunk("c", "c", 1, listOf(Sentence("s", "ko-KR", 0)))))

    private val episodes = listOf(
        ep("a", "영어 회화 A2: 호텔 체크인", "english"),
        ep("b", "영어 회화 B1: 공항에서 비행 지연 대응", "english"),
        ep("c", "면접 리허설: 직무 면접 자주 나오는 질문 3개", "interview"),
    )
    private val series = { c: String -> if (c == "interview") "면접 연습" else "영어 회화" }

    @Test
    fun `title fragment matches`() {
        assertEquals(0, EpisodeSearch.find("호텔 체크인", episodes, series))
        assertEquals(1, EpisodeSearch.find("공항", episodes, series))
    }

    @Test
    fun `words match across title and series`() {
        assertEquals(2, EpisodeSearch.find("면접 연습 틀어줘", episodes, series))
    }

    @Test
    fun `no match or empty query gives null`() {
        assertNull(EpisodeSearch.find("날씨", episodes, series))
        assertNull(EpisodeSearch.find("", episodes, series))
        assertNull(EpisodeSearch.find(null, episodes, series))
    }
}

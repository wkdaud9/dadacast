package ai.dadacast.app.playback

import ai.dadacast.app.content.Chunk
import ai.dadacast.app.content.Episode
import ai.dadacast.app.content.Sentence
import ai.dadacast.app.ux.UxLimits
import ai.dadacast.app.ux.UxRestrictionsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseTreeTest {
    private fun episode(id: String, category: String, title: String = "Episode $id") = Episode(
        id = id,
        title = title,
        category = category,
        chunks = listOf(Chunk("$id-c", "c", 60, listOf(Sentence("s", "ko-KR", 0)))),
    )

    private val episodes = listOf(
        episode("a", "english"),
        episode("b", "english"),
        episode("c", "english"),
        episode("d", "interview"),
    )

    private val categoryTitle = { category: String -> "[$category]" }

    private fun children(parent: String, limits: UxLimits) =
        BrowseTree.children(parent, episodes, limits, categoryTitle)

    @Test
    fun `root holds browsable categories in content order`() {
        val root = children(BrowseTree.ROOT_ID, UxLimits())
        assertEquals(listOf("english", "interview"), root.map { it.id })
        assertEquals(listOf("[english]", "[interview]"), root.map { it.title })
        assertTrue(root.none { it.playable })
    }

    @Test
    fun `categories hold playable episodes with durations`() {
        val english = children("english", UxLimits())
        assertEquals(listOf("a", "b", "c"), english.map { it.id })
        assertTrue(english.all { it.playable && it.durationMs == 60_000L })
        assertTrue(english.all { it.subtitle == "약 1분" })
        assertTrue(children(BrowseTree.ROOT_ID, UxLimits()).all { it.subtitle.isEmpty() })
        assertEquals(4, children(BrowseTree.ALL_EPISODES_ID, UxLimits()).size)
    }

    @Test
    fun `parked state leaves the tree untouched`() {
        val limits = UxLimits.from(false, UxLimits.LIMIT_CONTENT or UxLimits.LIMIT_STRING_LENGTH, 3, 1)
        assertEquals(3, children("english", limits).size)
        assertEquals("Episode a", children("english", limits).first().title)
    }

    @Test
    fun `content limit caps every node`() {
        val limits = UxLimits.from(true, UxLimits.LIMIT_CONTENT, 120, 2)
        assertEquals(listOf("a", "b"), children("english", limits).map { it.id })
        assertEquals(2, children(BrowseTree.ALL_EPISODES_ID, limits).size)
        assertEquals(2, children(BrowseTree.ROOT_ID, limits).size)
    }

    @Test
    fun `string limit shortens titles`() {
        val limits = UxLimits.from(true, UxLimits.LIMIT_STRING_LENGTH, 7, 21)
        assertEquals("Episode…", children("english", limits).first().title)
        assertEquals(3, children("english", limits).size)
    }

    @Test
    fun `every limit-dependent parent is listed for re-notification`() {
        assertEquals(
            listOf(BrowseTree.ROOT_ID, BrowseTree.ALL_EPISODES_ID, "english", "interview"),
            BrowseTree.parentIds(episodes),
        )
    }

    @Test
    fun `shared state starts restricted and resets to restricted`() {
        UxRestrictionsState.reset()
        assertFalse(UxRestrictionsState.limits.value.showDetails)
        UxRestrictionsState.update(UxLimits())
        assertTrue(UxRestrictionsState.limits.value.showDetails)
        UxRestrictionsState.reset()
        assertEquals(UxRestrictionsState.RESTRICTED, UxRestrictionsState.limits.value)
    }
}

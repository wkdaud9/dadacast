package ai.dadacast.app.playback

import ai.dadacast.app.content.Episode
import ai.dadacast.app.ux.UxLimits

data class BrowseNode(
    val id: String,
    val title: String,
    val playable: Boolean,
    val durationMs: Long = 0L,
    /** Series the node belongs to; picks its art. */
    val category: String = "",
)

/**
 * The media browse tree, restricted by the current UX limits. The vehicle Media Center shows the
 * root's children as tabs, so the root holds browsable categories and episodes sit one level down.
 */
object BrowseTree {
    const val ROOT_ID = "root"

    /** Flat list for the app's own screen. Not linked from the root. */
    const val ALL_EPISODES_ID = "all_episodes"

    fun children(
        parentId: String,
        episodes: List<Episode>,
        limits: UxLimits,
        categoryTitle: (String) -> String,
    ): List<BrowseNode> {
        val nodes = when (parentId) {
            ROOT_ID -> episodes.map { it.category }.distinct().map { category ->
                BrowseNode(category, categoryTitle(category), playable = false, category = category)
            }
            ALL_EPISODES_ID -> episodes.map(::episodeNode)
            else -> episodes.filter { it.category == parentId }.map(::episodeNode)
        }
        return limits.limitList(nodes).map { it.copy(title = limits.limitTitle(it.title)) }
    }

    /** Every node whose children depend on the limits, for re-notifying browsers after a change. */
    fun parentIds(episodes: List<Episode>): List<String> =
        listOf(ROOT_ID, ALL_EPISODES_ID) + episodes.map { it.category }.distinct()

    private fun episodeNode(episode: Episode) =
        BrowseNode(episode.id, episode.title, playable = true, durationMs = episode.estTotalSeconds * 1000L, category = episode.category)
}

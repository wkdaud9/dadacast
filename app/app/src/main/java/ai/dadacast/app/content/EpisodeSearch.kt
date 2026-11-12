package ai.dadacast.app.content

/** Voice and media-app search: a simple match over episode titles and series names. */
object EpisodeSearch {
    /** Index of the best match, or null when nothing matches (or the query is empty). */
    fun find(query: String?, episodes: List<Episode>, seriesTitle: (String) -> String = { "" }): Int? {
        val q = normalize(query ?: return null)
        if (q.isEmpty()) return null
        episodes.indexOfFirst { normalize(it.title).contains(q) }.takeIf { it >= 0 }?.let { return it }
        val words = q.split(' ').filter { it.length >= 2 }
        val best = episodes.withIndex().maxByOrNull { (_, e) ->
            val text = normalize(e.title + " " + seriesTitle(e.category))
            words.count { text.contains(it) }
        } ?: return null
        val hits = words.count { normalize(best.value.title + " " + seriesTitle(best.value.category)).contains(it) }
        return if (hits > 0) best.index else null
    }

    private fun normalize(text: String) =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
}

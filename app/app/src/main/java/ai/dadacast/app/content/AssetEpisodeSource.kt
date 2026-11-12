package ai.dadacast.app.content

import android.content.Context

class AssetEpisodeSource(private val context: Context) : EpisodeSource {
    override fun load(): List<Episode> =
        context.assets.open(ASSET).bufferedReader().use { EpisodeParser.parse(it.readText()) }

    private companion object {
        const val ASSET = "episodes.json"
    }
}

package ai.dadacast.app.content

import ai.dadacast.app.speech.VoiceRole

/** The unit a sudden stop can land on. One script line may hold several sentences. */
data class Sentence(
    val text: String,
    val languageTag: String,
    /** Silence after this sentence; only the last sentence of a line carries `pause_after_sec`. */
    val pauseAfterMs: Long,
    val voice: VoiceRole = VoiceRole.NARRATOR,
)

data class Chunk(
    val id: String,
    val title: String,
    val estSeconds: Int,
    val sentences: List<Sentence>,
    /** Short speaking practice offered at this chunk's end while the car is stopped. */
    val practice: RedLightPractice? = null,
)

/** `red_light_practice`: one prompt, one answer, one reply. */
data class RedLightPractice(
    val targetExpression: String,
    val prompt: String,
    val promptLanguage: String,
    val sampleAnswer: String,
    val answerLanguage: String,
    val followup: String,
    val maxSeconds: Int,
)

/** `deferred_roleplays`: a longer scripted exchange, only when parked. */
data class Roleplay(
    val id: String,
    val title: String,
    val intro: List<Sentence>,
    val turns: List<RoleplayTurn>,
)

data class RoleplayTurn(val partner: Boolean, val text: String, val languageTag: String)

data class Episode(
    val id: String,
    val title: String,
    val category: String,
    val chunks: List<Chunk>,
    val roleplays: List<Roleplay> = emptyList(),
) {
    val estTotalSeconds: Int get() = chunks.sumOf { it.estSeconds }
}

/** Seam for future episode origins (user material, generated scripts). */
fun interface EpisodeSource {
    fun load(): List<Episode>
}

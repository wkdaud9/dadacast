package ai.dadacast.app.content

import ai.dadacast.app.speech.VoiceRole
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses content/episodes.json (schema 0.2): the linear drive chunks, plus the stopped-car
 * speaking practice (red_light_practice per chunk, deferred_roleplays per episode).
 */
object EpisodeParser {
    private const val MODE_DRIVE = "drive"
    private const val KOREAN = "ko-KR"
    private const val ENGLISH = "en-US"

    fun parse(json: String): List<Episode> {
        val episodes = JSONObject(json).getJSONArray("episodes")
        return List(episodes.length()) { parseEpisode(episodes.getJSONObject(it)) }
    }

    private fun parseEpisode(obj: JSONObject): Episode {
        val narration = obj.optJSONObject("language")?.optString("narration")
            ?.takeIf { it.isNotEmpty() } ?: KOREAN
        val chunks = obj.getJSONArray("chunks").objects()
            .filter { it.getString("mode") == MODE_DRIVE }
            .map { parseChunk(it, narration) }
            .filter { it.sentences.isNotEmpty() }
        val target = obj.optJSONObject("language")?.optString("target")?.takeIf { it.isNotEmpty() } ?: narration
        return Episode(
            id = obj.getString("id"),
            title = obj.getString("title"),
            category = obj.optString("category"),
            chunks = chunks,
            roleplays = obj.optJSONArray("deferred_roleplays")?.objects().orEmpty()
                .map { parseRoleplay(it, narration, target) },
        )
    }

    private fun parsePractice(obj: JSONObject): RedLightPractice = RedLightPractice(
        targetExpression = obj.getString("target_expression"),
        prompt = obj.getString("prompt_ko_or_en"),
        promptLanguage = languageOfText(obj.getString("prompt_ko_or_en")),
        sampleAnswer = obj.getString("sample_user_answer"),
        answerLanguage = languageOfText(obj.getString("sample_user_answer")),
        followup = obj.getString("ai_followup"),
        maxSeconds = obj.optInt("max_seconds", 30),
    )

    private fun parseRoleplay(obj: JSONObject, narration: String, target: String): Roleplay {
        // The scripted intro mentions a hint command that needs the conversation model; it is
        // left out until that model exists.
        val intro = obj.getJSONArray("script").objects()
            .filter { it.getString("speaker").endsWith("_ko") && !it.getString("text").contains("힌트") }
            .map { Sentence(it.getString("text"), narration, 0L) }
        val turns = obj.getJSONObject("roleplay").getJSONArray("sample_turns").objects().map {
            RoleplayTurn(partner = it.getString("role") == "ai_partner", text = it.getString("text"), languageTag = target)
        }
        return Roleplay(obj.getString("roleplay_id"), obj.getString("title"), intro, turns)
    }

    fun languageOfText(text: String): String = if (text.any { it in '가'..'힣' }) KOREAN else ENGLISH

    private fun parseChunk(obj: JSONObject, narration: String): Chunk = Chunk(
        id = obj.getString("chunk_id"),
        title = obj.getString("title"),
        estSeconds = obj.getInt("est_seconds"),
        sentences = obj.getJSONArray("script").objects().flatMap { line ->
            sentencesOf(
                text = line.getString("text"),
                languageTag = languageOf(line.getString("speaker"), line.getString("text"), narration),
                pauseAfterMs = line.optDouble("pause_after_sec", 0.0).toSecondsMs(),
                voice = voiceOf(line.getString("speaker")),
            )
        },
        practice = obj.optJSONObject("red_light_practice")?.let(::parsePractice),
    )

    /**
     * `ai_partner` has no language suffix: in a drive chunk it voices the other party of an
     * English dialogue (finley) or a Korean partner line (haeun), so its language comes from the text.
     */
    fun languageOf(speaker: String, text: String, narration: String): String = when {
        speaker.startsWith("en_") -> ENGLISH
        speaker.endsWith("_ko") -> KOREAN
        speaker == "ai_partner" -> languageOfText(text)
        else -> narration
    }

    private fun sentencesOf(text: String, languageTag: String, pauseAfterMs: Long, voice: VoiceRole): List<Sentence> {
        val parts = SentenceSplitter.split(text)
        return parts.mapIndexed { i, part ->
            Sentence(part, languageTag, if (i == parts.lastIndex) pauseAfterMs else 0L, voice)
        }
    }

    /** `narrator_ko` is 다다, `en_voice` is model dialogue, `ai_partner` is the practice partner. */
    fun voiceOf(speaker: String): VoiceRole = when (speaker) {
        "en_voice" -> VoiceRole.DIALOGUE
        "ai_partner" -> VoiceRole.PARTNER
        else -> VoiceRole.NARRATOR
    }

    private fun Double.toSecondsMs(): Long = (this * 1000).toLong()

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
}

object SentenceSplitter {
    // A terminator only ends a sentence when a capital or Hangul follows, so "eight p.m. flight"
    // stays whole.
    private val boundary = Regex("""(?<=[.!?])\s+(?=[A-Z가-힣"'])""")

    fun split(text: String): List<String> =
        text.trim().split(boundary).map { it.trim() }.filter { it.isNotEmpty() }
}

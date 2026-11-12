package ai.dadacast.app.practice

import ai.dadacast.app.content.RedLightPractice
import ai.dadacast.app.content.Roleplay
import ai.dadacast.app.speech.VoiceRole

enum class PracticeKind { RED_LIGHT, ROLEPLAY }

/** One step of a stopped-car practice: say a line, or listen for an answer. */
sealed interface PracticeStep {
    data class Say(val text: String, val languageTag: String, val voice: VoiceRole = VoiceRole.NARRATOR) : PracticeStep

    data class Listen(
        val sampleAnswer: String,
        val targetExpression: String,
        val languageTag: String,
        val timeoutMs: Long,
        /** Reply after a good answer, from the content. */
        val followup: String?,
        val followupLanguage: String,
        /** A silent or missed answer in a roleplay ends the exchange. */
        val endOnMiss: Boolean,
    ) : PracticeStep
}

data class PracticeScript(
    val kind: PracticeKind,
    /** Shown on screen while practising. */
    val targetPhrase: String,
    val steps: List<PracticeStep>,
)

/** Turns content into practice steps. Spoken wording lives here so it stays plain and in one place. */
object PracticeScripts {
    const val KOREAN = "ko-KR"
    const val LISTEN_MS = 8_000L

    const val OFFER_RED_LIGHT = "신호 대기 중이에요. 방금 표현을 한 번 말해 볼까요?"
    const val OFFER_ROLEPLAY = "차를 세워 두셨네요. 짧은 대화 연습을 해 볼게요. 대답하지 않으셔도 괜찮아요."
    const val GOOD = "좋아요!"
    const val CLOSE = "거의 맞았어요. 이렇게 말해 보세요."
    const val MISS = "이렇게 말해 볼 수 있어요."
    const val SILENT = "괜찮아요. 이어서 들을게요."
    const val DONE = "그럼 이어서 들을게요."

    fun redLight(practice: RedLightPractice) = PracticeScript(
        kind = PracticeKind.RED_LIGHT,
        targetPhrase = practice.sampleAnswer,
        steps = listOf(
            PracticeStep.Say(OFFER_RED_LIGHT, KOREAN),
            PracticeStep.Say(practice.prompt, practice.promptLanguage, VoiceRole.PARTNER),
            PracticeStep.Listen(
                sampleAnswer = practice.sampleAnswer,
                targetExpression = practice.targetExpression,
                languageTag = practice.answerLanguage,
                timeoutMs = minOf(LISTEN_MS, practice.maxSeconds * 1_000L),
                followup = practice.followup,
                followupLanguage = practice.promptLanguage,
                endOnMiss = false,
            ),
            PracticeStep.Say(DONE, KOREAN),
        ),
    )

    /** At most [maxAnswers] answers; the partner's lines in between are scripted. */
    fun roleplay(roleplay: Roleplay, maxAnswers: Int = 2): PracticeScript {
        val steps = mutableListOf<PracticeStep>(PracticeStep.Say(OFFER_ROLEPLAY, KOREAN))
        roleplay.intro.forEach { steps += PracticeStep.Say(it.text, it.languageTag, it.voice) }
        var answers = 0
        for (turn in roleplay.turns) {
            if (turn.partner) {
                steps += PracticeStep.Say(turn.text, turn.languageTag, VoiceRole.PARTNER)
            } else {
                if (answers == maxAnswers) break
                answers++
                steps += PracticeStep.Listen(
                    sampleAnswer = turn.text,
                    targetExpression = turn.text,
                    languageTag = turn.languageTag,
                    timeoutMs = LISTEN_MS,
                    followup = null,
                    followupLanguage = turn.languageTag,
                    endOnMiss = true,
                )
            }
        }
        // The last partner line after the final answer is kept; anything after is dropped by break.
        steps += PracticeStep.Say(DONE, KOREAN)
        val firstAnswer = roleplay.turns.firstOrNull { !it.partner }?.text.orEmpty()
        return PracticeScript(PracticeKind.ROLEPLAY, firstAnswer, steps)
    }
}

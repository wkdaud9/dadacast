package ai.dadacast.app.practice

import ai.dadacast.app.speech.VoiceRole

/**
 * Decides what to say after an answer. This is the seam for a conversation model later; the
 * current coach scores on the device and uses fixed replies.
 */
fun interface PracticeCoach {
    fun reply(listen: PracticeStep.Listen, transcript: String?): CoachReply
}

data class CoachReply(
    val verdict: Verdict,
    val lines: List<PracticeStep.Say>,
    /** True when the rest of the practice should be skipped. */
    val endPractice: Boolean,
)

class LocalCoach : PracticeCoach {
    override fun reply(listen: PracticeStep.Listen, transcript: String?): CoachReply {
        val verdict = AnswerScorer.verdict(transcript, listen.sampleAnswer, listen.targetExpression)
        val ko = PracticeScripts.KOREAN
        val model = PracticeStep.Say(listen.sampleAnswer, listen.languageTag, VoiceRole.DIALOGUE)
        return when (verdict) {
            Verdict.GOOD -> CoachReply(
                verdict,
                listOfNotNull(
                    PracticeStep.Say(PracticeScripts.GOOD, ko),
                    listen.followup?.let { PracticeStep.Say(it, listen.followupLanguage, VoiceRole.PARTNER) },
                ),
                endPractice = false,
            )
            Verdict.CLOSE -> CoachReply(verdict, listOf(PracticeStep.Say(PracticeScripts.CLOSE, ko), model), false)
            Verdict.MISS -> CoachReply(
                verdict,
                listOfNotNull(
                    PracticeStep.Say(PracticeScripts.MISS, ko),
                    model,
                    PracticeStep.Say(PracticeScripts.SILENT, ko).takeIf { listen.endOnMiss },
                ),
                endPractice = listen.endOnMiss,
            )
            Verdict.SILENT -> CoachReply(verdict, listOf(PracticeStep.Say(PracticeScripts.SILENT, ko)), endPractice = true)
        }
    }
}

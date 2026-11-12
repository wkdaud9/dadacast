package ai.dadacast.app.speech

/** Who is talking. Each role gets its own voice so the driver can tell them apart. */
enum class VoiceRole {
    /** 다다, the host who explains in Korean. */
    NARRATOR,
    /** Model dialogue and phrases to repeat. */
    DIALOGUE,
    /** The conversation partner in a speaking practice, and the other party in a drive-chunk English dialogue. */
    PARTNER,
}

/** Pleos TextToSpeech speaker names (documented under setSpeakerType). Change voices here. */
object Voices {
    /** 다다. Chosen to differ from the navigation guidance voice. */
    const val NARRATOR_KO = "sanghyeok"
    /** Korean partner lines (interview practice), distinct from 다다. */
    const val PARTNER_KO = "haeun"
    const val DIALOGUE_EN = "lyra"
    const val PARTNER_EN = "finley"

    fun speakerFor(role: VoiceRole, languageTag: String): String {
        val korean = languageTag.startsWith("ko")
        return when (role) {
            VoiceRole.NARRATOR -> if (korean) NARRATOR_KO else DIALOGUE_EN
            VoiceRole.DIALOGUE -> if (korean) PARTNER_KO else DIALOGUE_EN
            VoiceRole.PARTNER -> if (korean) PARTNER_KO else PARTNER_EN
        }
    }
}

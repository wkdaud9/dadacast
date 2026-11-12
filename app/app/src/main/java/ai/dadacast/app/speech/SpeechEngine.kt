package ai.dadacast.app.speech

/** Seam between playback and a speech backend. All callbacks arrive on the main thread. */
interface SpeechEngine {
    val name: String

    interface Listener {
        fun onSpeechDone(token: Long)
        fun onSpeechFailed(token: Long, reason: String)
    }

    fun start(listener: Listener)

    /** Replaces anything pending. May be called before the engine is ready. */
    /** [speaker] is a Pleos speaker name from [Voices]. */
    fun speak(token: Long, text: String, languageTag: String, speaker: String)

    fun stop()

    fun release()
}

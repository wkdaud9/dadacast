package ai.dadacast.app.speech

/** Seam between practice and a speech recognizer. Callbacks arrive on the main thread. */
interface SpeechRecognizerEngine {
    interface Listener {
        /** Null when nothing usable was heard. */
        fun onHeard(token: Long, transcript: String?)

        /** The speaker stopped; the final text is still being worked out. */
        fun onSpeechEnded(token: Long) {}

        /** Sound above the noise floor reached the recogniser in this window. */
        fun onInputDetected(token: Long) {}
    }

    fun start(listener: Listener)

    fun listen(token: Long, languageTag: String)

    fun stop()

    fun release()
}

/**
 * Test hook: when set, the next listen reads this recorded speech (16 kHz, 16-bit, mono PCM)
 * instead of the microphone. Only debug builds set it.
 */
object SpeechInputOverride {
    @Volatile
    var nextUtterance: ByteArray? = null
}

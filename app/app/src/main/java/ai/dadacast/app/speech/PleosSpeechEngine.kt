package ai.dadacast.app.speech

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

/** Pleos TextToSpeech in ON_DEVICE mode: no network, no app registration, audio stays in the car. */
class PleosSpeechEngine(context: Context) : SpeechEngine {
    override val name = "Pleos TextToSpeech (ON_DEVICE)"

    private data class Request(val token: Long, val text: String, val languageTag: String, val speaker: String)

    private val main = Handler(Looper.getMainLooper())
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private var listener: SpeechEngine.Listener? = null
    private var ready = false
    private var failure: String? = null
    private var current: Request? = null
    private var pending: Request? = null

    /** The SDK has no utterance ids, so a new request waits until a stop has been confirmed. */
    private var stopping = false
    private val stopTimeout = Runnable { onStopConfirmed() }

    private val events = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = onMain { finishCurrent(null) }
        override fun onStop() = onMain { onStopConfirmed() }
        override fun onError(errorType: ErrorType) = onMain {
            Log.w(TAG, "tts error $errorType")
            finishCurrent(errorType.name)
        }
    }

    override fun start(listener: SpeechEngine.Listener) {
        this.listener = listener
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() = onMain {
                Log.i(TAG, "tts ready")
                tts.addEventListener(events)
                ready = true
                dispatchPending()
            }

            override fun onFailure(errorType: ErrorType) = onMain {
                Log.e(TAG, "tts init failed $errorType")
                failure = errorType.name
                pending?.let { request ->
                    pending = null
                    listener.onSpeechFailed(request.token, errorType.name)
                }
            }
        })
    }

    override fun speak(token: Long, text: String, languageTag: String, speaker: String) {
        val request = Request(token, text, languageTag, speaker)
        failure?.let {
            main.post { listener?.onSpeechFailed(token, it) }
            return
        }
        pending = request
        dispatchPending()
    }

    override fun stop() {
        pending = null
        if (current == null) return
        current = null
        stopping = true
        tts.stop()
        main.removeCallbacks(stopTimeout)
        main.postDelayed(stopTimeout, STOP_TIMEOUT_MS)
    }

    override fun release() {
        main.removeCallbacks(stopTimeout)
        listener = null
        runCatching {
            tts.stop()
            tts.removeEventListener(events)
            tts.release()
        }.onFailure { Log.w(TAG, "tts release failed", it) }
    }

    private fun dispatchPending() {
        if (!ready || stopping || current != null) return
        val request = pending ?: return
        pending = null
        current = request
        // Always set explicitly: otherwise the Gleo speaker setting would decide.
        tts.setSpeakerType(request.speaker)
        tts.setLocale(Locale.forLanguageTag(request.languageTag))
        tts.speak(request.text)
    }

    private fun finishCurrent(error: String?) {
        val finished = current ?: return
        current = null
        if (error == null) {
            listener?.onSpeechDone(finished.token)
        } else {
            listener?.onSpeechFailed(finished.token, error)
        }
        dispatchPending()
    }

    private fun onStopConfirmed() {
        main.removeCallbacks(stopTimeout)
        if (!stopping) return
        stopping = false
        dispatchPending()
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        main.post { block() }
    }

    private companion object {
        const val TAG = "DadaCast.Tts"
        const val STOP_TIMEOUT_MS = 1_000L
    }
}

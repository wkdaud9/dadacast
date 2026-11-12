package ai.dadacast.app.speech

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

/** Pleos SpeechToText in ON_DEVICE mode: speech stays in the car. */
class PleosSpeechRecognizer(context: Context) : SpeechRecognizerEngine {
    private val main = Handler(Looper.getMainLooper())
    private val stt = SpeechToText(context.applicationContext, Mode.ON_DEVICE)
    private var listener: SpeechRecognizerEngine.Listener? = null
    private var ready = false
    private var current: Long? = null
    private var best: String? = null
    @Volatile
    private var feeding = false

    private val results = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = onMain { Log.i(TAG, "recognition started") }
        override fun onUpdated(stt: String, completed: Boolean) = onMain {
            Log.i(TAG, "heard${if (completed) " (final)" else " (partial)"}: $stt")
            if (stt.isNotBlank()) best = stt
            if (completed) finish()
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = onMain { if (off > 0) speechEnded() }
        // Debug aid: shows whether any sound reaches the speech service while listening.
        override fun onUpdatedRms(rms: Double) = onMain { noteRms(rms) }
        // The final text can arrive just after the end notice, so give it a moment.
        override fun onEndedRecognition() = onMain {
            Log.i(TAG, "recognition ended")
            speechEnded()
            main.postDelayed(finishLate, FINAL_GRACE_MS)
        }
        override fun onError(errorType: ErrorType) = onMain {
            Log.w(TAG, "stt error $errorType")
            finish()
        }
    }

    override fun start(listener: SpeechRecognizerEngine.Listener) {
        this.listener = listener
        stt.initialize(object : OnInitializeListener {
            override fun onSuccess() = onMain {
                Log.i(TAG, "stt ready")
                stt.addListener(results)
                ready = true
            }

            override fun onFailure(errorType: ErrorType) = onMain { Log.e(TAG, "stt init failed $errorType") }
        })
    }

    override fun listen(token: Long, languageTag: String) {
        if (!ready) {
            main.post { listener?.onHeard(token, null) }
            return
        }
        current = token
        best = null
        endNoticeSent = false
        rmsCount = 0
        rmsMax = 0.0
        rmsLoggedAt = 0L
        Log.d(TAG, "listen window start: locale=${Locale.forLanguageTag(languageTag)}")
        stt.setLocale(Locale.forLanguageTag(languageTag))
        val recorded = SpeechInputOverride.nextUtterance
        if (recorded != null) {
            SpeechInputOverride.nextUtterance = null
            Log.i(TAG, "listening to recorded speech (${recorded.size} bytes, $languageTag)")
            listener?.onInputDetected(token)
            feed(recorded)
        } else {
            Log.i(TAG, "listening to the microphone ($languageTag)")
            stt.request()
        }
    }

    override fun stop() {
        feeding = false
        main.removeCallbacks(finishLate)
        if (current != null) Log.d(TAG, "listen window end: rms updates=$rmsCount, max=%.1f".format(rmsMax))
        if (current == null) return
        current = null
        runCatching { stt.stop() }.onFailure { Log.w(TAG, "stt stop failed", it) }
        Log.i(TAG, "listening stopped")
    }

    override fun release() {
        stop()
        listener = null
        runCatching {
            stt.removeListener(results)
            stt.release()
        }
    }

    /** Sends PCM at real-time pace, 100 ms per chunk, as the SDK guide describes. */
    private fun feed(pcm: ByteArray) {
        feeding = true
        Thread {
            var offset = 0
            while (feeding && offset < pcm.size) {
                val end = minOf(offset + BYTES_PER_100_MS, pcm.size)
                stt.sendAudio(pcm.copyOfRange(offset, end))
                offset = end
                Thread.sleep(100L)
            }
            if (feeding) stt.completeAudioSend()
        }.apply { name = "stt-feed" }.start()
    }

    private val finishLate = Runnable { finish() }
    private var endNoticeSent = false

    private fun speechEnded() {
        val token = current ?: return
        if (endNoticeSent) return
        endNoticeSent = true
        listener?.onSpeechEnded(token)
    }

    private var rmsCount = 0
    private var rmsMax = 0.0
    private var rmsLoggedAt = 0L

    /** Logs the input level at most four times a second, plus a summary when listening ends. */
    private fun noteRms(rms: Double) {
        val token = current ?: return
        rmsCount++
        if (rms >= SPEECH_RMS_THRESHOLD && rmsMax < SPEECH_RMS_THRESHOLD) listener?.onInputDetected(token)
        if (rms > rmsMax) rmsMax = rms
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - rmsLoggedAt >= 250) {
            rmsLoggedAt = now
            Log.d(TAG, "rms %.1f".format(rms))
        }
    }

    private fun finish() {
        main.removeCallbacks(finishLate)
        if (current != null) Log.d(TAG, "listen window end: rms updates=$rmsCount, max=%.1f".format(rmsMax))
        val token = current ?: return
        current = null
        feeding = false
        listener?.onHeard(token, best)
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        main.post { block() }
    }

    private companion object {
        const val TAG = "DadaCast.Stt"
        const val BYTES_PER_100_MS = 3_200
        const val FINAL_GRACE_MS = 800L
        /**
         * Input level above which the microphone is clearly delivering sound. Measured on the
         * emulator: with the microphone off the level stays flat at about 4.7–4.8; with it on, a
         * quiet room already reads 10–50 and speech reaches the hundreds to over 1000.
         */
        const val SPEECH_RMS_THRESHOLD = 8.0
    }
}

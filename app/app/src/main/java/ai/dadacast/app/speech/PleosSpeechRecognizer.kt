package ai.dadacast.app.speech

import ai.dadacast.app.practice.ListenTurn
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.Locale

/**
 * Pleos SpeechToText in ON_DEVICE mode: speech stays in the car. One answer turn may span several
 * recognitions: the recogniser ends at the first short pause, so [ListenTurn] decides whether to
 * ask again and the words heard are added up. Only one recognition is open at a time: the next
 * starts after the previous one has both ended and given its final text (or [FINAL_GRACE_MS]).
 */
class PleosSpeechRecognizer(context: Context) : SpeechRecognizerEngine {
    private val main = Handler(Looper.getMainLooper())
    private val stt = SpeechToText(context.applicationContext, Mode.ON_DEVICE)
    private var listener: SpeechRecognizerEngine.Listener? = null
    private var ready = false
    private var current: Long? = null
    private var turn: ListenTurn? = null
    private var finalSeen = false
    private var endedSeen = false
    @Volatile
    private var feeding = false
    private var errorRetries = 0

    private val results = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = onMain { Log.i(TAG, "recognition started") }
        override fun onUpdated(stt: String, completed: Boolean) = onMain {
            Log.i(TAG, "heard${if (completed) " (final)" else " (partial)"}: $stt")
            val token = current ?: return@onMain
            val turn = turn ?: return@onMain
            turn.onText(stt)
            if (!completed) {
                turn.transcript?.let { listener?.onPartial(token, it) }
                return@onMain
            }
            finalSeen = true
            if (endedSeen) recognitionDone() else main.postDelayed(recognitionDoneLate, FINAL_GRACE_MS)
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = onMain { if (off > 0) maybeSpeechEnded() }
        // Shows whether sound reaches the speech service, and tells the turn when the speaker talks.
        override fun onUpdatedRms(rms: Double) = onMain { noteRms(rms) }
        // The final text can arrive just after the end notice, so give it a moment.
        override fun onEndedRecognition() = onMain {
            Log.i(TAG, "recognition ended")
            if (current == null) return@onMain
            endedSeen = true
            maybeSpeechEnded()
            if (finalSeen) recognitionDone() else main.postDelayed(recognitionDoneLate, FINAL_GRACE_MS)
        }
        override fun onError(errorType: ErrorType) = onMain {
            Log.w(TAG, "stt error $errorType")
            val turn = turn
            // Mid-phrase (e.g. the recogniser giving up during a pause), keep the turn going.
            if (current != null && turn != null && errorRetries < MAX_ERROR_RETRIES &&
                turn.recognitionEnded(SystemClock.elapsedRealtime())
            ) {
                errorRetries++
                main.removeCallbacks(recognitionDoneLate)
                Log.i(TAG, "stt error mid-answer: listening on (recognition ${turn.recognitions}, so far: ${turn.transcript})")
                main.postDelayed(listenAgain, ERROR_RETRY_DELAY_MS)
                return@onMain
            }
            finish(stopStt = false)
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

    override fun listen(token: Long, languageTag: String, expected: SpeechRecognizerEngine.Expected) {
        if (!ready) {
            main.post { listener?.onHeard(token, null) }
            return
        }
        clearPending()
        current = token
        endNoticeSent = false
        rmsCount = 0
        rmsMax = 0.0
        rmsLoggedAt = 0L
        Log.d(TAG, "listen window start: locale=${Locale.forLanguageTag(languageTag)}")
        stt.setLocale(Locale.forLanguageTag(languageTag))
        val recorded = SpeechInputOverride.nextUtterance
        turn = ListenTurn(
            expected.sampleAnswer,
            expected.targetExpression,
            expected.windowMs,
            SystemClock.elapsedRealtime(),
            canContinue = recorded == null,
        )
        finalSeen = false
        endedSeen = false
        errorRetries = 0
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
        clearPending()
        if (current != null) Log.d(TAG, "listen window end: rms updates=$rmsCount, max=%.1f".format(rmsMax))
        if (current == null) return
        current = null
        turn = null
        runCatching { stt.stop() }.onFailure { Log.w(TAG, "stt stop failed", it) }
        Log.i(TAG, "listening stopped")
    }

    private fun clearPending() {
        main.removeCallbacks(recognitionDoneLate)
        main.removeCallbacks(listenAgain)
        main.removeCallbacks(silenceCheck)
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

    private val recognitionDoneLate = Runnable { recognitionDone() }

    /** One recognition is over: ask again if the answer is still coming, otherwise report the turn. */
    private fun recognitionDone() {
        main.removeCallbacks(recognitionDoneLate)
        val turn = turn ?: return
        if (current == null) return
        if (!turn.recognitionEnded(SystemClock.elapsedRealtime())) {
            finish(stopStt = false)
            return
        }
        Log.i(TAG, "short pause: listening on (recognition ${turn.recognitions}, line ${turn.line + 1}, so far: ${turn.transcript})")
        main.postDelayed(listenAgain, RELISTEN_DELAY_MS)
    }

    private val listenAgain = Runnable {
        if (current == null || turn == null) return@Runnable
        finalSeen = false
        endedSeen = false
        runCatching { stt.request() }.onFailure {
            Log.w(TAG, "stt request again failed", it)
            finish(stopStt = false)
            return@Runnable
        }
        main.removeCallbacks(silenceCheck)
        main.postDelayed(silenceCheck, SILENCE_CHECK_MS)
    }

    /**
     * In a follow-on recognition, the turn ends after [ListenTurn.SILENCE_MS] without voice, but
     * only once the speaker has reached the phrase's last display line; before that only the
     * window (the practice timer) ends it.
     */
    private val silenceCheck = object : Runnable {
        override fun run() {
            val turn = turn ?: return
            if (current == null) return
            if (turn.silentTooLong(SystemClock.elapsedRealtime())) {
                Log.i(TAG, "quiet after speech: turn over")
                maybeSpeechEnded()
                finish(stopStt = true)
            } else {
                main.postDelayed(this, SILENCE_CHECK_MS)
            }
        }
    }

    private var endNoticeSent = false

    /** Tells practice the answer is being checked, unless the turn is about to listen on. */
    private fun maybeSpeechEnded() {
        val token = current ?: return
        if (endNoticeSent) return
        if (turn?.wouldContinue(SystemClock.elapsedRealtime()) == true) return
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
        turn?.onLevel(rms, SystemClock.elapsedRealtime())
        if (rms >= SPEECH_RMS_THRESHOLD && rmsMax < SPEECH_RMS_THRESHOLD) listener?.onInputDetected(token)
        if (rms > rmsMax) rmsMax = rms
        val now = SystemClock.elapsedRealtime()
        if (now - rmsLoggedAt >= 250) {
            rmsLoggedAt = now
            Log.d(TAG, "rms %.1f".format(rms))
        }
    }

    private fun finish(stopStt: Boolean) {
        clearPending()
        if (current != null) Log.d(TAG, "listen window end: rms updates=$rmsCount, max=%.1f".format(rmsMax))
        val token = current ?: return
        current = null
        feeding = false
        val heard = turn?.transcript
        turn = null
        if (stopStt) runCatching { stt.stop() }.onFailure { Log.w(TAG, "stt stop failed", it) }
        listener?.onHeard(token, heard)
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        main.post { block() }
    }

    private companion object {
        const val TAG = "DadaCast.Stt"
        const val BYTES_PER_100_MS = 3_200
        const val FINAL_GRACE_MS = 800L
        /** Gap between one recognition ending and the next request, so the two never overlap. */
        const val RELISTEN_DELAY_MS = 100L
        const val SILENCE_CHECK_MS = 250L
        /** A recogniser error mid-answer is retried this many times per turn, after this delay. */
        const val MAX_ERROR_RETRIES = 3
        const val ERROR_RETRY_DELAY_MS = 300L
        /**
         * Input level above which the microphone is clearly delivering sound. Measured on the
         * emulator: with the microphone off the level stays flat at about 4.7–4.8; with it on, a
         * quiet room already reads 10–50 and speech reaches the hundreds to over 1000.
         */
        const val SPEECH_RMS_THRESHOLD = 8.0
    }
}

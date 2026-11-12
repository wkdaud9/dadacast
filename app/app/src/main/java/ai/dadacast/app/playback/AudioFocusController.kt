package ai.dadacast.app.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Speech cannot usefully be ducked under navigation guidance, so the request asks to be told about
 * ducking situations and playback holds instead.
 */
class AudioFocusController(context: Context, private val callback: Callback) {
    interface Callback {
        fun onFocusGained()
        fun onFocusLostTransient()
        fun onFocusLostPermanent()
    }

    enum class Result { GRANTED, DELAYED, FAILED }

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var held = false

    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setWillPauseWhenDucked(true)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener({ change ->
            Log.i(TAG, "focus change $change")
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> callback.onFocusGained()
                AudioManager.AUDIOFOCUS_LOSS -> callback.onFocusLostPermanent()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> callback.onFocusLostTransient()
            }
        }, main)
        .build()

    val isHeld: Boolean get() = held

    fun request(): Result {
        val result = when (audioManager.requestAudioFocus(request)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> Result.GRANTED
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Result.DELAYED
            else -> Result.FAILED
        }
        held = result != Result.FAILED
        Log.i(TAG, "focus request $result")
        return result
    }

    fun abandon() {
        if (!held) return
        audioManager.abandonAudioFocusRequest(request)
        held = false
        Log.i(TAG, "focus abandoned")
    }

    private companion object {
        const val TAG = "DadaCast.Focus"
    }
}

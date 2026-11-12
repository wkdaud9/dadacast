package ai.dadacast.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/**
 * Debug-only stand-in for navigation guidance on the emulator:
 *   adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action take
 *   adb shell am broadcast -n ai.dadacast.app/.debug.FocusProbeReceiver --es action release
 */
class FocusProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val audio = context.getSystemService(AudioManager::class.java)
        val result = when (intent.getStringExtra("action")) {
            "take" -> audio.requestAudioFocus(request)
            "release" -> audio.abandonAudioFocusRequest(request)
            else -> return
        }
        Log.i("DadaCast.Probe", "${intent.getStringExtra("action")} -> $result")
    }

    private companion object {
        val request: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { }
            .build()
    }
}

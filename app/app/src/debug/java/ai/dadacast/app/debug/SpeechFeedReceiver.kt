package ai.dadacast.app.debug

import ai.dadacast.app.speech.SpeechInputOverride
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * Debug-only stand-in for a speaking driver. The next practice answer is read from a 16 kHz,
 * 16-bit, mono PCM file in the app's files folder instead of the microphone:
 *   adb shell am broadcast -n ai.dadacast.app/.debug.SpeechFeedReceiver --es file answer.pcm
 *   adb shell am broadcast -n ai.dadacast.app/.debug.SpeechFeedReceiver --es file none
 */
class SpeechFeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("file") ?: return
        if (name == "none") {
            SpeechInputOverride.nextUtterance = null
            Log.i(TAG, "next answer: microphone")
            return
        }
        val file = File(context.filesDir, name)
        if (!file.isFile) {
            Log.w(TAG, "missing ${file.path}")
            return
        }
        SpeechInputOverride.nextUtterance = file.readBytes()
        Log.i(TAG, "next answer: ${file.name} (${file.length()} bytes)")
    }

    private companion object {
        const val TAG = "DadaCast.Probe"
    }
}

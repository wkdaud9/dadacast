package ai.dadacast.app.debug

import ai.dadacast.app.driving.GuidancePoint
import ai.dadacast.app.driving.TurnGuidanceInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Debug-only simulation of a turn-by-turn update, because the emulator cannot drive a route:
 *   adb shell am broadcast -n ai.dadacast.app/.debug.TurnGuidanceReceiver --es type TURN_RIGHT --ei distance 150
 *   adb shell am broadcast -n ai.dadacast.app/.debug.TurnGuidanceReceiver --es type none
 * It goes through the same path as the real navigation callback.
 */
class TurnGuidanceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val points = if (type == "none") {
            emptyList()
        } else {
            listOf(GuidancePoint(type, intent.getIntExtra("distance", 0).toDouble()))
        }
        val receiver = TurnGuidanceInput.receiver
        Log.i("DadaCast.Probe", "simulated turn guidance $points${if (receiver == null) " (service not running)" else ""}")
        receiver?.invoke(points)
    }
}

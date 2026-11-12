package ai.dadacast.app.ui

import ai.dadacast.app.ux.UxRestrictionsState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * The app's own screen, opened by explicit intent. UX restrictions come from the playback service,
 * which this screen binds to through [PlayerClient].
 */
class MainActivity : ComponentActivity() {
    private lateinit var player: PlayerClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = PlayerClient(this)
        setContent { DadaCastApp(player, UxRestrictionsState.limits) }
    }

    override fun onStart() {
        super.onStart()
        player.connect()
    }

    override fun onStop() {
        player.disconnect()
        super.onStop()
    }
}

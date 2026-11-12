package ai.dadacast.app.ui

import ai.dadacast.app.R
import ai.dadacast.app.consent.ConsentChoice
import ai.dadacast.app.consent.ConsentStore
import ai.dadacast.app.driving.VehicleSpeedSource
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * First-run choice about vehicle data and voice. Not marked as usable while driving, so the
 * system only shows it when the car is parked. Also opened later from our screen or from the
 * media app's settings button.
 */
class ConsentActivity : ComponentActivity() {
    private val askSpeed = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        ConsentStore.touch(this)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConsentStore.init(this)
        setContent { ConsentScreen(onAgree = ::agree, onDecline = ::decline) }
    }

    private fun agree() {
        ConsentStore.set(this, ConsentChoice.AGREED)
        if (checkSelfPermission(VehicleSpeedSource.PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            finish()
        } else {
            askSpeed.launch(VehicleSpeedSource.PERMISSION)
        }
    }

    private fun decline() {
        ConsentStore.set(this, ConsentChoice.DECLINED)
        finish()
    }
}

@Composable
private fun ConsentScreen(onAgree: () -> Unit, onDecline: () -> Unit) {
    DadaTheme {
        val c = LocalDadaColors.current
        Box(Modifier.fillMaxSize().background(c.background).padding(48.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 1100.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.consent_title), style = DadaType.titleLarge, color = c.text)
                Spacer(Modifier.height(28.dp))
                listOf(R.string.consent_item_car, R.string.consent_item_brake, R.string.consent_item_wiper, R.string.consent_item_voice, R.string.consent_item_stays).forEach {
                    Text("•  " + stringResource(it), style = DadaType.bodyLarge, color = c.text)
                    Spacer(Modifier.height(14.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.consent_decline_note), style = DadaType.bodySmall, color = c.textSecondary)
                Spacer(Modifier.height(36.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    ChoiceButton(R.string.consent_agree, c.accent, c.onAccent, c.accent, onAgree)
                    ChoiceButton(R.string.consent_decline, c.surfaceHigh, c.text, c.surfacePressed, onDecline)
                }
            }
        }
    }
}

@Composable
private fun ChoiceButton(label: Int, background: Color, text: Color, pressed: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 96.dp)
            .widthIn(min = 280.dp)
            .pressable(RoundedCornerShape(16.dp), background, pressed, onClick)
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(label), style = DadaType.bodyLargeStrong, color = text)
    }
}

package ai.dadacast.app.ui

import ai.dadacast.app.R
import ai.dadacast.app.playback.statusTextRes
import ai.dadacast.app.ux.UxLimits
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

private val TouchTarget = 112.dp
private val ScreenPadding = 24.dp

@Composable
fun DadaCastApp(player: PlayerClient, uxLimits: StateFlow<UxLimits>) {
    val state by player.state.collectAsStateWithLifecycle()
    val limits by uxLimits.collectAsStateWithLifecycle()
    var showNowPlaying by remember { mutableStateOf(false) }

    DadaTheme {
        val c = LocalDadaColors.current
        Column(Modifier.fillMaxSize().background(c.background).padding(ScreenPadding)) {
            when {
                state.practicePhrase != null -> {
                    Header(limits = limits)
                    PracticeScreen(state, limits)
                }
                showNowPlaying -> {
                    Header(limits = limits, onBack = { showNowPlaying = false })
                    NowPlayingScreen(state, limits, player)
                }
                else -> {
                    Header(limits = limits)
                    EpisodeListScreen(state, limits) { id ->
                        player.play(id)
                        showNowPlaying = true
                    }
                }
            }
        }
    }
}

/**
 * Pleos Page Navigation: app icon and name, a back button on deeper screens, and a suffix action.
 * Settings are a parked-only screen, so that entry is hidden while driving.
 */
@Composable
private fun Header(limits: UxLimits, onBack: (() -> Unit)? = null) {
    val c = LocalDadaColors.current
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().heightIn(min = 96.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Row(
                Modifier
                    .heightIn(min = 88.dp)
                    .pressable(RoundedCornerShape(44.dp), c.background, c.surfacePressed, onBack)
                    .padding(start = 12.dp, end = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_back), null, tint = c.text, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_back), style = DadaType.bodyLargeStrong, color = c.text)
            }
            Spacer(Modifier.width(16.dp))
        }
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2F6BEB)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_dada_mark), null, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.width(20.dp))
        Text(stringResource(R.string.app_name), style = DadaType.titleLarge, color = c.text, modifier = Modifier.weight(1f))
        if (limits.showDetails) {
            Box(
                Modifier
                    .heightIn(min = 88.dp)
                    .pressable(RoundedCornerShape(16.dp), c.surfaceHigh, c.surfacePressed) {
                        context.startActivity(Intent(context, ConsentActivity::class.java))
                    }
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.settings), style = DadaType.bodyLargeStrong, color = c.text)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/** Read-only view of a practice that runs by voice; it shows only when the user opens our app. */
@Composable
private fun PracticeScreen(state: PlayerUiState, limits: UxLimits) {
    val c = LocalDadaColors.current
    val phase = when {
        state.practiceChecking -> PracticePhase.CHECKING
        state.practiceListening -> PracticePhase.LISTENING
        else -> PracticePhase.SPEAKING
    }
    Row(
        Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(56.dp),
    ) {
        Box(
            Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(if (phase == PracticePhase.LISTENING) c.accent else c.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(phase.icon),
                stringResource(phase.label),
                tint = if (phase == PracticePhase.LISTENING) c.onAccent else c.accent,
                modifier = Modifier.size(128.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(phase.label), style = DadaType.titleMedium, color = c.accent)
            if (state.practiceNoMic && phase == PracticePhase.LISTENING) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.status_no_mic), style = DadaType.bodyLargeStrong, color = c.text)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                limits.limitTitle(state.practicePhrase.orEmpty()),
                style = DadaType.titleLarge,
                color = c.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            state.practiceTranscript?.let { heard ->
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.practice_heard_format, limits.limitTitle(heard)),
                    style = DadaType.bodyLarge,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            verdictText(state.practiceVerdict)?.let { verdict ->
                Spacer(Modifier.height(16.dp))
                Text(stringResource(verdict), style = DadaType.titleSmall, color = c.text)
            }
        }
    }
}

/** Listening → checking → speaking, after the Gleo voice state model. */
private enum class PracticePhase(@DrawableRes val icon: Int, @StringRes val label: Int) {
    SPEAKING(R.drawable.ic_speaker, R.string.practice_speaking),
    LISTENING(R.drawable.ic_mic, R.string.practice_listening),
    CHECKING(R.drawable.ic_checking, R.string.practice_checking),
}

private fun verdictText(verdict: String?): Int? = when (verdict) {
    "GOOD" -> R.string.practice_good
    "CLOSE" -> R.string.practice_close
    "MISS" -> R.string.practice_miss
    "SILENT" -> R.string.practice_silent
    else -> null
}

@Composable
private fun EpisodeListScreen(state: PlayerUiState, limits: UxLimits, onSelect: (String) -> Unit) {
    val c = LocalDadaColors.current
    val shown = limits.maxListItems?.let { state.episodes.take(it) } ?: state.episodes
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(shown, key = { it.id }) { episode ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchTarget)
                    .pressable(RoundedCornerShape(20.dp), c.surface, c.surfacePressed) { onSelect(episode.id) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painterResource(artRes(episode.category)),
                    null,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(24.dp))
                Text(
                    limits.limitTitle(episode.title),
                    style = DadaType.titleSmall,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.minutes_format, (episode.durationMs / 60_000).toInt().coerceAtLeast(1)),
                    style = DadaType.bodyMedium,
                    color = c.textSecondary,
                    modifier = Modifier.padding(start = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(state: PlayerUiState, limits: UxLimits, player: PlayerClient) {
    val c = LocalDadaColors.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            Image(
                painterResource(artRes(state.currentCategory)),
                null,
                modifier = Modifier.size(260.dp).clip(RoundedCornerShape(32.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    limits.limitTitle(state.currentTitle),
                    style = DadaType.titleLarge,
                    color = c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(statusTextRes(state.status, state.holdReason)), style = DadaType.titleMedium, color = c.accent)
                if (limits.showDetails && state.sectionCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.section_format, state.sectionIndex, state.sectionCount, state.sectionTitle),
                        style = DadaType.bodyMedium,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(32.dp))
                LinearProgressIndicator(
                    progress = { if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                    color = c.accent,
                    trackColor = c.track,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(R.drawable.ic_previous, R.string.action_previous, c.surfaceHigh, c.text, onClick = player::previous)
            Spacer(Modifier.width(64.dp))
            ControlButton(
                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (state.isPlaying) R.string.action_pause else R.string.action_play,
                c.accent,
                c.onAccent,
                size = TouchTarget + 24.dp,
                onClick = player::togglePlayPause,
            )
            Spacer(Modifier.width(64.dp))
            ControlButton(R.drawable.ic_next, R.string.action_next, c.surfaceHigh, c.text, onClick = player::next)
        }
    }
}

@Composable
private fun ControlButton(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    background: Color,
    tint: Color,
    size: Dp = TouchTarget,
    onClick: () -> Unit,
) {
    val c = LocalDadaColors.current
    Box(
        Modifier.size(size).pressable(CircleShape, background, if (background == c.accent) background else c.surfacePressed, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), stringResource(label), tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

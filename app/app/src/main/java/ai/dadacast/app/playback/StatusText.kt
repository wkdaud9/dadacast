package ai.dadacast.app.playback

import ai.dadacast.app.R
import androidx.annotation.StringRes

/** The one place that maps player state to the words the driver sees. */
@StringRes
fun statusTextRes(status: PlayerStatus, reason: DrivingSignal?): Int = when (status) {
    PlayerStatus.IDLE -> R.string.status_idle
    PlayerStatus.PLAYING -> R.string.status_playing
    PlayerStatus.ENDING_SENTENCE -> R.string.status_ending_sentence
    PlayerStatus.RESUMING -> R.string.status_resuming
    PlayerStatus.PRACTICE -> R.string.status_practice
    PlayerStatus.PAUSED -> R.string.status_paused
    PlayerStatus.FINISHED -> R.string.status_finished
    PlayerStatus.ERROR -> R.string.status_error
    PlayerStatus.HELD -> when (reason) {
        DrivingSignal.TURN_SIGNAL -> R.string.hold_turn_signal
        DrivingSignal.TURN_GUIDANCE -> R.string.hold_turn_guidance
        DrivingSignal.CAUTION_POINT -> R.string.hold_caution_point
        DrivingSignal.OTHER_AUDIO, null -> R.string.hold_other_audio
    }
}

/**
 * The two lines of the media app's now-playing view: TITLE / DISPLAY_TITLE (big first line) and
 * DISPLAY_SUBTITLE. While driving: the episode title and a short fixed status, never sentence
 * text. During a stopped-car practice: the phrase to say as rolling lines
 * ([PhraseSegments.lines]): the first line is the line to say now, the second previews the next
 * line, or on the last line shows a short status. Lines never move on a timer: while listening
 * they follow the speaker's live transcript ([PlaybackStateMachine] keeps the furthest line
 * reached, see [PhraseSegments.followLine]); before and after listening the phrase starts again
 * from its first line.
 */
sealed interface NowPlayingLines {
    /** Episode title on the first line, this status on the second. */
    data class Status(@StringRes val res: Int) : NowPlayingLines

    /**
     * [lines] depend on the phrase only, so state changes within a practice never re-cut them;
     * [index] is the line being said.
     */
    data class Phrase(val lines: List<String>, @StringRes val statusRes: Int, val index: Int = 0) : NowPlayingLines {
        private val at: Int get() = index.coerceIn(lines.indices)
        val title: String get() = lines[at]
        /** The next line, or null on the last line (the caller shows [statusRes] there). */
        val next: String? get() = lines.getOrNull(at + 1)
    }
}

fun nowPlayingLines(snapshot: PlayerSnapshot): NowPlayingLines {
    val practice = snapshot.practice ?: return NowPlayingLines.Status(drivingStatusRes(snapshot))
    val status = when {
        practice.noMicInput && practice.listening -> R.string.status_no_mic
        practice.checking -> R.string.status_checking
        practice.listening -> R.string.status_listening
        else -> R.string.status_shadow
    }
    val lines = PhraseSegments.lines(practice.targetPhrase)
    if (lines.isEmpty()) {
        return NowPlayingLines.Status(if (status == R.string.status_shadow) R.string.status_practice else status)
    }
    val following = practice.listening || practice.checking
    return NowPlayingLines.Phrase(lines, status, if (following) practice.line else 0)
}

/** Status words outside a practice; a repeat-after-me line gets its own short cue. */
@StringRes
fun drivingStatusRes(snapshot: PlayerSnapshot): Int = drivingStatusRes(snapshot.status, snapshot.holdReason, snapshot.shadowLine)

@StringRes
fun drivingStatusRes(status: PlayerStatus, reason: DrivingSignal?, shadowLine: Boolean): Int =
    if (shadowLine && status == PlayerStatus.PLAYING) R.string.status_shadow else statusTextRes(status, reason)

@StringRes
fun categoryTitleRes(category: String): Int = when (category) {
    "english_conversation" -> R.string.category_english
    "interview_rehearsal" -> R.string.category_interview
    "my_material_rehearsal" -> R.string.category_my_material
    else -> R.string.category_other
}

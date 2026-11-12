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

@StringRes
fun categoryTitleRes(category: String): Int = when (category) {
    "english_conversation" -> R.string.category_english
    "interview_rehearsal" -> R.string.category_interview
    "my_material_rehearsal" -> R.string.category_my_material
    else -> R.string.category_other
}

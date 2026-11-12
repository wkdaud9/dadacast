package ai.dadacast.app.playback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

/**
 * Plays silence from this app's own process while speech is playing.
 *
 * Android only routes media keys to a session whose app owns active audio, and Pleos TextToSpeech
 * plays through the Speech Service in another process. Without a track of our own the system has
 * no media button session and steering wheel keys never reach this app.
 */
class SilentAudioPresence {
    private var track: AudioTrack? = null
    private var active = false

    fun setActive(wanted: Boolean) {
        if (wanted == active) return
        active = wanted
        runCatching {
            if (wanted) start() else track?.pause()
        }.onFailure { Log.w(TAG, "silent track failed", it) }
    }

    fun release() {
        runCatching { track?.release() }
        track = null
        active = false
    }

    private fun start() {
        val current = track ?: buildTrack().also { track = it }
        current.play()
    }

    private fun buildTrack(): AudioTrack {
        val silence = ShortArray(SAMPLE_RATE)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(silence.size * Short.SIZE_BYTES)
            .build()
        track.write(silence, 0, silence.size)
        track.setLoopPoints(0, silence.size, -1)
        return track
    }

    private companion object {
        const val TAG = "DadaCast.Silence"
        const val SAMPLE_RATE = 8_000
    }
}

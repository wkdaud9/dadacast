package ai.dadacast.app.driving

import ai.dadacast.app.playback.DrivingSignal

/** Receives driving signals on the main thread. */
fun interface DrivingSignalSink {
    fun onSignal(signal: DrivingSignal, on: Boolean)
}

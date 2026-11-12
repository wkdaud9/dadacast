package ai.dadacast.app.driving

import ai.dadacast.app.playback.DrivingSignal
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Turn signal from the Vehicle SDK. The data never leaves the car. */
class VehicleSignalSource(
    context: Context,
    private val sink: DrivingSignalSink,
    /** Brake pedal pressed or released; used only to tell a standing car from a moving one. */
    private val onBrake: (Boolean) -> Unit = {},
    /** Front wipers running or not; rain means no stop practice. */
    private val onWiper: (Boolean) -> Unit = {},
) {
    private val windshield = WindowArea(WindowValue.FRONT_WINDSHIELD)

    private val wiper = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            Log.i(TAG, "wiper $state")
            if (state != null) main.post { onWiper(state == WiperState.ON) }
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "wiper listener failed", e)
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private val vehicle = Vehicle(context.applicationContext)
    private var started = false

    private val turnSignal = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            Log.i(TAG, "turn signal $state")
            if (state != null) emit(DrivingSignal.TURN_SIGNAL, state != TurnSignalValue.OFF)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "turn signal listener failed", e)
        }
    }

    private val brake = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) {
            Log.i(TAG, "brake pedal $pressed")
            if (pressed != null) main.post { onBrake(pressed) }
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "brake listener failed", e)
        }
    }

    fun start() {
        runCatching {
            vehicle.initialize()
            vehicle.getTurnSignal().registerTurnSignalState(turnSignal)
            vehicle.getBrake().registerBrakePedalStatus(brake)
            vehicle.getWiper().registerWiperState(windshield, wiper)
            vehicle.getWiper().getCurrentWiperState(
                windshield,
                onComplete = { state -> if (state != null) main.post { onWiper(state == WiperState.ON) } },
                onFailed = { Log.w(TAG, "wiper state unavailable", it) },
            )
            started = true
        }.onFailure { Log.e(TAG, "vehicle signals unavailable", it) }
    }

    fun stop() {
        if (!started) return
        runCatching {
            vehicle.getTurnSignal().unregisterTurnSignalState(turnSignal)
            vehicle.getBrake().unregisterBrakePedalStatus(brake)
            vehicle.getWiper().unregisterWiperState(windshield, wiper)
            vehicle.release()
        }.onFailure { Log.w(TAG, "vehicle release failed", it) }
        started = false
    }

    private fun emit(signal: DrivingSignal, on: Boolean) {
        main.post { sink.onSignal(signal, on) }
    }

    private companion object {
        const val TAG = "DadaCast.Vehicle"
    }
}

package ai.dadacast.app.driving

import ai.dadacast.app.playback.DrivingSignal
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Upcoming turns and caution points from NaviHelper, known ahead of time from remaining distance. */
class NaviSignalSource(context: Context, private val sink: DrivingSignalSink) {
    private val main = Handler(Looper.getMainLooper())
    private val appContext = context.applicationContext
    private var helper: NaviHelper? = null

    private val turn = ProximityTracker(TURN_ON_METERS, TURN_OFF_METERS)
    private val caution = ProximityTracker(CAUTION_ON_METERS, CAUTION_OFF_METERS)

    private val staleTimeout = Runnable {
        Log.i(TAG, "guidance updates stopped")
        clear()
    }

    private val listener = object : NaviHelperEventListener {
        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            Log.i(TAG, "tbt $tbtInfoList")
            val turns = tbtInfoList.map { GuidancePoint(it.type.name, it.distance) }
            main.post { updateTurn(turns) }
        }

        override fun onCautionPointInfo(cautionPointList: List<CautionPoint>) {
            Log.i(TAG, "caution $cautionPointList")
            main.post { updateCaution(cautionPointList.map { it.component3().toDouble() }) }
        }

        override fun onRouteEnded() = clearOnMain()
        override fun onRouteCancelled() = clearOnMain()
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = clearOnMain()

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navi error $errorCode")
        }
    }

    fun start() {
        TurnGuidanceInput.receiver = { turns -> updateTurn(turns) }
        runCatching {
            NaviHelper(appContext).also {
                it.initialize()
                it.addListener(listener)
                helper = it
            }
        }.onFailure { Log.e(TAG, "navi signals unavailable", it) }
    }

    fun stop() {
        TurnGuidanceInput.receiver = null
        main.removeCallbacks(staleTimeout)
        helper?.let {
            runCatching {
                it.removeListener(listener)
                it.release()
            }.onFailure { e -> Log.w(TAG, "navi release failed", e) }
        }
        helper = null
    }

    private fun updateTurn(turns: List<GuidancePoint>) {
        val wasActive = turn.active
        if (turn.update(TurnGuidance.distanceToNextTurn(turns)) != wasActive) {
            sink.onSignal(DrivingSignal.TURN_GUIDANCE, turn.active)
        }
        armStaleTimeout()
    }

    private fun updateCaution(distances: List<Double>) {
        val wasActive = caution.active
        if (caution.update(distances) != wasActive) sink.onSignal(DrivingSignal.CAUTION_POINT, caution.active)
        armStaleTimeout()
    }

    /** A hold must never outlive its data, for example when the navigation app stops updating. */
    private fun armStaleTimeout() {
        main.removeCallbacks(staleTimeout)
        if (turn.active || caution.active) main.postDelayed(staleTimeout, STALE_AFTER_MS)
    }

    private fun clearOnMain() {
        main.post { clear() }
    }

    private fun clear() {
        main.removeCallbacks(staleTimeout)
        if (turn.active) {
            turn.clear()
            sink.onSignal(DrivingSignal.TURN_GUIDANCE, false)
        }
        if (caution.active) {
            caution.clear()
            sink.onSignal(DrivingSignal.CAUTION_POINT, false)
        }
    }

    private companion object {
        const val TAG = "DadaCast.Navi"
        const val TURN_ON_METERS = 300.0
        const val TURN_OFF_METERS = 350.0
        const val CAUTION_ON_METERS = 200.0
        const val CAUTION_OFF_METERS = 250.0
        const val STALE_AFTER_MS = 15_000L
    }
}

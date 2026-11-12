package ai.dadacast.app.ux

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log

/** Registers with CarUxRestrictionsManager and reports every change as [UxLimits]. */
class UxRestrictionsMonitor(
    private val context: Context,
    private val onChanged: (UxLimits) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { apply(it) }

    fun start() {
        try {
            val connected = Car.createCar(context) ?: error("car service unavailable")
            val uxManager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            uxManager.registerListener(listener)
            Log.i(TAG, "listener registered")
            car = connected
            manager = uxManager
            apply(uxManager.currentCarUxRestrictions)
        } catch (e: Exception) {
            Log.e(TAG, "UX restrictions unavailable, staying fully restricted", e)
            onChanged(UxRestrictionsState.RESTRICTED)
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "android.car library missing, staying fully restricted", e)
            onChanged(UxRestrictionsState.RESTRICTED)
        }
    }

    fun stop() {
        manager?.let {
            runCatching { it.unregisterListener() }
                .onSuccess { Log.i(TAG, "listener unregistered") }
                .onFailure { e -> Log.w(TAG, "unregister failed", e) }
        }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    private fun apply(restrictions: CarUxRestrictions?) {
        restrictions ?: return
        Log.i(TAG, "onUxRestrictionsChanged requiresDO=${restrictions.isRequiresDistractionOptimization} active=${restrictions.activeRestrictions}")
        onChanged(
            UxLimits.from(
                requiresDistractionOptimization = restrictions.isRequiresDistractionOptimization,
                activeRestrictions = restrictions.activeRestrictions,
                maxStringLength = restrictions.maxRestrictedStringLength,
                maxContentItems = restrictions.maxCumulativeContentItems,
            )
        )
    }

    private companion object {
        const val TAG = "DadaCast.Ux"
    }
}

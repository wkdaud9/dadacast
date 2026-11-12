package ai.dadacast.app.driving

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Standard AAOS vehicle speed, used only to tell a standing car from a moving one. */
class VehicleSpeedSource(private val context: Context, private val onSpeed: (Double?) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private var car: Car? = null
    private var manager: CarPropertyManager? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val metresPerSecond = (value.value as? Float) ?: return
            main.post { onSpeed(metresPerSecond * 3.6) }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "speed error")
        }
    }

    fun start() {
        if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "speed permission not granted, using the brake pedal instead")
            return
        }
        try {
            val connected = Car.createCar(context) ?: return
            val properties = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            properties.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            car = connected
            manager = properties
            Log.i(TAG, "speed listener registered")
        } catch (e: Exception) {
            Log.w(TAG, "speed unavailable", e)
        } catch (e: NoClassDefFoundError) {
            Log.w(TAG, "android.car library missing", e)
        }
    }

    fun stop() {
        runCatching { manager?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
        manager = null
        car = null
        onSpeed(null)
    }

    companion object {
        const val PERMISSION = "android.car.permission.CAR_SPEED"
        private const val TAG = "DadaCast.Speed"
    }
}

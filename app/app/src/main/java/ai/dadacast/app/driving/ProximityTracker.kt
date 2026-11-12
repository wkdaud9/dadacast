package ai.dadacast.app.driving

/**
 * Turns "metres to the next guidance point" into on/off. Clearing needs a larger distance than
 * triggering so small distance changes around the threshold do not flap.
 */
class ProximityTracker(private val onMeters: Double, private val offMeters: Double) {
    init {
        require(offMeters >= onMeters)
    }

    var active = false
        private set

    /** Returns the new state. An empty list means no guidance point ahead. */
    fun update(distancesMeters: List<Double>): Boolean {
        val nearest = distancesMeters.filter { it >= 0 }.minOrNull()
        active = when {
            nearest == null -> false
            active -> nearest <= offMeters
            else -> nearest <= onMeters
        }
        return active
    }

    fun clear() {
        active = false
    }
}

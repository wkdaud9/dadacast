package ai.dadacast.app.driving

import ai.dadacast.app.playback.StopContext

/**
 * Works out whether the car is standing still from what an app can observe:
 * - Platform says no driving restrictions apply: parked.
 * - Vehicle speed is readable (needs the CAR_SPEED permission): stopped once it has read about 0
 *   for [speedHoldMs] without a break, so stop-and-go traffic never counts as a stop. Any reading
 *   at or above [stoppedBelowKmh] ends the stop at once.
 * - Otherwise: stopped when the brake pedal has been held for [brakeHoldMs]. A held brake also
 *   happens while slowing down, so this is the weaker signal.
 */
class StopDetector(
    private val brakeHoldMs: Long = 2_000L,
    private val stoppedBelowKmh: Double = 1.0,
    private val speedHoldMs: Long = SPEED_HOLD_MS,
) {
    private var requiresDistractionOptimization = true
    private var speedKmh: Double? = null
    private var standingSinceMs: Long? = null
    private var brakeSinceMs: Long? = null

    fun onRestrictions(requiresDistractionOptimization: Boolean) {
        this.requiresDistractionOptimization = requiresDistractionOptimization
    }

    fun onSpeed(kmh: Double?, nowMs: Long) {
        speedKmh = kmh
        standingSinceMs = if (kmh != null && kmh < stoppedBelowKmh) standingSinceMs ?: nowMs else null
    }

    fun onBrake(pressed: Boolean, nowMs: Long) {
        brakeSinceMs = if (pressed) brakeSinceMs ?: nowMs else null
    }

    fun context(nowMs: Long): StopContext {
        if (!requiresDistractionOptimization) return StopContext.PARKED
        val since = (if (speedKmh != null) standingSinceMs else brakeSinceMs) ?: return StopContext.NONE
        return if (nowMs - since >= holdMs()) StopContext.RED_LIGHT else StopContext.NONE
    }

    /** When to look again because a stop is about to count as one; null if not needed. */
    fun recheckInMs(nowMs: Long): Long? {
        if (!requiresDistractionOptimization) return null
        val since = (if (speedKmh != null) standingSinceMs else brakeSinceMs) ?: return null
        val left = holdMs() - (nowMs - since)
        return if (left > 0) left else null
    }

    private fun holdMs() = if (speedKmh != null) speedHoldMs else brakeHoldMs

    companion object {
        /** How long the speed must stay at about 0 before a stop in drive counts as a red light. */
        const val SPEED_HOLD_MS = 4_000L
    }
}

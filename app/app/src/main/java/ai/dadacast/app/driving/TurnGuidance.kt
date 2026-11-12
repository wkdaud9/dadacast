package ai.dadacast.app.driving

data class GuidancePoint(val type: String, val distanceMeters: Double)

object TurnGuidance {
    const val ARRIVE = "TURN_ARRIVE"

    /**
     * The navigation app sends the whole list of upcoming points. Only the first entry is measured
     * from the car; later entries are distances between consecutive points, so they say nothing
     * about how close the car is. Arriving is not a manoeuvre worth pausing for.
     */
    fun distanceToNextTurn(points: List<GuidancePoint>): List<Double> {
        val next = points.firstOrNull() ?: return emptyList()
        return if (next.type == ARRIVE) emptyList() else listOf(next.distanceMeters)
    }
}

/** Test hook: lets a debug build feed turn guidance as if it came from the navigation app. */
object TurnGuidanceInput {
    @Volatile
    var receiver: ((List<GuidancePoint>) -> Unit)? = null
}

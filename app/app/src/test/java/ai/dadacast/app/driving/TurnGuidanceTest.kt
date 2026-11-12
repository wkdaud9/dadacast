package ai.dadacast.app.driving

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnGuidanceTest {
    /** Shape seen from the built-in navigation app: first entry from the car, then gaps between turns. */
    private val route = listOf(
        GuidancePoint("TURN_RIGHT", 700.0),
        GuidancePoint("TURN_LEFT", 80.0),
        GuidancePoint("TURN_RIGHT", 150.0),
        GuidancePoint("TURN_ARRIVE", 20.0),
    )

    @Test
    fun `only the first entry counts as distance to the next turn`() {
        assertEquals(listOf(700.0), TurnGuidance.distanceToNextTurn(route))
        val tracker = ProximityTracker(300.0, 350.0)
        assertFalse(tracker.update(TurnGuidance.distanceToNextTurn(route)))
    }

    @Test
    fun `a near first turn holds`() {
        val near = listOf(GuidancePoint("TURN_LEFT", 250.0)) + route.drop(1)
        assertTrue(ProximityTracker(300.0, 350.0).update(TurnGuidance.distanceToNextTurn(near)))
    }

    @Test
    fun `arriving does not hold`() {
        assertEquals(emptyList<Double>(), TurnGuidance.distanceToNextTurn(listOf(GuidancePoint("TURN_ARRIVE", 20.0))))
    }

    @Test
    fun `an empty list clears the hold`() {
        val tracker = ProximityTracker(300.0, 350.0)
        tracker.update(listOf(100.0))
        assertFalse(tracker.update(TurnGuidance.distanceToNextTurn(emptyList())))
    }
}

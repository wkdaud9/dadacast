package ai.dadacast.app.driving

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityTrackerTest {
    private val tracker = ProximityTracker(onMeters = 300.0, offMeters = 350.0)

    @Test
    fun `turns on within range and uses the nearest point`() {
        assertFalse(tracker.update(listOf(500.0, 900.0)))
        assertTrue(tracker.update(listOf(900.0, 280.0)))
    }

    @Test
    fun `does not flap between the on and off distances`() {
        tracker.update(listOf(290.0))
        assertTrue(tracker.update(listOf(340.0)))
        assertFalse(tracker.update(listOf(360.0)))
        assertFalse(tracker.update(listOf(340.0)))
    }

    @Test
    fun `clears when nothing is ahead`() {
        tracker.update(listOf(100.0))
        assertFalse(tracker.update(emptyList()))
    }
}

package ai.dadacast.app.driving

import ai.dadacast.app.playback.StopContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopDetectorTest {
    @Test
    fun `no restrictions means parked`() {
        val detector = StopDetector()
        detector.onRestrictions(false)
        assertEquals(StopContext.PARKED, detector.context(0))
    }

    @Test
    fun `with speed, standing still for 4 seconds is a red light stop`() {
        val detector = StopDetector()
        detector.onSpeed(0.0, 1_000)
        assertEquals(StopContext.NONE, detector.context(1_000))
        assertEquals(4_000L, detector.recheckInMs(1_000))
        // Further zero readings do not restart the count.
        detector.onSpeed(0.0, 3_000)
        assertEquals(StopContext.NONE, detector.context(4_999))
        assertEquals(1L, detector.recheckInMs(4_999))
        assertEquals(StopContext.RED_LIGHT, detector.context(5_000))
        assertNull(detector.recheckInMs(5_000))
    }

    @Test
    fun `stop and go traffic never counts as a stop`() {
        val detector = StopDetector()
        var now = 0L
        repeat(10) {
            detector.onSpeed(0.0, now)
            now += 3_000
            assertEquals(StopContext.NONE, detector.context(now))
            detector.onSpeed(5.0, now)
            now += 1_000
            assertEquals(StopContext.NONE, detector.context(now))
        }
    }

    @Test
    fun `moving ends a red light stop at once`() {
        val detector = StopDetector()
        detector.onSpeed(0.0, 0)
        assertEquals(StopContext.RED_LIGHT, detector.context(4_000))
        detector.onSpeed(3.0, 4_100)
        assertEquals(StopContext.NONE, detector.context(4_100))
        assertNull(detector.recheckInMs(4_100))
        // Standing again starts a fresh count.
        detector.onSpeed(0.0, 5_000)
        assertEquals(StopContext.NONE, detector.context(8_000))
        assertEquals(StopContext.RED_LIGHT, detector.context(9_000))
    }

    @Test
    fun `without speed, a brake held long enough counts as a stop`() {
        val detector = StopDetector(brakeHoldMs = 2_000)
        detector.onBrake(true, 1_000)
        assertEquals(StopContext.NONE, detector.context(1_500))
        assertEquals(1_500L, detector.recheckInMs(1_500))
        assertEquals(StopContext.RED_LIGHT, detector.context(3_000))
        detector.onBrake(false, 3_500)
        assertEquals(StopContext.NONE, detector.context(3_500))
        assertNull(detector.recheckInMs(3_500))
    }

    @Test
    fun `speed wins over the brake when both are known`() {
        val detector = StopDetector()
        detector.onBrake(true, 0)
        detector.onSpeed(30.0, 0)
        assertEquals(StopContext.NONE, detector.context(10_000))
    }

    @Test
    fun `parked is unchanged by speed readings`() {
        val detector = StopDetector()
        detector.onRestrictions(false)
        detector.onSpeed(0.0, 0)
        assertEquals(StopContext.PARKED, detector.context(0))
        assertNull(detector.recheckInMs(0))
    }
}

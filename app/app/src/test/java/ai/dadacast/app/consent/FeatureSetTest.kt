package ai.dadacast.app.consent

import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureSetTest {
    @Test
    fun `before a choice nothing is read and playback waits`() {
        assertEquals(FeatureSet(false, false, false, needsChoice = true), FeatureSet.of(ConsentChoice.NOT_ASKED, true))
    }

    @Test
    fun `declining keeps plain playback only`() {
        assertEquals(FeatureSet(false, false, false, needsChoice = false), FeatureSet.of(ConsentChoice.DECLINED, true))
    }

    @Test
    fun `agreeing enables everything the permissions allow`() {
        assertEquals(FeatureSet(true, true, true, false), FeatureSet.of(ConsentChoice.AGREED, true))
        // Speed denied: stop detection falls back to the brake pedal.
        assertEquals(FeatureSet(true, false, true, false), FeatureSet.of(ConsentChoice.AGREED, false))
    }
}

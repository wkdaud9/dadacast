package ai.dadacast.app.ux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UxLimitsTest {
    @Test
    fun `parked state has no limits`() {
        val limits = UxLimits.from(false, 0, 120, 21)
        assertEquals(UxLimits(), limits)
        assertTrue(limits.showDetails)
    }

    @Test
    fun `driving hides details even without string or list limits`() {
        val limits = UxLimits.from(true, 0x50, 120, 21)
        assertFalse(limits.showDetails)
        assertEquals(null, limits.maxTitleLength)
        assertEquals(null, limits.maxListItems)
    }

    @Test
    fun `applies active string and list limits`() {
        val limits = UxLimits.from(true, UxLimits.LIMIT_STRING_LENGTH or UxLimits.LIMIT_CONTENT, 5, 3)
        assertEquals(3, limits.maxListItems)
        assertEquals("가나다라마…", limits.limitTitle("가나다라마바사"))
        assertEquals("가나다", limits.limitTitle("가나다"))
    }
}

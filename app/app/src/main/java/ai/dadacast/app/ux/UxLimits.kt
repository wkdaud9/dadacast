package ai.dadacast.app.ux

/** What the UI may show right now, derived from the platform's active UX restrictions. */
data class UxLimits(
    val requiresDistractionOptimization: Boolean = false,
    /** Null means no limit. */
    val maxTitleLength: Int? = null,
    /** Null means no limit. */
    val maxListItems: Int? = null,
    val showDetails: Boolean = true,
) {
    /** Applied per browse node, which keeps every level within the cumulative limit too. */
    fun <T> limitList(items: List<T>): List<T> = maxListItems?.let(items::take) ?: items

    fun limitTitle(title: String): String {
        val max = maxTitleLength ?: return title
        return if (title.length <= max) title else title.take(max).trimEnd() + "…"
    }

    companion object {
        /** Bit values mirror CarUxRestrictions so the mapping stays testable off-device. */
        const val LIMIT_STRING_LENGTH = 4
        const val LIMIT_CONTENT = 32

        fun from(
            requiresDistractionOptimization: Boolean,
            activeRestrictions: Int,
            maxStringLength: Int,
            maxContentItems: Int,
        ): UxLimits {
            if (!requiresDistractionOptimization) return UxLimits()
            return UxLimits(
                requiresDistractionOptimization = true,
                maxTitleLength = maxStringLength.takeIf { activeRestrictions and LIMIT_STRING_LENGTH != 0 && it > 0 },
                maxListItems = maxContentItems.takeIf { activeRestrictions and LIMIT_CONTENT != 0 && it > 0 },
                // Study text and details are parked-only, whichever restriction bits are set.
                showDetails = false,
            )
        }
    }
}

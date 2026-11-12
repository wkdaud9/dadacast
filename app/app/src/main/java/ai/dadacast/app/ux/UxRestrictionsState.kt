package ai.dadacast.app.ux

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Current UX limits for the whole app. The playback service owns the listener and writes here;
 * screens only read. Until the platform reports otherwise, everything is treated as restricted.
 */
object UxRestrictionsState {
    val RESTRICTED = UxLimits(requiresDistractionOptimization = true, showDetails = false)

    private val _limits = MutableStateFlow(RESTRICTED)
    val limits: StateFlow<UxLimits> = _limits.asStateFlow()

    fun update(limits: UxLimits) {
        _limits.value = limits
    }

    fun reset() {
        _limits.value = RESTRICTED
    }
}

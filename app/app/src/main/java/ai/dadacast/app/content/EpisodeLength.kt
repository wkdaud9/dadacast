package ai.dadacast.app.content

/** Episode length as listeners read it: whole minutes, never "0분". */
object EpisodeLength {
    /** Whole minutes, rounded down (1953초 → 32분), at least 1. */
    fun minutes(durationMs: Long): Int = (durationMs / 60_000L).toInt().coerceAtLeast(1)

    /** "약 32분" — shown under each episode in Media Center and in the app's own list. */
    fun label(durationMs: Long): String = "약 ${minutes(durationMs)}분"
}

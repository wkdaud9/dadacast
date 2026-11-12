package ai.dadacast.app.consent

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConsentChoice { NOT_ASKED, AGREED, DECLINED }

/** What the app may do, given the user's choice and the permissions Android granted. */
data class FeatureSet(
    /** Turn signal, brake, navigation guidance. */
    val drivingSignals: Boolean,
    /** Vehicle speed for telling a red-light stop from driving. */
    val speed: Boolean,
    /** Speaking practice when the car is stopped. */
    val practice: Boolean,
    /** Playback waits until the user has made a choice. */
    val needsChoice: Boolean,
) {
    companion object {
        /**
         * Declining keeps plain playback that only yields to other audio, as the proposal says.
         * Agreeing without the speed permission falls back to the brake pedal for stop detection.
         */
        fun of(choice: ConsentChoice, speedGranted: Boolean) = when (choice) {
            ConsentChoice.NOT_ASKED -> FeatureSet(false, false, false, needsChoice = true)
            ConsentChoice.DECLINED -> FeatureSet(false, false, false, needsChoice = false)
            ConsentChoice.AGREED -> FeatureSet(true, speedGranted, true, needsChoice = false)
        }
    }
}

/** The user's choice, kept on the device. */
object ConsentStore {
    private const val FILE = "consent"
    private const val KEY = "choice"
    private const val KEY_REVISION = "revision"

    private val _choice = MutableStateFlow(ConsentChoice.NOT_ASKED)
    val choice: StateFlow<ConsentChoice> = _choice.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context): SharedPreferences {
        val p = prefs ?: context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).also { prefs = it }
        _choice.value = read(p)
        return p
    }

    fun set(context: Context, choice: ConsentChoice) {
        init(context).edit().putString(KEY, choice.name).apply()
        _choice.value = choice
    }

    /** Signals that a permission answer arrived, so listeners re-check what is allowed. */
    fun touch(context: Context) {
        val p = init(context)
        p.edit().putLong(KEY_REVISION, p.getLong(KEY_REVISION, 0) + 1).apply()
    }

    fun read(p: SharedPreferences): ConsentChoice =
        p.getString(KEY, null)?.let { runCatching { ConsentChoice.valueOf(it) }.getOrNull() } ?: ConsentChoice.NOT_ASKED
}

package ai.dadacast.app.ui

import ai.dadacast.app.playback.BrowseTree
import ai.dadacast.app.playback.DadaCastService
import ai.dadacast.app.playback.DrivingSignal
import ai.dadacast.app.playback.PlayerStatus
import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EpisodeItem(val id: String, val title: String, val durationMs: Long, val category: String = "")

data class PlayerUiState(
    val episodes: List<EpisodeItem> = emptyList(),
    val status: PlayerStatus = PlayerStatus.IDLE,
    val holdReason: DrivingSignal? = null,
    val currentId: String? = null,
    val currentTitle: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val sectionIndex: Int = 0,
    val sectionCount: Int = 0,
    val sectionTitle: String = "",
    val practicePhrase: String? = null,
    val practiceListening: Boolean = false,
    val practiceChecking: Boolean = false,
    val practiceNoMic: Boolean = false,
    val currentCategory: String? = null,
    val practiceTranscript: String? = null,
    val practiceVerdict: String? = null,
) {
    val isPlaying: Boolean
        get() = status in setOf(
            PlayerStatus.PLAYING, PlayerStatus.ENDING_SENTENCE, PlayerStatus.HELD, PlayerStatus.RESUMING, PlayerStatus.PRACTICE,
        )
}

/** The UI talks to the player the same way the car's media controls do: through the session. */
class PlayerClient(context: Context) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val appContext = context.applicationContext

    private companion object {
        const val TAG = "DadaCast.Client"
    }
    private var controller: MediaControllerCompat? = null

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) = applyState(state)
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) = applyMetadata(metadata)
    }

    private val subscription = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            Log.i(TAG, "episodes: ${children.size}")
            _state.value = _state.value.copy(
                episodes = children.map {
                    EpisodeItem(
                        id = it.mediaId.orEmpty(),
                        title = it.description.title?.toString().orEmpty(),
                        durationMs = it.description.extras?.getLong(DadaCastService.EXTRA_DURATION_MS) ?: 0L,
                        category = it.description.extras?.getString(DadaCastService.EXTRA_CATEGORY).orEmpty(),
                    )
                }
            )
        }
    }

    private var browser: MediaBrowserCompat? = null

    /** A fresh browser per connection; a disconnected MediaBrowserCompat is not reused. */
    fun connect() {
        if (browser != null) return
        lateinit var created: MediaBrowserCompat
        created = MediaBrowserCompat(
            appContext,
            ComponentName(appContext, DadaCastService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    Log.i(TAG, "connected")
                    val connected = MediaControllerCompat(appContext, created.sessionToken)
                    connected.registerCallback(controllerCallback)
                    controller = connected
                    applyState(connected.playbackState)
                    applyMetadata(connected.metadata)
                    created.subscribe(BrowseTree.ALL_EPISODES_ID, subscription)
                }

                override fun onConnectionFailed() { Log.w(TAG, "connection failed") }
                override fun onConnectionSuspended() { Log.w(TAG, "connection suspended") }
            },
            null,
        )
        browser = created
        created.connect()
    }

    fun disconnect() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
        browser?.let {
            if (it.isConnected) it.unsubscribe(BrowseTree.ALL_EPISODES_ID)
            it.disconnect()
        }
        browser = null
    }

    fun play(episodeId: String) {
        val current = _state.value
        if (episodeId == current.currentId && current.isPlaying) return
        controller?.transportControls?.playFromMediaId(episodeId, null)
    }

    fun togglePlayPause() {
        val controls = controller?.transportControls ?: return
        if (_state.value.isPlaying) controls.pause() else controls.play()
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    private fun applyState(state: PlaybackStateCompat?) {
        state ?: return
        val extras = state.extras
        _state.value = _state.value.copy(
            status = extras?.getString(DadaCastService.EXTRA_STATUS)?.let(PlayerStatus::valueOf) ?: PlayerStatus.IDLE,
            holdReason = extras?.getString(DadaCastService.EXTRA_HOLD_REASON)?.let(DrivingSignal::valueOf),
            positionMs = state.position,
            sectionIndex = extras?.getInt(DadaCastService.EXTRA_SECTION_INDEX) ?: 0,
            sectionCount = extras?.getInt(DadaCastService.EXTRA_SECTION_COUNT) ?: 0,
            sectionTitle = extras?.getString(DadaCastService.EXTRA_SECTION_TITLE).orEmpty(),
            practicePhrase = extras?.getString(DadaCastService.EXTRA_PRACTICE_PHRASE),
            practiceListening = extras?.getBoolean(DadaCastService.EXTRA_PRACTICE_LISTENING) ?: false,
            practiceChecking = extras?.getBoolean(DadaCastService.EXTRA_PRACTICE_CHECKING) ?: false,
            practiceNoMic = extras?.getBoolean(DadaCastService.EXTRA_PRACTICE_NO_MIC) ?: false,
            practiceTranscript = extras?.getString(DadaCastService.EXTRA_PRACTICE_TRANSCRIPT),
            practiceVerdict = extras?.getString(DadaCastService.EXTRA_PRACTICE_VERDICT),
        )
    }

    private fun applyMetadata(metadata: MediaMetadataCompat?) {
        metadata ?: return
        _state.value = _state.value.copy(
            currentId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
            currentTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE).orEmpty(),
            currentCategory = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE),
            durationMs = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
        )
    }
}

package ai.dadacast.app.playback

import ai.dadacast.app.R
import ai.dadacast.app.content.AssetEpisodeSource
import ai.dadacast.app.content.Episode
import ai.dadacast.app.content.EpisodeSearch
import ai.dadacast.app.driving.NaviSignalSource
import ai.dadacast.app.driving.VehicleSignalSource
import ai.dadacast.app.consent.ConsentChoice
import ai.dadacast.app.consent.ConsentStore
import ai.dadacast.app.consent.FeatureSet
import ai.dadacast.app.driving.StopDetector
import ai.dadacast.app.ui.ConsentActivity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.media.utils.MediaConstants
import ai.dadacast.app.driving.VehicleSpeedSource
import ai.dadacast.app.speech.PleosSpeechEngine
import ai.dadacast.app.speech.PleosSpeechRecognizer
import ai.dadacast.app.speech.SpeechRecognizerEngine
import ai.dadacast.app.speech.Voices
import ai.dadacast.app.speech.SpeechEngine
import ai.dadacast.app.ui.MainActivity
import ai.dadacast.app.ui.artRes
import ai.dadacast.app.ui.artUri
import android.net.Uri
import ai.dadacast.app.ux.UxLimits
import ai.dadacast.app.ux.UxRestrictionsMonitor
import ai.dadacast.app.ux.UxRestrictionsState
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle

/**
 * The player. Runs without the UI, exposes standard media controls through a MediaSession, and
 * feeds speech, timers, audio focus and driving signals into [PlaybackStateMachine].
 */
class DadaCastService : MediaBrowserServiceCompat(), SpeechEngine.Listener, AudioFocusController.Callback {
    private val main = Handler(Looper.getMainLooper())

    private lateinit var episodes: List<Episode>
    private lateinit var machine: PlaybackStateMachine
    private lateinit var speech: SpeechEngine
    private lateinit var session: MediaSessionCompat
    private lateinit var focus: AudioFocusController
    private var vehicleSignals: VehicleSignalSource? = null
    private var naviSignals: NaviSignalSource? = null
    private var speedSource: VehicleSpeedSource? = null
    private var features = FeatureSet.of(ConsentChoice.NOT_ASKED, speedGranted = false)
    private lateinit var consentPrefs: SharedPreferences
    // Kept as a field: SharedPreferences holds listeners weakly.
    private val consentListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applyFeatures() }
    private val artBitmaps = mutableMapOf<String, Bitmap>()
    private lateinit var uxMonitor: UxRestrictionsMonitor
    private lateinit var recognizer: SpeechRecognizerEngine
    private val stopDetector = StopDetector()
    private var stopContext = StopContext.NONE
    private val stopRecheck = Runnable { evaluateStop() }

    private val audioPresence = SilentAudioPresence()
    private var timer: Runnable? = null
    private var inForeground = false
    private var publishedEpisode = -1
    private var publishedSubtitle: String? = null
    private var publishedTitle: String? = null
    private var publishedSnapshot: PlayerSnapshot? = null

    override fun onCreate() {
        super.onCreate()
        // Keeps the service in the Started state after a bind-and-unbind (Pleos Music/Audio guide).
        runCatching { startService(Intent(this, DadaCastService::class.java)) }
            .onFailure { Log.w(TAG, "startService refused", it) }

        createNotificationChannel()
        episodes = AssetEpisodeSource(this).load()
        machine = PlaybackStateMachine(episodes)
        focus = AudioFocusController(this, this)
        speech = PleosSpeechEngine(this).also {
            Log.i(TAG, "speech engine: ${it.name}")
            it.start(this)
        }
        session = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(
                PendingIntent.getActivity(
                    this@DadaCastService,
                    0,
                    Intent(this@DadaCastService, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            setCallback(sessionCallback)
            isActive = true
        }
        sessionToken = session.sessionToken
        restorePosition()
        publish()
        recognizer = PleosSpeechRecognizer(this).also {
            it.start(object : SpeechRecognizerEngine.Listener {
                override fun onHeard(token: Long, transcript: String?) {
                    Log.i(TAG, "heard: ${transcript ?: "(nothing)"}")
                    dispatch(PlayerEvent.Heard(token, transcript))
                }

                override fun onSpeechEnded(token: Long) = dispatch(PlayerEvent.SpeechEnded(token))
                override fun onInputDetected(token: Long) = dispatch(PlayerEvent.InputDetected(token))
            })
        }
        uxMonitor = UxRestrictionsMonitor(this, ::onUxLimitsChanged).also { it.start() }

        // Vehicle and navigation data are read only after the user agrees.
        consentPrefs = ConsentStore.init(this)
        consentPrefs.registerOnSharedPreferenceChangeListener(consentListener)
        applyFeatures()
    }

    /** Starts or stops each data source to match the user's choice and granted permissions. */
    private fun applyFeatures() {
        val speedGranted = checkSelfPermission(VehicleSpeedSource.PERMISSION) == PackageManager.PERMISSION_GRANTED
        val next = FeatureSet.of(ConsentStore.read(consentPrefs), speedGranted)
        Log.i(TAG, "features $next")
        if (next.drivingSignals && vehicleSignals == null) {
            vehicleSignals = VehicleSignalSource(
                this,
                ::onDrivingSignal,
                onBrake = { pressed ->
                    stopDetector.onBrake(pressed, SystemClock.elapsedRealtime())
                    evaluateStop()
                },
                onWiper = { running ->
                    Log.i(TAG, "wipers ${if (running) "running" else "off"}")
                    dispatch(PlayerEvent.WiperChanged(running))
                },
            ).also { it.start() }
            naviSignals = NaviSignalSource(this, ::onDrivingSignal).also { it.start() }
        } else if (!next.drivingSignals && vehicleSignals != null) {
            vehicleSignals?.stop()
            naviSignals?.stop()
            vehicleSignals = null
            naviSignals = null
            stopDetector.onBrake(false, SystemClock.elapsedRealtime())
            dispatch(PlayerEvent.WiperChanged(false))
            listOf(DrivingSignal.TURN_SIGNAL, DrivingSignal.TURN_GUIDANCE, DrivingSignal.CAUTION_POINT)
                .forEach { dispatch(PlayerEvent.SignalOff(it)) }
        }
        if (next.speed && speedSource == null) {
            speedSource = VehicleSpeedSource(this) { kmh ->
                stopDetector.onSpeed(kmh, SystemClock.elapsedRealtime())
                evaluateStop()
            }.also { it.start() }
        } else if (!next.speed && speedSource != null) {
            speedSource?.stop()
            speedSource = null
        }
        val choiceChanged = next.needsChoice != features.needsChoice
        features = next
        evaluateStop()
        publish()
        if (choiceChanged) BrowseTree.parentIds(episodes).forEach(::notifyChildrenChanged)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enterForeground()
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "task removed: stopping as the Pleos media guide asks")
        dispatch(PlayerEvent.Stop)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        uxMonitor.stop()
        speedSource?.stop()
        consentPrefs.unregisterOnSharedPreferenceChangeListener(consentListener)
        recognizer.release()
        UxRestrictionsState.reset()
        vehicleSignals?.stop()
        naviSignals?.stop()
        audioPresence.release()
        focus.abandon()
        speech.release()
        session.release()
        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        Log.i(TAG, "browser connected: $clientPackageName")
        reannounceSession()
        return BrowserRoot(BrowseTree.ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        // AAOS pattern for "set up before use": a null root makes the media app show the
        // playback error full screen, with its resolution button.
        if (features.needsChoice && parentId == BrowseTree.ROOT_ID) {
            result.sendResult(null)
            return
        }
        val nodes = BrowseTree.children(parentId, episodes, UxRestrictionsState.limits.value) {
            getString(categoryTitleRes(it))
        }
        result.sendResult(nodes.map(::mediaItem).toMutableList())
    }

    private fun mediaItem(node: BrowseNode) = MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder()
            .setMediaId(node.id)
            .setTitle(node.title)
            .setIconUri(Uri.parse(artUri(node.category)))
            .setExtras(
                Bundle().apply {
                    putString(EXTRA_CATEGORY, node.category)
                    if (node.playable) putLong(EXTRA_DURATION_MS, node.durationMs)
                }
            )
            .build(),
        if (node.playable) MediaBrowserCompat.MediaItem.FLAG_PLAYABLE else MediaBrowserCompat.MediaItem.FLAG_BROWSABLE,
    )

    /** Runs for the service's lifetime, so restrictions apply even when no app screen is open. */
    private fun onUxLimitsChanged(limits: UxLimits) {
        UxRestrictionsState.update(limits)
        stopDetector.onRestrictions(limits.requiresDistractionOptimization)
        evaluateStop()
        BrowseTree.parentIds(episodes).forEach(::notifyChildrenChanged)
        publish()
    }

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        /** Which controller sent a command; tells the mini player, Media Center and keys apart. */
        private fun log(command: String) {
            val caller = runCatching { session.currentControllerInfo.packageName }.getOrNull()
            Log.i(TAG, "command $command from $caller")
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            log("media button ${mediaButtonEvent?.getParcelableExtra<android.view.KeyEvent>(Intent.EXTRA_KEY_EVENT)?.keyCode}")
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onPlay() { log("play"); startPlayback(null) }
        override fun onPause() { log("pause"); dispatch(PlayerEvent.Pause) }
        override fun onStop() { log("stop"); dispatch(PlayerEvent.Stop) }
        override fun onSkipToNext() { log("next"); dispatch(PlayerEvent.Next) }
        override fun onSkipToPrevious() { log("previous"); dispatch(PlayerEvent.Previous) }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            log("playFromMediaId $mediaId")
            val index = episodes.indexOfFirst { it.id == mediaId }
            if (index >= 0) startPlayback(index)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            val index = EpisodeSearch.find(query, episodes) { getString(categoryTitleRes(it)) }
            Log.i(TAG, "search \"$query\" -> ${index?.let { episodes[it].id } ?: "current"}")
            startPlayback(index)
        }
    }

    private fun startPlayback(episodeIndex: Int?) {
        if (features.needsChoice) {
            // The media app shows the error with a button that opens the choice screen.
            Log.i(TAG, "play refused: no choice about vehicle data yet")
            publish()
            return
        }
        when (focus.request()) {
            AudioFocusController.Result.FAILED -> return
            AudioFocusController.Result.DELAYED -> dispatch(PlayerEvent.SignalOn(DrivingSignal.OTHER_AUDIO))
            AudioFocusController.Result.GRANTED -> Unit
        }
        dispatch(PlayerEvent.Play(episodeIndex))
    }

    private fun onDrivingSignal(signal: DrivingSignal, on: Boolean) {
        Log.i(TAG, "signal $signal ${if (on) "on" else "off"}")
        dispatch(if (on) PlayerEvent.SignalOn(signal) else PlayerEvent.SignalOff(signal))
    }

    override fun onSpeechDone(token: Long) = dispatch(PlayerEvent.SpeechDone(token))

    override fun onSpeechFailed(token: Long, reason: String) {
        Log.e(TAG, "speech failed: $reason")
        dispatch(PlayerEvent.SpeechFailed(token))
    }

    override fun onFocusGained() = dispatch(PlayerEvent.SignalOff(DrivingSignal.OTHER_AUDIO))
    override fun onFocusLostTransient() = dispatch(PlayerEvent.SignalOn(DrivingSignal.OTHER_AUDIO))
    override fun onFocusLostPermanent() = dispatch(PlayerEvent.Pause)

    private fun evaluateStop() {
        val now = SystemClock.elapsedRealtime()
        main.removeCallbacks(stopRecheck)
        stopDetector.recheckInMs(now)?.let { main.postDelayed(stopRecheck, it) }
        val context = if (features.practice) stopDetector.context(now) else StopContext.NONE
        if (context == stopContext) return
        stopContext = context
        Log.i(TAG, "car is ${context.name}")
        dispatch(PlayerEvent.StopChanged(context))
    }

    private fun dispatch(event: PlayerEvent) {
        machine.handle(event).forEach(::apply)
        val snapshot = machine.snapshot
        if (!snapshot.wantsAudio && focus.isHeld) {
            focus.abandon()
            machine.handle(PlayerEvent.SignalOff(DrivingSignal.OTHER_AUDIO)).forEach(::apply)
        }
        publish()
    }

    private fun apply(effect: PlayerEffect) {
        when (effect) {
            is PlayerEffect.Speak -> {
                val speaker = Voices.speakerFor(effect.voice, effect.languageTag)
                Log.i(TAG, "speak [${effect.languageTag}/$speaker] ${effect.text}")
                speech.speak(effect.token, effect.text, effect.languageTag, speaker)
            }
            PlayerEffect.StopSpeech -> {
                Log.i(TAG, "stop speech")
                speech.stop()
            }
            is PlayerEffect.StartTimer -> {
                cancelTimer()
                timer = Runnable { dispatch(PlayerEvent.TimerFired(effect.token)) }
                    .also { main.postDelayed(it, effect.delayMs) }
            }
            PlayerEffect.CancelTimer -> cancelTimer()
            is PlayerEffect.Listen -> {
                Log.i(TAG, "listen [${effect.languageTag}]")
                recognizer.listen(effect.token, effect.languageTag)
            }
            PlayerEffect.StopListening -> recognizer.stop()
        }
    }

    private fun cancelTimer() {
        timer?.let(main::removeCallbacks)
        timer = null
    }

    private val reannounce = Runnable {
        // The system media widget only attaches to sessions that become active after it starts.
        // When the media app (re)connects to an already running session, switch the session off
        // and on again so the widget sees it as newly active.
        Log.i(TAG, "re-announcing media session")
        session.isActive = false
        session.isActive = true
        publish()
    }

    private fun reannounceSession() {
        main.removeCallbacks(reannounce)
        main.postDelayed(reannounce, REANNOUNCE_DELAY_MS)
    }

    /** Last position, kept on the device so a restarted service picks up where it was. */
    private val positionPrefs by lazy { getSharedPreferences("playback", MODE_PRIVATE) }

    private fun restorePosition() {
        val p = positionPrefs
        if (!p.contains("episode")) return
        machine.handle(PlayerEvent.Restore(p.getInt("episode", 0), p.getInt("chunk", 0), p.getInt("sentence", 0)))
        Log.i(TAG, "restored ${machine.snapshot.episodeIndex}/${machine.snapshot.chunkIndex}/${machine.snapshot.sentenceIndex}, paused")
    }

    private fun savePosition(snapshot: PlayerSnapshot) {
        if (snapshot.episodeIndex < 0) return
        positionPrefs.edit()
            .putInt("episode", snapshot.episodeIndex)
            .putInt("chunk", snapshot.chunkIndex)
            .putInt("sentence", snapshot.sentenceIndex)
            .apply()
    }

    private fun publish() {
        val snapshot = machine.snapshot
        val statusText = statusText(snapshot)
        if (snapshot != publishedSnapshot) {
            publishedSnapshot = snapshot
            savePosition(snapshot)
            Log.i(TAG, "state ${snapshot.status} ep=${snapshot.episodeIndex} chunk=${snapshot.chunkIndex} sentence=${snapshot.sentenceIndex} reason=${snapshot.holdReason} : $statusText${snapshot.practice?.let { " practice=$it" } ?: ""}")
        }
        audioPresence.setActive(snapshot.status == PlayerStatus.PLAYING || snapshot.status == PlayerStatus.ENDING_SENTENCE)
        session.setPlaybackState(playbackState(snapshot))
        updateMetadata(snapshot, statusText)
        if (snapshot.wantsAudio) enterForeground() else leaveForeground(snapshot.status == PlayerStatus.FINISHED)
    }

    private fun playbackState(snapshot: PlayerSnapshot): PlaybackStateCompat {
        val state = when (snapshot.status) {
            PlayerStatus.PLAYING, PlayerStatus.ENDING_SENTENCE,
            PlayerStatus.HELD, PlayerStatus.RESUMING, PlayerStatus.PRACTICE -> PlaybackStateCompat.STATE_PLAYING
            PlayerStatus.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlayerStatus.FINISHED, PlayerStatus.IDLE -> PlaybackStateCompat.STATE_STOPPED
            PlayerStatus.ERROR -> PlaybackStateCompat.STATE_ERROR
        }
        // A hold keeps STATE_PLAYING at speed 0 so a pause press means "don't resume on your own".
        val held = snapshot.status == PlayerStatus.HELD || snapshot.status == PlayerStatus.RESUMING ||
            snapshot.status == PlayerStatus.PRACTICE
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING && !held) 1f else 0f
        if (features.needsChoice && snapshot.status == PlayerStatus.IDLE) return consentNeededState()
        return PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
            )
            .setState(state, positionMs(snapshot), speed)
            .setExtras(
                Bundle().apply {
                    putString(EXTRA_STATUS, snapshot.status.name)
                    putString(EXTRA_HOLD_REASON, snapshot.holdReason?.name)
                    snapshot.practice?.let { practice ->
                        putString(EXTRA_PRACTICE_KIND, practice.kind.name)
                        putString(EXTRA_PRACTICE_PHRASE, practice.targetPhrase)
                        putBoolean(EXTRA_PRACTICE_LISTENING, practice.listening)
                        putBoolean(EXTRA_PRACTICE_CHECKING, practice.checking)
                        putBoolean(EXTRA_PRACTICE_NO_MIC, practice.noMicInput)
                        putString(EXTRA_PRACTICE_TRANSCRIPT, practice.transcript)
                        putString(EXTRA_PRACTICE_VERDICT, practice.verdict?.name)
                    }
                    episodes.getOrNull(snapshot.episodeIndex)?.let { episode ->
                        putInt(EXTRA_SECTION_INDEX, snapshot.chunkIndex + 1)
                        putInt(EXTRA_SECTION_COUNT, episode.chunks.size)
                        putString(EXTRA_SECTION_TITLE, episode.chunks[snapshot.chunkIndex].title)
                    }
                }
            )
            .build()
    }

    /** Estimated from each chunk's est_seconds; TTS has no seek position. */
    private fun positionMs(snapshot: PlayerSnapshot): Long {
        val episode = episodes.getOrNull(snapshot.episodeIndex) ?: return 0L
        val chunk = episode.chunks[snapshot.chunkIndex]
        val before = episode.chunks.take(snapshot.chunkIndex).sumOf { it.estSeconds }
        val within = chunk.estSeconds * snapshot.sentenceIndex.toDouble() / chunk.sentences.size
        return ((before + within) * 1000).toLong()
    }

    private fun updateMetadata(snapshot: PlayerSnapshot, fullStatusText: String) {
        val episode = episodes.getOrNull(snapshot.episodeIndex) ?: return
        val limits = UxRestrictionsState.limits.value
        val title = limits.limitTitle(episode.title)
        val statusText = limits.limitTitle(fullStatusText)
        if (snapshot.episodeIndex == publishedEpisode && statusText == publishedSubtitle && title == publishedTitle) return
        publishedEpisode = snapshot.episodeIndex
        publishedSubtitle = statusText
        publishedTitle = title
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, episode.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, statusText)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.host_name))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getString(R.string.app_name))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, episode.estTotalSeconds * 1000L)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, episode.category)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artBitmap(episode.category))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri(episode.category))
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, artUri(episode.category))
                .build()
        )
    }

    /**
     * Practice runs by voice; the media app and mini player show it through this line, with the
     * phrase to say when it fits.
     */
    private fun statusText(snapshot: PlayerSnapshot): String {
        val practice = snapshot.practice
            ?: return getString(statusTextRes(snapshot.status, snapshot.holdReason))
        if (practice.noMicInput && practice.listening) return getString(R.string.status_no_mic)
        val base = getString(
            when {
                practice.checking -> R.string.status_checking
                practice.listening -> R.string.status_listening
                else -> R.string.status_practice
            }
        )
        return if (practice.targetPhrase.isBlank()) base else "$base · ${practice.targetPhrase}"
    }

    /**
     * Standard AAOS media error with a resolution action: the media app shows the message and a
     * button that opens [ConsentActivity].
     */
    private fun consentNeededState(): PlaybackStateCompat {
        val openConsent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, ConsentActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
            .setErrorMessage(PlaybackStateCompat.ERROR_CODE_ACTION_ABORTED, getString(R.string.consent_needed))
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)
            .setExtras(
                Bundle().apply {
                    putString(MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL, getString(R.string.consent_start))
                    putParcelable(MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT, openConsent)
                }
            )
            .build()
    }

    /** Opaque art tile for the media app; cached per series. */
    private fun artBitmap(category: String): Bitmap = artBitmaps.getOrPut(category) {
        ContextCompat.getDrawable(this, artRes(category))!!.toBitmap(512, 512)
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun enterForeground() {
        if (inForeground) return
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_headphones)
            .setContentTitle(getString(R.string.app_name))
            .setContentIntent(session.controller.sessionActivity)
            .setStyle(MediaStyle().setMediaSession(session.sessionToken))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            inForeground = true
        }.onFailure { Log.w(TAG, "could not enter foreground", it) }
    }

    private fun leaveForeground(removeNotification: Boolean) {
        if (!inForeground) return
        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        inForeground = false
    }

    companion object {
        const val EXTRA_STATUS = "ai.dadacast.app.STATUS"
        private const val REANNOUNCE_DELAY_MS = 1_500L
        const val EXTRA_HOLD_REASON = "ai.dadacast.app.HOLD_REASON"
        const val EXTRA_SECTION_INDEX = "ai.dadacast.app.SECTION_INDEX"
        const val EXTRA_SECTION_COUNT = "ai.dadacast.app.SECTION_COUNT"
        const val EXTRA_SECTION_TITLE = "ai.dadacast.app.SECTION_TITLE"
        const val EXTRA_DURATION_MS = "ai.dadacast.app.DURATION_MS"
        const val EXTRA_CATEGORY = "ai.dadacast.app.CATEGORY"
        const val EXTRA_PRACTICE_CHECKING = "ai.dadacast.app.PRACTICE_CHECKING"
        const val EXTRA_PRACTICE_NO_MIC = "ai.dadacast.app.PRACTICE_NO_MIC"
        const val EXTRA_PRACTICE_KIND = "ai.dadacast.app.PRACTICE_KIND"
        const val EXTRA_PRACTICE_PHRASE = "ai.dadacast.app.PRACTICE_PHRASE"
        const val EXTRA_PRACTICE_LISTENING = "ai.dadacast.app.PRACTICE_LISTENING"
        const val EXTRA_PRACTICE_TRANSCRIPT = "ai.dadacast.app.PRACTICE_TRANSCRIPT"
        const val EXTRA_PRACTICE_VERDICT = "ai.dadacast.app.PRACTICE_VERDICT"
        private const val TAG = "DadaCast.Service"
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
    }
}

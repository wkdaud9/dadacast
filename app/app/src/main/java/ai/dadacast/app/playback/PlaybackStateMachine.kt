package ai.dadacast.app.playback

import ai.dadacast.app.content.Episode
import ai.dadacast.app.practice.LocalCoach
import ai.dadacast.app.practice.PracticeCoach
import ai.dadacast.app.practice.PracticeKind
import ai.dadacast.app.practice.PracticeScripts
import ai.dadacast.app.practice.PracticeStep
import ai.dadacast.app.practice.Verdict
import ai.dadacast.app.speech.VoiceRole

/** How soon playback must stop once a signal is active. Ordered from least to most urgent. */
enum class StopTier { CHUNK_BOUNDARY, SENTENCE_END, IMMEDIATE }

enum class DrivingSignal(val tier: StopTier) {
    TURN_GUIDANCE(StopTier.CHUNK_BOUNDARY),
    CAUTION_POINT(StopTier.CHUNK_BOUNDARY),
    TURN_SIGNAL(StopTier.SENTENCE_END),
    /** Another stream, usually navigation guidance, took audio focus. It must not be talked over. */
    OTHER_AUDIO(StopTier.IMMEDIATE),
}

enum class PlayerStatus { IDLE, PLAYING, ENDING_SENTENCE, HELD, RESUMING, PRACTICE, PAUSED, FINISHED, ERROR }

/** Whether the car is standing still. Speaking practice is only offered when it is. */
enum class StopContext { NONE, RED_LIGHT, PARKED }

data class PracticeView(
    val kind: PracticeKind,
    val targetPhrase: String,
    val listening: Boolean,
    /** Speech has ended and the answer is being worked out. */
    val checking: Boolean = false,
    /** Several listening windows in a row heard no sound at all. */
    val noMicInput: Boolean = false,
    val transcript: String?,
    val verdict: Verdict?,
)

data class PlayerSnapshot(
    val status: PlayerStatus,
    val episodeIndex: Int,
    val chunkIndex: Int,
    val sentenceIndex: Int,
    val holdReason: DrivingSignal?,
    val practice: PracticeView? = null,
) {
    /** True while the app owes the driver audio, including while waiting out a driving signal. */
    val wantsAudio: Boolean
        get() = status in setOf(
            PlayerStatus.PLAYING,
            PlayerStatus.ENDING_SENTENCE,
            PlayerStatus.HELD,
            PlayerStatus.RESUMING,
            PlayerStatus.PRACTICE,
        )
}

sealed interface PlayerEvent {
    /** Null means "resume whatever is current". */
    data class Play(val episodeIndex: Int? = null) : PlayerEvent
    data object Pause : PlayerEvent
    data object Stop : PlayerEvent
    data object Next : PlayerEvent
    data object Previous : PlayerEvent
    data class SignalOn(val signal: DrivingSignal) : PlayerEvent
    data class SignalOff(val signal: DrivingSignal) : PlayerEvent
    data class SpeechDone(val token: Long) : PlayerEvent
    data class SpeechFailed(val token: Long) : PlayerEvent
    data class TimerFired(val token: Long) : PlayerEvent
    data class StopChanged(val context: StopContext) : PlayerEvent
    /** Null means nothing usable was heard. */
    data class Heard(val token: Long, val transcript: String?) : PlayerEvent
    data class SpeechEnded(val token: Long) : PlayerEvent
    /** Wipers running (rain): no red-light practice. */
    data class WiperChanged(val running: Boolean) : PlayerEvent
    /** The microphone picked up sound above the noise floor in this listening window. */
    data class InputDetected(val token: Long) : PlayerEvent
    /** Puts back a saved position, paused. Only before anything else has been played. */
    data class Restore(val episodeIndex: Int, val chunkIndex: Int, val sentenceIndex: Int) : PlayerEvent
}

sealed interface PlayerEffect {
    data class Speak(
        val token: Long,
        val text: String,
        val languageTag: String,
        val voice: VoiceRole = VoiceRole.NARRATOR,
    ) : PlayerEffect
    data object StopSpeech : PlayerEffect
    data class StartTimer(val token: Long, val delayMs: Long) : PlayerEffect
    data object CancelTimer : PlayerEffect
    data class Listen(val token: Long, val languageTag: String) : PlayerEffect
    data object StopListening : PlayerEffect
}

/**
 * Decides what is spoken and when to stop. It has no Android or SDK dependency: signals and speech
 * completions come in as [PlayerEvent]s and the service carries out the returned [PlayerEffect]s.
 *
 * Rules:
 * - A sudden signal lets the current sentence finish, then holds. A signal known in advance lets
 *   the current chunk finish. Whichever active signal is most urgent decides.
 * - No chunk starts while any signal is active.
 * - Once every signal clears and [settleDelayMs] passes, playback resumes. A sentence cut off
 *   mid-way is said again from its start; otherwise playback continues with the next sentence.
 *   Earlier sentences of the chunk are never repeated.
 * - While the car is stopped, a chunk boundary may open a short speaking practice. It never
 *   requires an answer, and it ends at once when the car moves; the next chunk then starts.
 * - Previous says the current sentence again from its start; within [PREVIOUS_GRACE_MS] of the
 *   sentence starting it goes one sentence further back. Episodes change through Next or the list.
 * - A sentence the speech engine fails on is tried once more, then skipped. Only after
 *   [MAX_FAILED_SENTENCES] sentences in a row are skipped does playback stop with an error.
 */
class PlaybackStateMachine(
    private val episodes: List<Episode>,
    private val settleDelayMs: Long = DEFAULT_SETTLE_DELAY_MS,
    private val coach: PracticeCoach = LocalCoach(),
    /** Milliseconds from any fixed point; only differences are used. */
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    /** RETRY: a short wait after a speech failure before the next attempt. */
    private enum class Phase { IDLE, SPEAKING, GAP, RETRY, HOLDING, PRACTICE, PAUSED, FINISHED, ERROR }

    private var stopContext = StopContext.NONE
    private val practiceSteps = ArrayDeque<PracticeStep>()
    private var practiceKind: PracticeKind? = null
    private var practicePhrase = ""
    private var listeningFor: PracticeStep.Listen? = null
    private var lastTranscript: String? = null
    private var lastVerdict: Verdict? = null
    private var checking = false
    private var wiperRunning = false
    private var inputThisWindow = false
    private var silentWindows = 0
    private val roleplaysDone = mutableSetOf<String>()

    private var phase = Phase.IDLE
    private var episode = -1
    private var chunk = 0
    private var sentence = 0
    private var settling = false
    private val active = LinkedHashSet<DrivingSignal>()

    /** Position and time of the last sentence handed to the speech engine. */
    private var spokenAt: Triple<Int, Int, Int>? = null
    private var spokenAtMs = 0L
    /** Position whose first attempt failed; a second failure there skips it. */
    private var failedOnce: Triple<Int, Int, Int>? = null
    private var skippedInARow = 0

    /** Identifies the pending speech or timer; anything older is stale and ignored. */
    private var token = 0L

    val snapshot: PlayerSnapshot
        get() = PlayerSnapshot(status(), episode, chunk, sentence, holdReason(), practiceView())

    fun handle(event: PlayerEvent): List<PlayerEffect> {
        val out = mutableListOf<PlayerEffect>()
        when (event) {
            is PlayerEvent.Play -> play(event.episodeIndex, out)
            PlayerEvent.Pause -> if (isRunning()) {
                interrupt(out)
                phase = Phase.PAUSED
            }
            PlayerEvent.Stop -> if (episode >= 0) {
                interrupt(out)
                phase = Phase.IDLE
            }
            PlayerEvent.Next -> if (episode >= 0 && episode + 1 < episodes.size) moveTo(episode + 1, out)
            PlayerEvent.Previous -> if (episode >= 0) previous(out)
            is PlayerEvent.SignalOn -> signalOn(event.signal, out)
            is PlayerEvent.SignalOff -> signalOff(event.signal, out)
            is PlayerEvent.SpeechDone -> when {
                event.token != token -> Unit
                phase == Phase.SPEAKING -> sentenceDone(out)
                phase == Phase.PRACTICE && listeningFor == null -> nextPracticeStep(out)
            }
            is PlayerEvent.SpeechFailed -> if (phase == Phase.PRACTICE && event.token == token) {
                endPractice(out)
            } else if (phase == Phase.SPEAKING && event.token == token) {
                speechFailed(out)
            }
            is PlayerEvent.TimerFired -> if (event.token == token) timerFired(out)
            is PlayerEvent.StopChanged -> stopChanged(event.context, out)
            is PlayerEvent.Restore -> restore(event)
            is PlayerEvent.WiperChanged -> wiperRunning = event.running
            is PlayerEvent.InputDetected -> if (phase == Phase.PRACTICE && listeningFor != null && event.token == token) {
                inputThisWindow = true
                silentWindows = 0
            }
            is PlayerEvent.SpeechEnded -> if (phase == Phase.PRACTICE && listeningFor != null && event.token == token) {
                checking = true
            }
            is PlayerEvent.Heard -> if (phase == Phase.PRACTICE && listeningFor != null && event.token == token) {
                out += PlayerEffect.CancelTimer
                answer(event.transcript, out)
            }
        }
        return out
    }

    private fun play(episodeIndex: Int?, out: MutableList<PlayerEffect>) {
        if (episodeIndex == null && isRunning()) return
        if (episodeIndex != null && episodeIndex !in episodes.indices) return
        if (episodes.isEmpty()) return
        cancelActivity(out)
        when {
            episodeIndex != null -> {
                episode = episodeIndex
                chunk = 0
            }
            episode < 0 -> {
                episode = 0
                chunk = 0
            }
            phase == Phase.FINISHED -> chunk = 0
        }
        if (episodeIndex != null || phase == Phase.FINISHED) sentence = 0
        failedOnce = null
        skippedInARow = 0
        continuePlayback(out)
    }

    /**
     * Says the current sentence again, or the one before it when the current one has only just
     * started (or has not started yet). Crosses chunks backwards; the first sentence just restarts.
     * A pause stays a pause and a hold stays a hold; only the position moves.
     */
    private fun previous(out: MutableList<PlayerEffect>) {
        val here = position()
        val heardFor = if ((phase == Phase.SPEAKING || phase == Phase.GAP) && spokenAt == here) {
            clock() - spokenAtMs
        } else {
            0L
        }
        val wasRunning = isRunning()
        val wasFinished = phase == Phase.FINISHED
        cancelActivity(out)
        if (heardFor < PREVIOUS_GRACE_MS && !wasFinished) stepBack()
        failedOnce = null
        if (wasRunning) continuePlayback(out) else phase = Phase.PAUSED
    }

    /** Moves to the sentence before, across a chunk boundary if needed. Stays on the very first. */
    private fun stepBack() {
        when {
            sentence > 0 -> sentence--
            chunk > 0 -> {
                chunk--
                sentence = episodes[episode].chunks[chunk].sentences.size - 1
            }
        }
    }

    private fun position() = Triple(episode, chunk, sentence)

    /** First failure: try the sentence again. Second: skip it. Too many skipped in a row: stop. */
    private fun speechFailed(out: MutableList<PlayerEffect>) {
        token++
        val here = position()
        if (failedOnce != here) {
            failedOnce = here
        } else {
            failedOnce = null
            skippedInARow++
            if (skippedInARow >= MAX_FAILED_SENTENCES) {
                phase = Phase.ERROR
                return
            }
            if (!stepForward()) {
                phase = Phase.FINISHED
                return
            }
        }
        phase = Phase.RETRY
        out += PlayerEffect.StartTimer(token, RETRY_DELAY_MS)
    }

    private fun restore(saved: PlayerEvent.Restore) {
        if (phase != Phase.IDLE || episode >= 0) return
        val chunks = episodes.getOrNull(saved.episodeIndex)?.chunks ?: return
        val sentences = chunks.getOrNull(saved.chunkIndex)?.sentences ?: return
        if (saved.sentenceIndex !in sentences.indices) return
        episode = saved.episodeIndex
        chunk = saved.chunkIndex
        sentence = saved.sentenceIndex
        phase = Phase.PAUSED
    }

    private fun moveTo(target: Int, out: MutableList<PlayerEffect>) {
        val wasRunning = isRunning()
        cancelActivity(out)
        episode = target
        chunk = 0
        sentence = 0
        if (wasRunning) continuePlayback(out) else phase = Phase.PAUSED
    }

    private fun signalOn(signal: DrivingSignal, out: MutableList<PlayerEffect>) {
        active.remove(signal)
        active.add(signal)
        when (phase) {
            Phase.SPEAKING, Phase.RETRY -> if (signal.tier == StopTier.IMMEDIATE) {
                interrupt(out)
                enterHold(out)
            }
            Phase.GAP -> if (signal.tier >= StopTier.SENTENCE_END) {
                if (interrupt(out)) enterHold(out) else phase = Phase.FINISHED
            }
            Phase.HOLDING -> if (settling && isBlocked()) {
                cancelActivity(out)
                settling = false
            }
            // Other audio must never be talked over; the practice ends and playback holds.
            Phase.PRACTICE -> if (signal.tier == StopTier.IMMEDIATE) {
                interrupt(out)
                enterHold(out)
            }
            else -> Unit
        }
    }

    private fun signalOff(signal: DrivingSignal, out: MutableList<PlayerEffect>) {
        active.remove(signal)
        if (phase == Phase.HOLDING && !isBlocked() && !settling) startSettle(out)
    }

    private fun sentenceDone(out: MutableList<PlayerEffect>) {
        token++
        failedOnce = null
        skippedInARow = 0
        val tier = currentTier()
        if (tier != null && tier >= StopTier.SENTENCE_END) {
            // Nothing was cut off: the hold resumes with the next sentence.
            if (stepForward()) enterHold(out) else phase = Phase.FINISHED
            return
        }
        val pauseMs = currentSentence().pauseAfterMs
        if (pauseMs > 0) {
            phase = Phase.GAP
            out += PlayerEffect.StartTimer(token, pauseMs)
        } else {
            advance(out)
        }
    }

    private fun timerFired(out: MutableList<PlayerEffect>) {
        if (phase == Phase.PRACTICE && listeningFor != null) {
            out += PlayerEffect.StopListening
            answer(null, out)
            return
        }
        token++
        when (phase) {
            Phase.GAP -> advance(out)
            Phase.RETRY -> continuePlayback(out)
            Phase.HOLDING -> {
                settling = false
                continuePlayback(out)
            }
            else -> Unit
        }
    }

    private fun advance(out: MutableList<PlayerEffect>) {
        val chunks = episodes[episode].chunks
        when {
            sentence + 1 < chunks[chunk].sentences.size -> {
                sentence++
                speak(out)
            }
            chunk + 1 < chunks.size -> {
                val finished = chunks[chunk]
                chunk++
                sentence = 0
                if (!startPractice(finished.practice, out)) continuePlayback(out)
            }
            else -> phase = Phase.FINISHED
        }
    }

    /**
     * Speaks the current sentence, or holds. A chunk does not start while any signal is active;
     * inside a chunk only signals that stop at a sentence end or sooner keep it held.
     */
    private fun continuePlayback(out: MutableList<PlayerEffect>) {
        if (isBlocked()) enterHold(out) else speak(out)
    }

    private fun isBlocked(): Boolean =
        if (sentence == 0) active.isNotEmpty() else active.any { it.tier >= StopTier.SENTENCE_END }

    /**
     * Ends the current activity and leaves the position where playback should pick up again:
     * a sentence cut off mid-way is said again, a silence between sentences is skipped.
     */
    private fun interrupt(out: MutableList<PlayerEffect>): Boolean {
        val inGap = phase == Phase.GAP
        cancelActivity(out)
        return !inGap || stepForward()
    }

    /** Moves to the next sentence, across a chunk boundary if needed. False at the episode end. */
    private fun stepForward(): Boolean {
        val chunks = episodes[episode].chunks
        return when {
            sentence + 1 < chunks[chunk].sentences.size -> {
                sentence++
                true
            }
            chunk + 1 < chunks.size -> {
                chunk++
                sentence = 0
                true
            }
            else -> false
        }
    }

    private fun speak(out: MutableList<PlayerEffect>) {
        phase = Phase.SPEAKING
        token++
        val current = currentSentence()
        spokenAt = position()
        spokenAtMs = clock()
        out += PlayerEffect.Speak(token, current.text, current.languageTag, current.voice)
    }

    private fun enterHold(out: MutableList<PlayerEffect>) {
        phase = Phase.HOLDING
        settling = false
        if (!isBlocked()) startSettle(out)
    }

    private fun startSettle(out: MutableList<PlayerEffect>) {
        settling = true
        token++
        out += PlayerEffect.StartTimer(token, settleDelayMs)
    }

    private fun stopChanged(context: StopContext, out: MutableList<PlayerEffect>) {
        stopContext = context
        if (phase == Phase.PRACTICE && context == StopContext.NONE) {
            // The car is moving: stop listening and talking now, then go on with the episode.
            cancelActivity(out)
            continuePlayback(out)
        }
    }

    /** Starts a practice at a chunk boundary if the car is stopped and there is something to practise. */
    private fun startPractice(
        finished: ai.dadacast.app.content.RedLightPractice?,
        out: MutableList<PlayerEffect>,
    ): Boolean {
        if (stopContext == StopContext.NONE || active.isNotEmpty()) return false
        val roleplay = episodes[episode].roleplays.firstOrNull { it.id !in roleplaysDone }
        val script = when {
            stopContext == StopContext.PARKED && roleplay != null -> {
                roleplaysDone += roleplay.id
                PracticeScripts.roleplay(roleplay)
            }
            // In the rain the driver needs more attention, so the short stop practice rests.
            finished != null && !wiperRunning -> PracticeScripts.redLight(finished)
            else -> return false
        }
        phase = Phase.PRACTICE
        practiceKind = script.kind
        practicePhrase = script.targetPhrase
        lastTranscript = null
        lastVerdict = null
        practiceSteps.clear()
        practiceSteps.addAll(script.steps)
        nextPracticeStep(out)
        return true
    }

    private fun nextPracticeStep(out: MutableList<PlayerEffect>) {
        checking = false
        val step = practiceSteps.removeFirstOrNull() ?: return endPractice(out)
        token++
        when (step) {
            is PracticeStep.Say -> {
                listeningFor = null
                out += PlayerEffect.Speak(token, step.text, step.languageTag, step.voice)
            }
            is PracticeStep.Listen -> {
                listeningFor = step
                inputThisWindow = false
                practicePhrase = step.sampleAnswer
                lastTranscript = null
                lastVerdict = null
                out += PlayerEffect.Listen(token, step.languageTag)
                out += PlayerEffect.StartTimer(token, step.timeoutMs)
            }
        }
    }

    private fun answer(transcript: String?, out: MutableList<PlayerEffect>) {
        val listen = listeningFor ?: return
        silentWindows = if (inputThisWindow || transcript != null) 0 else silentWindows + 1
        listeningFor = null
        val reply = coach.reply(listen, transcript)
        lastTranscript = transcript
        lastVerdict = reply.verdict
        if (reply.endPractice) practiceSteps.clear()
        reply.lines.asReversed().forEach(practiceSteps::addFirst)
        nextPracticeStep(out)
    }

    private fun endPractice(out: MutableList<PlayerEffect>) {
        clearPractice()
        token++
        continuePlayback(out)
    }

    private fun clearPractice() {
        practiceSteps.clear()
        practiceKind = null
        listeningFor = null
    }

    /** Ends whatever speech, listening or timer is outstanding and invalidates its token. */
    private fun cancelActivity(out: MutableList<PlayerEffect>) {
        when {
            phase == Phase.SPEAKING -> out += PlayerEffect.StopSpeech
            phase == Phase.PRACTICE && listeningFor != null -> {
                out += PlayerEffect.StopListening
                out += PlayerEffect.CancelTimer
            }
            phase == Phase.PRACTICE -> out += PlayerEffect.StopSpeech
            phase == Phase.GAP || phase == Phase.RETRY || (phase == Phase.HOLDING && settling) ->
                out += PlayerEffect.CancelTimer
        }
        if (phase == Phase.PRACTICE) clearPractice()
        settling = false
        token++
    }

    private fun isRunning() =
        phase == Phase.SPEAKING || phase == Phase.GAP || phase == Phase.RETRY || phase == Phase.HOLDING ||
            phase == Phase.PRACTICE

    private fun practiceView(): PracticeView? {
        val kind = practiceKind ?: return null
        if (phase != Phase.PRACTICE) return null
        return PracticeView(
            kind = kind,
            targetPhrase = practicePhrase,
            listening = listeningFor != null && !checking,
            checking = listeningFor != null && checking,
            noMicInput = silentWindows >= NO_INPUT_WINDOWS,
            transcript = lastTranscript,
            verdict = lastVerdict,
        )
    }

    private fun currentSentence() = episodes[episode].chunks[chunk].sentences[sentence]

    private fun currentTier(): StopTier? = active.maxOfOrNull { it.tier }

    /** The most urgent active signal; the latest one wins a tie. */
    private fun holdReason(): DrivingSignal? =
        active.toList().asReversed().maxByOrNull { it.tier }

    private fun status(): PlayerStatus = when (phase) {
        Phase.IDLE -> PlayerStatus.IDLE
        Phase.SPEAKING ->
            if ((currentTier() ?: StopTier.CHUNK_BOUNDARY) >= StopTier.SENTENCE_END) {
                PlayerStatus.ENDING_SENTENCE
            } else {
                PlayerStatus.PLAYING
            }
        Phase.GAP, Phase.RETRY -> PlayerStatus.PLAYING
        Phase.HOLDING -> if (settling) PlayerStatus.RESUMING else PlayerStatus.HELD
        Phase.PRACTICE -> PlayerStatus.PRACTICE
        Phase.PAUSED -> PlayerStatus.PAUSED
        Phase.FINISHED -> PlayerStatus.FINISHED
        Phase.ERROR -> PlayerStatus.ERROR
    }

    companion object {
        const val DEFAULT_SETTLE_DELAY_MS = 1_500L
        /** Listening windows in a row with no sound before the "no microphone input" hint shows. */
        const val NO_INPUT_WINDOWS = 2
        /** Within this long of a sentence starting, Previous goes to the sentence before it. */
        const val PREVIOUS_GRACE_MS = 2_000L
        /** Wait before trying again after the speech engine fails on a sentence. */
        const val RETRY_DELAY_MS = 500L
        /** Sentences skipped in a row, each after two failed tries, before playback stops. */
        const val MAX_FAILED_SENTENCES = 3
    }
}

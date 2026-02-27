package assistant.core.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * End-to-end flow:
 * 1) idle listening for wake-word
 * 2) play cue tone and capture utterance
 * 3) transcribe
 * 4) route to intent system
 */
class VoiceCommandPipeline(
    private val wakeWordEngine: WakeWordEngine,
    private val speechCaptureManager: SpeechCaptureManager,
    private val speechToTextProvider: SpeechToTextProvider,
    private val intentRouter: IntentRouter,
    private val cuePlayer: CueTonePlayer,
    private val promptPlayer: PromptPlayer,
    private val minConfidence: Float = 0.45f,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    suspend fun start() {
        wakeWordEngine.startListening {
            scope.launch {
                handleWakeEvent()
            }
        }
    }

    suspend fun stop() {
        wakeWordEngine.stopListening()
    }

    fun close() {
        wakeWordEngine.close()
        scope.cancel()
    }

    private suspend fun handleWakeEvent() {
        cuePlayer.playWakeCue()

        when (val captureResult = speechCaptureManager.captureCommand()) {
            CaptureResult.Empty -> {
                promptPlayer.say("I didn't catch that.")
            }

            is CaptureResult.Audio -> {
                val transcription = speechToTextProvider.transcribe(captureResult.pcm16)
                val transcript = transcription.text
                if (transcript.isBlank() || transcription.confidence < minConfidence) {
                    promptPlayer.say("I didn't catch that.")
                    return
                }
                intentRouter.route(transcript)
            }
        }
    }
}

fun interface IntentRouter {
    suspend fun route(transcript: String)
}

fun interface CueTonePlayer {
    suspend fun playWakeCue()
}

fun interface PromptPlayer {
    suspend fun say(text: String)
}

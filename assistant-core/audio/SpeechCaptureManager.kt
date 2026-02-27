package assistant.core.audio

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Captures a single user utterance after wake-word detection.
 * Stops capture when VAD determines end-of-speech or when timeout is reached.
 */
class SpeechCaptureManager(
    private val recorder: UtteranceRecorder,
    private val vad: VoiceActivityDetector,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun captureCommand(config: CaptureConfig = CaptureConfig()): CaptureResult = withContext(dispatcher) {
        recorder.start()
        val samples = mutableListOf<ShortArray>()
        var silenceMs = 0L
        val startMs = System.currentTimeMillis()

        while (System.currentTimeMillis() - startMs < config.maxCaptureMs) {
            val chunk = recorder.readChunk()
            if (chunk.isEmpty()) continue

            samples += chunk
            if (vad.isSpeech(chunk)) {
                silenceMs = 0L
            } else {
                silenceMs += config.chunkDurationMs
                if (silenceMs >= config.endOfSpeechSilenceMs) {
                    break
                }
            }
        }

        recorder.stop()

        val flattened = flatten(samples)
        if (flattened.isEmpty()) {
            return@withContext CaptureResult.Empty
        }

        CaptureResult.Audio(flattened)
    }

    private fun flatten(chunks: List<ShortArray>): ShortArray {
        val totalLength = chunks.sumOf { it.size }
        val result = ShortArray(totalLength)
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, destinationOffset = offset)
            offset += chunk.size
        }
        return result
    }
}

data class CaptureConfig(
    val maxCaptureMs: Long = 8_000L,
    val endOfSpeechSilenceMs: Long = 1_200L,
    val chunkDurationMs: Long = 100L,
)

sealed interface CaptureResult {
    data object Empty : CaptureResult
    data class Audio(val pcm16: ShortArray) : CaptureResult
}

fun interface VoiceActivityDetector {
    fun isSpeech(audioChunk: ShortArray): Boolean
}

interface UtteranceRecorder {
    suspend fun start()
    suspend fun readChunk(): ShortArray
    suspend fun stop()
}

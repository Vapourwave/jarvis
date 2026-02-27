package assistant.core.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Listens for the wake word and emits debounced wake events.
 */
class WakeWordEngine(
    private val detector: HotwordDetector,
    private val audioInput: AudioInputStream,
    private val lockoutMillis: Long = DEFAULT_LOCKOUT_MS,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mutex = Mutex()
    private var streamJob: Job? = null
    private var lastWakeTimestamp: Long = 0L
    @Volatile
    private var listening: Boolean = false

    suspend fun startListening(onWakeWordDetected: suspend () -> Unit) {
        mutex.withLock {
            if (listening) return
            listening = true
            streamJob = coroutineScope.launch {
                audioInput.collectFrames { frame ->
                    if (detector.detect(frame) && shouldEmitWakeEvent()) {
                        onWakeWordDetected()
                    }
                }
            }
        }
    }

    suspend fun stopListening() {
        mutex.withLock {
            listening = false
            streamJob?.cancel()
            streamJob = null
        }
    }

    fun close() {
        coroutineScope.cancel()
    }

    private suspend fun shouldEmitWakeEvent(now: Long = System.currentTimeMillis()): Boolean {
        return mutex.withLock {
            if (now - lastWakeTimestamp < lockoutMillis) {
                return@withLock false
            }
            lastWakeTimestamp = now
            true
        }
    }

    companion object {
        const val DEFAULT_LOCKOUT_MS: Long = 2_500L
    }
}

/**
 * Input stream abstraction to keep DSP/audio source pluggable.
 */
fun interface AudioInputStream {
    suspend fun collectFrames(onFrame: suspend (FloatArray) -> Unit)
}

/**
 * Hotword detector abstraction; runtime can provide Picovoice, Snowboy, or custom model.
 */
fun interface HotwordDetector {
    fun detect(frame: FloatArray): Boolean
}

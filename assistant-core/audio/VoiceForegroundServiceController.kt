package assistant.core.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps wake-word flow alive in foreground-service contexts even when screen is off.
 * Battery-managed mode can be toggled by user settings.
 */
class VoiceForegroundServiceController(
    private val voicePipeline: VoiceCommandPipeline,
    private val serviceHooks: ForegroundServiceHooks,
    private val settingsStore: VoiceServiceSettingsStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val running = AtomicBoolean(false)

    suspend fun onServiceCreate() {
        serviceHooks.startForegroundNotification()
        serviceHooks.registerScreenStateListener(
            onScreenOff = { onScreenOff() },
            onScreenOn = { onScreenOn() },
        )

        if (settingsStore.isWakeWordEnabled()) {
            running.set(true)
            voicePipeline.start()
        }
    }

    suspend fun onServiceDestroy() {
        if (running.compareAndSet(true, false)) {
            voicePipeline.stop()
        }
        serviceHooks.unregisterScreenStateListener()
        voicePipeline.close()
        scope.cancel()
    }

    suspend fun onWakeWordToggleChanged(enabled: Boolean) {
        settingsStore.setWakeWordEnabled(enabled)
        if (enabled && running.compareAndSet(false, true)) {
            voicePipeline.start()
        } else if (!enabled && running.compareAndSet(true, false)) {
            voicePipeline.stop()
        }
    }

    private fun onScreenOff() {
        scope.launch {
            if (settingsStore.isBatteryManagedModeEnabled()) {
                serviceHooks.reduceMicDutyCycle()
            } else {
                serviceHooks.acquirePartialWakeLock()
            }
        }
    }

    private fun onScreenOn() {
        scope.launch {
            serviceHooks.releasePartialWakeLock()
            serviceHooks.restoreNormalMicDutyCycle()
        }
    }
}

interface ForegroundServiceHooks {
    suspend fun startForegroundNotification()
    fun registerScreenStateListener(onScreenOff: () -> Unit, onScreenOn: () -> Unit)
    fun unregisterScreenStateListener()
    suspend fun acquirePartialWakeLock()
    suspend fun releasePartialWakeLock()
    suspend fun reduceMicDutyCycle()
    suspend fun restoreNormalMicDutyCycle()
}

interface VoiceServiceSettingsStore {
    suspend fun isWakeWordEnabled(): Boolean
    suspend fun setWakeWordEnabled(enabled: Boolean)
    suspend fun isBatteryManagedModeEnabled(): Boolean
}

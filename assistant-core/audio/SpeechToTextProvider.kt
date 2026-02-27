package assistant.core.audio

/**
 * Speech-to-text provider abstraction with local and remote implementations.
 */
interface SpeechToTextProvider {
    suspend fun transcribe(audioPcm16: ShortArray): TranscriptionResult
}

data class TranscriptionResult(
    val text: String,
    val confidence: Float,
)

class LocalSpeechToTextProvider(
    private val engine: LocalSttEngine,
) : SpeechToTextProvider {
    override suspend fun transcribe(audioPcm16: ShortArray): TranscriptionResult {
        val result = engine.transcribe(audioPcm16)
        return TranscriptionResult(
            text = result.text.trim(),
            confidence = result.confidence,
        )
    }
}

class RemoteSpeechToTextProvider(
    private val client: RemoteSttClient,
) : SpeechToTextProvider {
    override suspend fun transcribe(audioPcm16: ShortArray): TranscriptionResult {
        val response = client.transcribe(audioPcm16)
        return TranscriptionResult(
            text = response.text.trim(),
            confidence = response.confidence,
        )
    }
}

data class LocalSttResult(val text: String, val confidence: Float)
fun interface LocalSttEngine {
    suspend fun transcribe(audioPcm16: ShortArray): LocalSttResult
}

data class RemoteSttResponse(val text: String, val confidence: Float)
fun interface RemoteSttClient {
    suspend fun transcribe(audioPcm16: ShortArray): RemoteSttResponse
}

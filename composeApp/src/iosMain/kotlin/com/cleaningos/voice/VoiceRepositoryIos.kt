package com.cleaningos.voice

import com.cleaningos.domain.model.VoiceState
import com.cleaningos.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** iOS stub — implement with SFSpeechRecognizer in production */
class VoiceRepositoryIos : VoiceRepository {
    override val isAvailable: Boolean = false
    override fun startListening(): Flow<VoiceState> = flowOf(VoiceState(error = "iOS STT: implement SFSpeechRecognizer"))
    override fun stopListening() {}
}

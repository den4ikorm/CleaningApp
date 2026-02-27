package com.cleaningos.voice.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.speech.SpeechRecognizer
import com.cleaningos.domain.model.VoiceState
import com.cleaningos.domain.repository.VoiceRepository
import com.cleaningos.voice.service.SpeechRecognitionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class VoiceRepositoryImpl(private val context: Context) : VoiceRepository {

    private var voiceService: SpeechRecognitionService? = null
    private val _serviceBound = MutableStateFlow<SpeechRecognitionService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            voiceService = (binder as SpeechRecognitionService.VoiceBinder).getService()
            _serviceBound.value = voiceService
        }
        override fun onServiceDisconnected(name: ComponentName) {
            voiceService = null
            _serviceBound.value = null
        }
    }

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening(): Flow<VoiceState> {
        val intent = Intent(context, SpeechRecognitionService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        context.startService(intent)

        return _serviceBound.filterNotNull().flatMapLatest { service ->
            service.startRecognition()
            service.voiceFlow
        }
    }

    override fun stopListening() {
        voiceService?.stopRecognition()
        context.unbindService(connection)
    }
}

package com.cleaningos.voice.service

import android.app.*
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.speech.*
import androidx.core.app.NotificationCompat
import com.cleaningos.domain.model.VoiceState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * SpeechRecognitionService — Background Service for continuous STT.
 *
 * Audio focus management:
 *   1. Voice command detected → request AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
 *   2. AudioManager ducks music volume automatically (or we call player.setVolume(0.3f))
 *   3. Recognition complete → abandon focus → music restores to full volume
 *
 * All results flow via Channel → VoiceRepository → ViewModel → State.
 */
class SpeechRecognitionService : Service() {

    companion object {
        const val CHANNEL_ID      = "cleaning_os_voice"
        const val NOTIFICATION_ID = 1002
    }

    inner class VoiceBinder : Binder() {
        fun getService(): SpeechRecognitionService = this@SpeechRecognitionService
    }

    private val binder = VoiceBinder()

    // Speech recognizer (must run on main thread)
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Audio focus
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // State output
    private val _voiceChannel = Channel<VoiceState>(Channel.BUFFERED)
    val voiceFlow: Flow<VoiceState> = _voiceChannel.receiveAsFlow()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        stopRecognition()
        super.onDestroy()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun startRecognition() {
        requestAudioFocus()
        startForeground(NOTIFICATION_ID, buildNotification())
        mainHandler.post { initAndStartRecognizer() }
    }

    fun stopRecognition() {
        mainHandler.post {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        _voiceChannel.trySend(VoiceState(isListening = false))
    }

    // ── SpeechRecognizer ──────────────────────────────────────────────────────

    private fun initAndStartRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            _voiceChannel.trySend(VoiceState(error = "STT недоступен на этом устройстве"))
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceChannel.trySend(VoiceState(isListening = true))
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: return
                    _voiceChannel.trySend(VoiceState(isListening = true, transcript = partial, isFinal = false))
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    val confidence = results
                        ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        ?.firstOrNull() ?: 0f
                    _voiceChannel.trySend(
                        VoiceState(isListening = false, transcript = text, confidence = confidence, isFinal = true)
                    )
                    abandonAudioFocus()
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH          -> "Речь не распознана"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT    -> "Тайм-аут — говорите громче"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на микрофон"
                        SpeechRecognizer.ERROR_NETWORK           -> "Нет сети для STT"
                        else                                     -> "Ошибка STT: $error"
                    }
                    _voiceChannel.trySend(VoiceState(error = message))
                    abandonAudioFocus()
                }

                override fun onBeginningOfSpeech()                  {}
                override fun onBufferReceived(buffer: ByteArray?)    {}
                override fun onEndOfSpeech()                         {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float)              {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,       "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,     3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        speechRecognizer?.startListening(intent)
    }

    // ── Audio Focus — Ducking Protocol ────────────────────────────────────────

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                // Music player will automatically duck (reduce volume) via AudioManager
                // For manual ducking: playerController.setVolume(if (focusChange == ...) 0.3f else 1.0f)
            }
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setWillPauseWhenDucked(false)  // let AudioManager duck automatically
                .build()
                .also { audioManager.requestAudioFocus(it) }
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "Голосовой ввод", NotificationManager.IMPORTANCE_LOW)
                .also { getSystemService(NotificationManager::class.java).createNotificationChannel(it) }
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cleaning OS — Слушаю...")
            .setContentText("Скажите команду")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .build()
}

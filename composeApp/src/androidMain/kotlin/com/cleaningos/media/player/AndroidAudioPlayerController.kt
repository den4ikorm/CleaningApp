package com.cleaningos.media.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.cleaningos.domain.model.AudioTrack
import com.cleaningos.domain.model.MusicState
import com.cleaningos.domain.repository.AudioPlayerController
import com.cleaningos.media.service.AudioPlayerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AndroidAudioPlayerController — bridges domain interface with AudioPlayerService.
 * Binds to the service and delegates all commands to it.
 */
class AndroidAudioPlayerController(private val context: Context) : AudioPlayerController {

    private var playerService: AudioPlayerService? = null
    private val _state = MutableStateFlow(MusicState())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = (binder as AudioPlayerService.PlayerBinder).getService()
            playerService = service
            // Forward service state to our StateFlow
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                service.musicState.collect { _state.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            playerService = null
        }
    }

    init { bindService() }

    private fun bindService() {
        val intent = Intent(context, AudioPlayerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun play(track: AudioTrack)    { playerService?.play(track) }
    override fun pause()                    { playerService?.pause() }
    override fun resume()                   { playerService?.resume() }
    override fun stop()                     { playerService?.stop() }
    override fun seekTo(positionMs: Long)   { playerService?.seekTo(positionMs) }
    override fun next()                     { playerService?.next() }
    override fun previous()                 { playerService?.previous() }
    override fun setVolume(volume: Float)   { playerService?.setVolume(volume) }
    override fun observeState(): Flow<MusicState> = _state.asStateFlow()
}

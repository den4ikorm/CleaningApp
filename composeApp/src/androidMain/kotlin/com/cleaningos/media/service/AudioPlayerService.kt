package com.cleaningos.media.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.cleaningos.domain.model.AudioTrack
import com.cleaningos.domain.model.MusicState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * AudioPlayerService — Android Foreground Service with ExoPlayer + MediaSession.
 *
 * Architecture:
 *   - Foreground service keeps playback alive when app is backgrounded
 *   - MediaSession enables lock screen / notification controls
 *   - Exposes MusicState via StateFlow, consumed by MusicViewModel
 *   - Audio focus management integrated via ExoPlayer's built-in handling
 */
@OptIn(UnstableApi::class)
class AudioPlayerService : Service() {

    companion object {
        const val CHANNEL_ID      = "cleaning_os_media"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY     = "action_play"
        const val ACTION_PAUSE    = "action_pause"
        const val ACTION_NEXT     = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_STOP     = "action_stop"
    }

    inner class PlayerBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    private val binder = PlayerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ExoPlayer instance (lazy — created on bind)
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSession

    // State exposed to ViewModel
    private val _musicState = MutableStateFlow(MusicState())
    val musicState: StateFlow<MusicState> = _musicState.asStateFlow()

    private var playlist: List<AudioTrack> = emptyList()
    private var currentIndex: Int = -1

    // Position update job
    private var positionJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initPlayer()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY     -> resume()
            ACTION_PAUSE    -> pause()
            ACTION_NEXT     -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_STOP     -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        positionJob?.cancel()
        serviceScope.cancel()
        mediaSession.release()
        exoPlayer.release()
        super.onDestroy()
    }

    // ── Player Initialization ─────────────────────────────────────────────────

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)  // pause on headphone disconnect
            .build()

        mediaSession = MediaSession.Builder(this, exoPlayer).build()

        // Listen to player state changes → update MusicState
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState { copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionTracking() else stopPositionTracking()
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: android.media3.common.MediaItem?, reason: Int) {
                val track = playlist.getOrNull(exoPlayer.currentMediaItemIndex)
                updateState { copy(currentTrack = track) }
                updateNotification()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                updateState { copy(error = error.message) }
            }
        })
    }

    // ── Playback Controls ─────────────────────────────────────────────────────

    fun play(track: AudioTrack, newPlaylist: List<AudioTrack> = listOf(track)) {
        playlist = newPlaylist
        currentIndex = newPlaylist.indexOf(track).coerceAtLeast(0)

        val mediaItems = newPlaylist.map {
            MediaItem.fromUri(android.net.Uri.parse(it.uri))
        }
        exoPlayer.setMediaItems(mediaItems, currentIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        updateState { copy(playlist = newPlaylist, currentTrack = track) }
        startForeground(NOTIFICATION_ID, buildNotification(track))
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        updateState { MusicState() }
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem()
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    /**
     * Set volume — used by VoiceService for audio ducking when STT is active.
     * Reduces music volume while voice command is being recognized.
     */
    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    // ── Position Tracking ─────────────────────────────────────────────────────

    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = serviceScope.launch {
            while (isActive) {
                updateState { copy(currentPositionMs = exoPlayer.currentPosition) }
                delay(500L)
            }
        }
    }

    private fun stopPositionTracking() {
        positionJob?.cancel()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "Cleaning OS — Музыка",
                NotificationManager.IMPORTANCE_LOW
            ).also {
                it.description = "Управление воспроизведением"
                getSystemService(NotificationManager::class.java).createNotificationChannel(it)
            }
        }
    }

    private fun buildNotification(track: AudioTrack): Notification {
        val playPauseAction = if (exoPlayer.isPlaying) {
            notificationAction(ACTION_PAUSE, "Pause", android.R.drawable.ic_media_pause)
        } else {
            notificationAction(ACTION_PLAY, "Play", android.R.drawable.ic_media_play)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(notificationAction(ACTION_PREVIOUS, "Prev", android.R.drawable.ic_media_previous))
            .addAction(playPauseAction)
            .addAction(notificationAction(ACTION_NEXT, "Next", android.R.drawable.ic_media_next))
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(exoPlayer.isPlaying)
            .setSilent(true)
            .build()
    }

    private fun notificationAction(action: String, title: String, icon: Int): NotificationCompat.Action {
        val intent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioPlayerService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(icon, title, intent)
    }

    private fun updateNotification() {
        val track = _musicState.value.currentTrack ?: return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(track))
    }

    // ── State Helper ──────────────────────────────────────────────────────────

    private fun updateState(reducer: MusicState.() -> MusicState) {
        _musicState.update { it.reducer() }
    }
}

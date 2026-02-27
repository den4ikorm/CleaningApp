package com.cleaningos.presentation.features.dashboard

import com.cleaningos.core.mvi.BaseViewModel
import com.cleaningos.core.mvi.UiEffect
import com.cleaningos.core.mvi.UiEvent
import com.cleaningos.core.mvi.UiState
import com.cleaningos.domain.model.AudioTrack
import com.cleaningos.domain.model.MusicState
import com.cleaningos.domain.repository.AudioPlayerController
import com.cleaningos.domain.repository.MediaRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// ── MusicViewModel Contract ───────────────────────────────────────────────────

data class MusicUiState(
    val musicState: MusicState = MusicState(),
    val isScanning: Boolean = false,
    val error: String? = null
) : UiState

sealed interface MusicEvent : UiEvent {
    data object ScanLibrary : MusicEvent
    data class PlayTrack(val track: AudioTrack) : MusicEvent
    data object PlayPause : MusicEvent
    data object Next : MusicEvent
    data object Previous : MusicEvent
    data class SeekTo(val positionMs: Long) : MusicEvent
    data class SetVolume(val volume: Float) : MusicEvent
    data object Stop : MusicEvent
}

sealed interface MusicEffect : UiEffect {
    data class ShowError(val message: String) : MusicEffect
    data class RequestPermission(val permission: String) : MusicEffect
}

/**
 * MusicViewModel — manages music playback via MVI.
 *
 * Thin Screen rule enforced:
 *   - UI dispatches PlayTrack / PlayPause / Next events
 *   - ViewModel controls AudioPlayerController
 *   - MusicUiState flows back to UI (isPlaying, currentTrack, etc.)
 */
class MusicViewModel(
    private val mediaRepository: MediaRepository,
    private val audioPlayer: AudioPlayerController
) : BaseViewModel<MusicUiState, MusicEvent, MusicEffect>(MusicUiState()) {

    init { observePlayerState() }

    override suspend fun handleEvent(event: MusicEvent) {
        when (event) {
            is MusicEvent.ScanLibrary  -> scanLibrary()
            is MusicEvent.PlayTrack    -> audioPlayer.play(event.track)
            is MusicEvent.PlayPause    -> handlePlayPause()
            is MusicEvent.Next         -> audioPlayer.next()
            is MusicEvent.Previous     -> audioPlayer.previous()
            is MusicEvent.SeekTo       -> audioPlayer.seekTo(event.positionMs)
            is MusicEvent.SetVolume    -> audioPlayer.setVolume(event.volume)
            is MusicEvent.Stop         -> audioPlayer.stop()
        }
    }

    private fun scanLibrary() {
        launchSafe(onError = { e ->
            setState { copy(isScanning = false, error = e.message) }
            if (e is SecurityException) {
                setEffect(MusicEffect.RequestPermission(android.Manifest.permission.READ_MEDIA_AUDIO))
            }
        }) {
            setState { copy(isScanning = true) }
            val tracks = mediaRepository.scanAudioFiles()
            val currentMusic = currentState.musicState
            setState { copy(isScanning = false, musicState = currentMusic.copy(playlist = tracks)) }

            // Auto-play first track if nothing is playing
            if (tracks.isNotEmpty() && currentMusic.currentTrack == null) {
                audioPlayer.play(tracks.first())
            }
        }
    }

    private fun handlePlayPause() {
        val music = currentState.musicState
        if (music.isPlaying) audioPlayer.pause() else {
            if (music.currentTrack != null) audioPlayer.resume()
            else setEffect(MusicEffect.ShowError("Нет треков. Сканируйте библиотеку."))
        }
    }

    private fun observePlayerState() {
        audioPlayer.observeState()
            .onEach { playerState -> setState { copy(musicState = playerState) } }
            .catch { /* non-critical */ }
            .launchIn(screenModelScope)
    }

    override fun handleError(throwable: Throwable) {
        setState { copy(error = throwable.message) }
    }
}

package com.cleaningos.presentation.features.dashboard

import com.cleaningos.core.mvi.BaseViewModel
import com.cleaningos.domain.model.EdiScore
import com.cleaningos.domain.model.MusicState
import com.cleaningos.domain.model.VoiceState
import com.cleaningos.domain.repository.AudioPlayerController
import com.cleaningos.domain.repository.ObjectRepository
import com.cleaningos.domain.repository.SearchRepository
import com.cleaningos.domain.repository.VoiceRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * DashboardViewModel — all business logic for the Dashboard.
 *
 * "Thin Screen" protocol enforced:
 *   ✅ Handles all data fetching, transformation, and side-effects
 *   ✅ Exposes only immutable DashboardState to the Composable
 *   ❌ No logic in DashboardScreen.kt
 */
class DashboardViewModel(
    private val searchRepository: SearchRepository,
    private val objectRepository: ObjectRepository,
    private val audioPlayer: AudioPlayerController,
    private val voiceRepository: VoiceRepository
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(
    initialState = DashboardState()
) {

    init {
        observeMusicState()
        loadRecentObjects()
    }

    // ── Event Router ──────────────────────────────────────────────────────────
    override suspend fun handleEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.QueryChanged      -> setState { copy(query = event.text) }
            is DashboardEvent.SearchClicked     -> performSearch(currentState.query)
            is DashboardEvent.VoiceClicked      -> handleVoiceClick()
            is DashboardEvent.QuickActionClicked -> performSearch(event.action.query)
            is DashboardEvent.OpenChecklist     -> setEffect(DashboardEffect.OpenChecklistScreen(event.checklist))
            is DashboardEvent.DismissError      -> setState { copy(errorMessage = null) }

            // Music
            is DashboardEvent.PlayPauseClicked  -> handlePlayPause()
            is DashboardEvent.NextTrackClicked  -> audioPlayer.next()
            is DashboardEvent.PrevTrackClicked  -> audioPlayer.previous()

            // Voice result
            is DashboardEvent.VoiceActivated    -> startVoiceListening()
            is DashboardEvent.VoiceResult       -> {
                setState { copy(query = event.transcript, voiceState = VoiceState()) }
                performSearch(event.transcript)
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private fun performSearch(text: String) {
        if (text.isBlank()) return
        launchSafe(onError = { e ->
            setState { copy(isLoading = false, errorMessage = e.message ?: "Search error") }
        }) {
            setState { copy(isLoading = true, searchResult = null, errorMessage = null) }

            val result = searchRepository.query(text)

            setState {
                copy(
                    isLoading    = false,
                    edi          = result.edi,
                    searchResult = result,
                    checklist    = result.checklist,
                )
            }

            // Trigger safety effect immediately
            if (result.safetyAlert) {
                setEffect(DashboardEffect.ShowSnackbar("🚨 ${result.alertText}"))
            }
        }
    }

    // ── Voice ─────────────────────────────────────────────────────────────────
    private fun handleVoiceClick() {
        if (!voiceRepository.isAvailable) {
            setEffect(DashboardEffect.ShowSnackbar("Голосовой ввод недоступен"))
            return
        }
        setEffect(DashboardEffect.ShowVoiceDialog)
    }

    private fun startVoiceListening() {
        voiceRepository.startListening()
            .onEach { voiceState ->
                setState { copy(voiceState = voiceState) }
                if (voiceState.isFinal && voiceState.transcript.isNotBlank()) {
                    dispatch(DashboardEvent.VoiceResult(voiceState.transcript))
                    voiceRepository.stopListening()
                }
            }
            .catch { e -> setState { copy(voiceState = VoiceState(error = e.message)) } }
            .launchIn(screenModelScope)
    }

    // ── Music ─────────────────────────────────────────────────────────────────
    private fun handlePlayPause() {
        val music = currentState.musicState
        if (music.isPlaying) audioPlayer.pause() else {
            music.currentTrack?.let { audioPlayer.play(it) }
                ?: setEffect(DashboardEffect.ShowSnackbar("Нет треков для воспроизведения"))
        }
    }

    private fun observeMusicState() {
        audioPlayer.observeState()
            .onEach { musicState -> setState { copy(musicState = musicState) } }
            .catch { /* silently ignore media errors on UI */ }
            .launchIn(screenModelScope)
    }

    // ── Objects ───────────────────────────────────────────────────────────────
    private fun loadRecentObjects() {
        objectRepository.getAllObjects()
            .onEach { objects -> setState { copy(recentObjects = objects.take(3)) } }
            .catch { /* non-critical */ }
            .launchIn(screenModelScope)
    }

    // ── Error handler ─────────────────────────────────────────────────────────
    override fun handleError(throwable: Throwable) {
        setState { copy(isLoading = false, errorMessage = throwable.message) }
    }
}

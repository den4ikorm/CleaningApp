package com.cleaningos.core.mvi

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * BaseViewModel — Placebooker Manifest Core
 *
 * MVI Contract:
 *   State  → StateFlow  → single source of truth, always available
 *   Event  → one-shot user actions dispatched from UI
 *   Effect → SharedFlow → one-time side-effects (navigation, snackbars, etc.)
 *
 * "Thin Screen" protocol: Composable ONLY reads State and dispatches Events.
 * All logic, transformations and effects live exclusively in the ViewModel.
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ScreenModel {

    // ── State ─────────────────────────────────────────────────────────────────
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S get() = _state.value

    // ── Effects ───────────────────────────────────────────────────────────────
    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect: Flow<F> = _effect.receiveAsFlow()

    // ── Event Entry Point ─────────────────────────────────────────────────────
    fun dispatch(event: E) {
        screenModelScope.launch { handleEvent(event) }
    }

    // ── Protected API ─────────────────────────────────────────────────────────
    protected abstract suspend fun handleEvent(event: E)

    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun setEffect(effect: F) {
        screenModelScope.launch { _effect.send(effect) }
    }

    protected fun launchSafe(
        onError: (Throwable) -> Unit = { e -> handleError(e) },
        block: suspend () -> Unit
    ) {
        screenModelScope.launch {
            runCatching { block() }.onFailure(onError)
        }
    }

    protected open fun handleError(throwable: Throwable) {
        // Override in subclasses for global error handling
    }
}

/** Marker interfaces for MVI contract types */
interface UiState
interface UiEvent
interface UiEffect

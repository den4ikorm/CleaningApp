package com.cleaningos.presentation.features.dashboard

import com.cleaningos.core.mvi.UiEffect
import com.cleaningos.core.mvi.UiEvent
import com.cleaningos.core.mvi.UiState
import com.cleaningos.domain.model.*

// ═══════════════════════════════════════════════════════════════════════════
//  Dashboard MVI Contract — "Thin Screen" protocol
//  Screen reads State + dispatches Events. Zero business logic in Composable.
// ═══════════════════════════════════════════════════════════════════════════

/** Complete UI state for DashboardScreen. All fields are immutable. */
data class DashboardState(
    val isLoading: Boolean = false,
    val query: String = "",
    val edi: EdiScore = EdiScore(),
    val searchResult: SearchResult? = null,
    val checklist: Checklist? = null,
    val musicState: MusicState = MusicState(),
    val voiceState: VoiceState = VoiceState(),
    val recentObjects: List<CleaningObject> = emptyList(),
    val errorMessage: String? = null,
    // Quick-action presets
    val quickActions: List<QuickAction> = QuickAction.defaults()
) : UiState

data class QuickAction(
    val id: String,
    val label: String,
    val style: ActionStyle,
    val query: String
) {
    enum class ActionStyle { Danger, Purple, Primary, Warning }

    companion object {
        fun defaults() = listOf(
            QuickAction("emergency",   "!! Экстренно",   ActionStyle.Danger,  "кухня, клиент уже едет, устала, срочно"),
            QuickAction("general",     "* Генеральная",  ActionStyle.Purple,  "генеральная уборка, всё, энергии полно"),
            QuickAction("standard",    "> Стандарт",     ActionStyle.Primary, "квартира, стандартная уборка, нормально"),
            QuickAction("post_repair", "~ Послеремонт",  ActionStyle.Warning, "послеремонтная уборка, цемент, строители"),
        )
    }
}

/** User-dispatched events — all input from the UI layer */
sealed interface DashboardEvent : UiEvent {
    data class QueryChanged(val text: String) : DashboardEvent
    data object SearchClicked : DashboardEvent
    data object VoiceClicked : DashboardEvent
    data class QuickActionClicked(val action: QuickAction) : DashboardEvent
    data class OpenChecklist(val checklist: Checklist) : DashboardEvent
    data object DismissError : DashboardEvent

    // Music
    data object PlayPauseClicked : DashboardEvent
    data object NextTrackClicked : DashboardEvent
    data object PrevTrackClicked : DashboardEvent

    // Voice activation
    data object VoiceActivated : DashboardEvent
    data class VoiceResult(val transcript: String) : DashboardEvent
}

/** One-time effects emitted to the UI — navigation, snackbars, dialogs */
sealed interface DashboardEffect : UiEffect {
    data class NavigateTo(val route: String) : DashboardEffect
    data class OpenChecklistScreen(val checklist: Checklist) : DashboardEffect
    data class ShowSnackbar(val message: String) : DashboardEffect
    data object ShowVoiceDialog : DashboardEffect
    data class RequestPermission(val permission: String) : DashboardEffect
}

package com.cleaningos.presentation.features.checklist

import com.cleaningos.core.mvi.UiEffect
import com.cleaningos.core.mvi.UiEvent
import com.cleaningos.core.mvi.UiState
import com.cleaningos.domain.model.Checklist
import com.cleaningos.domain.model.ChecklistStep

data class ChecklistState(
    val checklist: Checklist? = null,
    val steps: List<ChecklistStep> = emptyList(),
    val completedCount: Int = 0,
    val progressPercent: Float = 0f,
    val isFinished: Boolean = false
) : UiState

sealed interface ChecklistEvent : UiEvent {
    data class Load(val checklist: Checklist) : ChecklistEvent
    data class ToggleStep(val index: Int) : ChecklistEvent
    data object CloseObject : ChecklistEvent
    data object TakePhoto : ChecklistEvent
    data object GoBack : ChecklistEvent
}

sealed interface ChecklistEffect : UiEffect {
    data object NavigateBack : ChecklistEffect
    data object NavigateToDashboard : ChecklistEffect
    data class ShowCompletion(val done: Int, val total: Int) : ChecklistEffect
    data class RequestCameraPermission(val permission: String) : ChecklistEffect
}

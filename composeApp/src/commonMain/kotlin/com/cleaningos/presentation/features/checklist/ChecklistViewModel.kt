package com.cleaningos.presentation.features.checklist

import com.cleaningos.core.mvi.BaseViewModel
import com.cleaningos.domain.model.Checklist

class ChecklistViewModel : BaseViewModel<ChecklistState, ChecklistEvent, ChecklistEffect>(
    initialState = ChecklistState()
) {
    override suspend fun handleEvent(event: ChecklistEvent) {
        when (event) {
            is ChecklistEvent.Load -> {
                val steps = event.checklist.steps.map { it.copy(isDone = false) }
                setState { copy(checklist = event.checklist, steps = steps) }
                recalcProgress()
            }
            is ChecklistEvent.ToggleStep -> {
                val updated = currentState.steps.toMutableList().also {
                    val step = it[event.index]
                    it[event.index] = step.copy(isDone = !step.isDone)
                }
                setState { copy(steps = updated) }
                recalcProgress()
            }
            is ChecklistEvent.CloseObject -> {
                setEffect(ChecklistEffect.ShowCompletion(
                    done  = currentState.completedCount,
                    total = currentState.steps.size
                ))
            }
            is ChecklistEvent.TakePhoto -> {
                setEffect(ChecklistEffect.RequestCameraPermission(
                    android.Manifest.permission.CAMERA
                ))
            }
            is ChecklistEvent.GoBack -> setEffect(ChecklistEffect.NavigateBack)
        }
    }

    private fun recalcProgress() {
        val done  = currentState.steps.count { it.isDone }
        val total = currentState.steps.size
        val pct   = if (total > 0) done / total.toFloat() else 0f
        setState { copy(completedCount = done, progressPercent = pct, isFinished = done == total && total > 0) }
    }
}

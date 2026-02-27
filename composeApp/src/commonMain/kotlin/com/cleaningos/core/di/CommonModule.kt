package com.cleaningos.core.di

import com.cleaningos.presentation.features.checklist.ChecklistViewModel
import com.cleaningos.presentation.features.dashboard.DashboardViewModel
import com.cleaningos.presentation.features.dashboard.MusicViewModel
import org.koin.dsl.module

val commonModule = module {
    // ViewModels
    factory { DashboardViewModel(get(), get(), get(), get()) }
    factory { ChecklistViewModel() }
    factory { MusicViewModel(get(), get()) }
}

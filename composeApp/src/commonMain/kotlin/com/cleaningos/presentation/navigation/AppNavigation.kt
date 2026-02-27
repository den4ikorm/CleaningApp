package com.cleaningos.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.*
import com.cleaningos.presentation.features.dashboard.DashboardScreen
import com.cleaningos.presentation.theme.*

sealed class AppTab(
    override val key: String,
    val label: String,
    val icon: ImageVector
) : Tab {
    object Search   : AppTab("search",   "Поиск",   Icons.Rounded.Search)
    object Map      : AppTab("map",      "Карта",   Icons.Rounded.Map)
    object KnBase   : AppTab("kb",       "Базы",    Icons.Rounded.LibraryBooks)
    object Plans    : AppTab("plans",    "Планы",   Icons.Rounded.ChecklistRtl)
    object Safety   : AppTab("safety",   "Безопас.", Icons.Rounded.Shield)

    @Composable
    override fun Content() {
        when (this) {
            is Search  -> DashboardScreen().Content()
            else       -> PlaceholderTab(label)
        }
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = label)
}

@Composable
fun AppNavigation() {
    TabNavigator(tab = AppTab.Search) { navigator ->
        Scaffold(
            containerColor = OceanDeep,
            bottomBar = {
                NavigationBar(
                    containerColor = FrostedGlass,
                    contentColor   = CyanMint,
                    tonalElevation = 0.dp
                ) {
                    listOf(AppTab.Search, AppTab.Map, AppTab.KnBase, AppTab.Plans, AppTab.Safety)
                        .forEach { tab ->
                            val isSelected = navigator.current.key == tab.key
                            NavigationBarItem(
                                selected  = isSelected,
                                onClick   = { navigator.current = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) CyanMint else TextDisabled
                                    )
                                },
                                label = {
                                    Text(
                                        text  = tab.label,
                                        color = if (isSelected) CyanMint else TextDisabled
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = GlowCyan20
                                )
                            )
                        }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                CurrentTab()
            }
        }
    }
}

@Composable
private fun PlaceholderTab(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanDeep),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("$name — Coming soon", color = TextSecondary)
    }
}

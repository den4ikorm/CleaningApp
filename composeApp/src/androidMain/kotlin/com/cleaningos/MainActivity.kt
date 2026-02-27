package com.cleaningos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cleaningos.core.platform.AppContextHolder
import com.cleaningos.presentation.navigation.AppNavigation
import com.cleaningos.presentation.theme.CleaningOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleaningOSTheme {
                AppNavigation()
            }
        }
    }
}

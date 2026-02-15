package com.example.arthguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.arthguard.core.navigation.AppNavigation
import com.example.arthguard.ui.theme.ArthGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArthGuardTheme {
                AppNavigation()
            }
        }
    }
}

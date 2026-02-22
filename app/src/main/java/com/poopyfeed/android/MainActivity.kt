package com.poopyfeed.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.poopyfeed.android.ui.screens.HomeScreen
import com.poopyfeed.android.ui.theme.PoopyFeedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopyFeedTheme {
                HomeScreen()
            }
        }
    }
}

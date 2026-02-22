package com.poopyfeed.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.poopyfeed.android.navigation.PoopyFeedNavHost
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PoopyFeedTheme {
                PoopyFeedNavHost()
            }
        }
    }
}

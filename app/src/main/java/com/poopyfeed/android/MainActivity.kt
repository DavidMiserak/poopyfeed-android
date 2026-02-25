package com.poopyfeed.android

import android.content.Intent
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
        val inviteToken =
            intent?.data?.pathSegments?.let { segments ->
                if (segments.size >= 2 && segments[0] == "accept") segments[1] else null
            }
        setContent {
            PoopyFeedTheme {
                PoopyFeedNavHost(initialInviteToken = inviteToken)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

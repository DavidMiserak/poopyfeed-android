package com.poopyfeed.android.ui.screens.greeting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.BrandLogo
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose200
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.Rose50
import com.poopyfeed.android.ui.theme.Slate600

@Composable
fun GreetingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: GreetingViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState == AuthCheckState.AUTHENTICATED) {
            onNavigateToHome()
        }
    }

    when (authState) {
        AuthCheckState.LOADING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Rose400)
            }
        }
        AuthCheckState.AUTHENTICATED -> {
            // Will navigate via LaunchedEffect
        }
        AuthCheckState.UNAUTHENTICATED -> {
            GreetingContent(
                onGetStarted = onNavigateToSignup,
                onLogIn = onNavigateToLogin,
            )
        }
    }
}

@Composable
private fun GreetingContent(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Rose50, Amber50),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogo()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Track your baby's feedings,\ndiapers, and naps with ease",
                style = MaterialTheme.typography.bodyLarge,
                color = Slate600,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            GradientButton(
                text = "Get Started",
                onClick = onGetStarted,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Rose200),
            ) {
                Text(
                    text = "Log In",
                    color = Rose400,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingContentPreview() {
    PoopyFeedTheme {
        GreetingContent(
            onGetStarted = {},
            onLogIn = {},
        )
    }
}

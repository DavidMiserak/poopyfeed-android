package com.poopyfeed.android.ui.screens.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@Composable
fun AcceptInviteScreen(
    token: String,
    onAcceptSuccess: () -> Unit,
    onNavigateToChildren: () -> Unit,
    viewModel: AcceptInviteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(token) {
        viewModel.setToken(token)
    }
    LaunchedEffect(uiState.success) {
        if (uiState.success) onAcceptSuccess()
    }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(Rose50, Amber50)))
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error!!)
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = "Accept invite",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(
                text = "Accept",
                onClick = viewModel::accept,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.TextButton(
                onClick = onNavigateToChildren,
            ) {
                Text("Cancel")
            }
        }
    }
}

package com.poopyfeed.android.ui.screens.naps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NapDeleteScreen(
    onNavigateBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    viewModel: NapDeleteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onDeleteSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete nap") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(Rose50, Amber50)))
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error!!)
            }
            Text("Are you sure you want to delete this nap?")
            androidx.compose.material3.Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
            androidx.compose.material3.OutlinedButton(
                onClick = viewModel::delete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Text("Delete")
                }
            }
        }
    }
}

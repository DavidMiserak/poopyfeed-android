package com.poopyfeed.android.ui.screens.diapers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaperFormScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: DiaperFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditMode) "Edit diaper" else "Add diaper change",
                    )
                },
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
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.apiError != null) {
                ErrorBanner(message = uiState.apiError!!)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "wet" to "Wet",
                    "dirty" to "Dirty",
                    "both" to "Both",
                ).forEach { (value, label) ->
                    val selected = uiState.changeType == value
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { viewModel.onChangeTypeChange(value) },
                        label = { Text(label) },
                    )
                }
            }
            OutlinedTextField(
                value = uiState.changedAt,
                onValueChange = viewModel::onChangedAtChange,
                label = { Text("Date & time (ISO UTC)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            GradientButton(
                text = if (viewModel.isEditMode) "Save" else "Add",
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

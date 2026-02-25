package com.poopyfeed.android.ui.screens.feedings

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
fun FeedingFormScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: FeedingFormViewModel = hiltViewModel(),
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
                        if (viewModel.isEditMode) "Edit feeding" else "Add feeding",
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
                listOf("bottle" to "Bottle", "breast" to "Breast").forEach { (value, label) ->
                    val selected = uiState.feedingType == value
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { viewModel.onFeedingTypeChange(value) },
                        label = { Text(label) },
                    )
                }
            }
            OutlinedTextField(
                value = uiState.fedAt,
                onValueChange = viewModel::onFedAtChange,
                label = { Text("Date & time (ISO UTC)") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (uiState.feedingType == "bottle") {
                OutlinedTextField(
                    value = uiState.amountOz,
                    onValueChange = viewModel::onAmountOzChange,
                    label = { Text("Amount (oz)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                )
            } else {
                OutlinedTextField(
                    value = uiState.durationMinutes,
                    onValueChange = viewModel::onDurationMinutesChange,
                    label = { Text("Duration (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.durationError != null,
                    supportingText = uiState.durationError?.let { { Text(it) } },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "left" to "Left",
                        "right" to "Right",
                        "both" to "Both",
                    ).forEach { (value, label) ->
                        val selected = uiState.side == value
                        androidx.compose.material3.FilterChip(
                            selected = selected,
                            onClick = { viewModel.onSideChange(value) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            GradientButton(
                text = if (viewModel.isEditMode) "Save" else "Add feeding",
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

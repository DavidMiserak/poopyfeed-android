package com.poopyfeed.android.ui.screens.children

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
fun ChildFormScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ChildFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("Saved")
            onSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditMode) "Edit child" else "Add child",
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
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.apiError != null) {
                ErrorBanner(message = uiState.apiError!!)
            }
            if (uiState.isLoading && !viewModel.isEditMode) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                )
                OutlinedTextField(
                    value = uiState.dateOfBirth,
                    onValueChange = viewModel::onDateOfBirthChange,
                    label = { Text("Date of birth (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.dateOfBirthError != null,
                    supportingText = uiState.dateOfBirthError?.let { { Text(it) } },
                )
                GenderSelector(
                    selected = uiState.gender,
                    onSelect = viewModel::onGenderChange,
                )
                Spacer(modifier = Modifier.height(8.dp))
                GradientButton(
                    text = if (viewModel.isEditMode) "Save" else "Add child",
                    onClick = viewModel::submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                )
                if (uiState.isLoading && viewModel.isEditMode) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun GenderSelector(
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Gender",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "M" to "Male",
                "F" to "Female",
                "O" to "Other",
            ).forEach { (value, label) ->
                val isSelected = selected == value
                androidx.compose.material3.FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(if (isSelected) null else value) },
                    label = { Text(label) },
                )
            }
        }
    }
}

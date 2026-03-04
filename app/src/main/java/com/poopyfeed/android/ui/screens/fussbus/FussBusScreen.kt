package com.poopyfeed.android.ui.screens.fussbus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FussBusScreen(
    onNavigateBack: () -> Unit,
    viewModel: FussBusViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuss Bus") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to dashboard")
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(Rose50, Amber50)))
                    .padding(20.dp),
        ) {
            FussBusContent(viewModel = viewModel, onNavigateBack = onNavigateBack)
        }
    }
}

@Composable
private fun FussBusContent(
    viewModel: FussBusViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        is FussBusUiState.Loading -> {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.padding(24.dp),
            )
        }
        is FussBusUiState.Step1Symptom -> {
            FussBusStep1Content(
                childAgeMonths = state.childAgeMonths,
                onSymptomSelected = viewModel::onSymptomSelected,
            )
        }
        is FussBusUiState.Step2Checklist -> {
            FussBusStep2Content(
                checklist = state.checklist,
                progress = state.checkedCount to state.totalCount,
                onToggleItem = viewModel::toggleChecklistItem,
                onNext = viewModel::goToStep3,
                onBack = viewModel::goToStep1,
            )
        }
        is FussBusUiState.Step3Suggestions -> {
            FussBusStep3Content(
                suggestions = state.suggestions,
                onDone = onNavigateBack,
                onBack = viewModel::goToStep2,
            )
        }
        is FussBusUiState.Error -> {
            androidx.compose.material3.Text(
                text = state.message,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun FussBusStep1Content(
    childAgeMonths: Float,
    onSymptomSelected: (FussBusSymptom) -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.material3.Text("What's going on?", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        listOf(
            FussBusSymptom.Crying to "Crying",
            FussBusSymptom.WonTSleep to "Won't sleep",
            FussBusSymptom.GeneralFussiness to "General fussiness",
        ).forEach { (symptom, label) ->
            androidx.compose.material3.FilledTonalButton(
                onClick = { onSymptomSelected(symptom) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Text(label)
            }
        }
        if (childAgeMonths >= 12f) {
            androidx.compose.material3.FilledTonalButton(
                onClick = { onSymptomSelected(FussBusSymptom.RefusingFood) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Text("Refusing food")
            }
        }
    }
}

@Composable
private fun FussBusStep2Content(
    checklist: List<FussBusChecklistItem>,
    progress: Pair<Int, Int>,
    onToggleItem: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.Text(
            "Checklist: ${progress.first}/${progress.second}",
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        )
        for (item in checklist) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                if (item.isAutoChecked) {
                    androidx.compose.material3.Text("✓", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.material3.Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { _ -> onToggleItem(item.id) },
                    )
                }
                androidx.compose.material3.Text(item.label + (item.detail?.let { " — $it" } ?: ""))
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onNext) { androidx.compose.material3.Text("Next") }
        androidx.compose.material3.TextButton(onClick = onBack) { androidx.compose.material3.Text("Back") }
    }
}

@Composable
private fun FussBusStep3Content(
    suggestions: List<String>,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.Text("Suggestions", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        suggestions.forEach { androidx.compose.material3.Text(it) }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onDone) { androidx.compose.material3.Text("Done") }
        androidx.compose.material3.TextButton(onClick = onBack) { androidx.compose.material3.Text("Back") }
    }
}

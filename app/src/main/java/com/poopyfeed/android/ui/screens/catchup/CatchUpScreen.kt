package com.poopyfeed.android.ui.screens.catchup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.data.repository.CatchUpBatchEvent
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.screens.children.ChildDisplayUtils
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchUpScreen(
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit,
    viewModel: CatchUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch-up") },
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Brush.verticalGradient(listOf(Rose50, Amber50))),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
            ) {
                if (uiState.error != null) {
                    ErrorBanner(message = uiState.error!!)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "Log multiple events at once, then submit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.addFeeding() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("🍼 Feeding")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.addDiaper() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("💩 Diaper")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.addNap() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("😴 Nap")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.events.isEmpty()) {
                    Text(
                        text = "No events yet. Tap a button above to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            uiState.events,
                            key = { index, _ -> index },
                        ) { index, event ->
                            CatchUpEventRow(
                                event = event,
                                onDelete = { viewModel.removeAt(index) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = { viewModel.submit(onSubmitSuccess) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.events.isNotEmpty(),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(if (uiState.isLoading) "Saving…" else "Submit ${uiState.events.size} event(s)")
                }
            }
        }
    }
}

@Composable
private fun CatchUpEventRow(
    event: CatchUpBatchEvent,
    onDelete: () -> Unit,
) {
    val (emoji, label, time) =
        when (event) {
            is CatchUpBatchEvent.Feeding ->
                Triple("🍼", "Feeding ${event.amountOz?.toInt()?.toString() ?: "?"} oz", event.fedAt)
            is CatchUpBatchEvent.Diaper ->
                Triple("💩", "Diaper (${event.changeType})", event.changedAt)
            is CatchUpBatchEvent.Nap ->
                Triple("😴", "Nap", event.nappedAt)
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = ChildDisplayUtils.formatTimestamp(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

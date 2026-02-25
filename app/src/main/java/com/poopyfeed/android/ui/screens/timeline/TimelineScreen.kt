package com.poopyfeed.android.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.poopyfeed.android.ui.screens.children.ChildDisplayUtils
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.childId) {
        viewModel.loadTimeline()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") },
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
                .statusBarsPadding(),
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
                }
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.items.isEmpty() -> {
                        Text(
                            text = "No activity yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                uiState.items,
                                key = { item ->
                                    when (item) {
                                        is TimelineItem.FeedingItem -> "f${item.feeding.id}"
                                        is TimelineItem.DiaperItem -> "d${item.diaper.id}"
                                        is TimelineItem.NapItem -> "n${item.nap.id}"
                                    }
                                },
                            ) { item ->
                                TimelineItemRow(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItemRow(item: TimelineItem) {
    val (emoji, label, time) =
        when (item) {
            is TimelineItem.FeedingItem -> {
                val f = item.feeding
                val detail =
                    when (f.feedingType) {
                        "bottle" -> "${f.amountOz ?: "?"} oz"
                        "breast" -> "${f.durationMinutes ?: "?"} min ${f.side ?: ""}"
                        else -> ""
                    }
                Triple("🍼", "Feeding $detail", item.timestamp)
            }
            is TimelineItem.DiaperItem ->
                Triple("💩", "Diaper (${item.diaper.changeType})", item.timestamp)
            is TimelineItem.NapItem -> {
                val dur = item.nap.durationMinutes?.toInt()?.let { "$it min" } ?: "—"
                Triple("😴", "Nap $dur", item.timestamp)
            }
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
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
        }
    }
}

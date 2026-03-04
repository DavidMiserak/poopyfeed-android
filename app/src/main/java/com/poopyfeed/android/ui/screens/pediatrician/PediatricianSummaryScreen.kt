package com.poopyfeed.android.ui.screens.pediatrician

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.data.remote.dto.WeeklySummaryResponse
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.screens.children.ChildDisplayUtils
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

private val CardShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PediatricianSummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: PediatricianSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pediatrician summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.child != null && uiState.weeklySummary != null) {
                        IconButton(
                            onClick = {
                                val summary = buildShareText(uiState.child!!.name, uiState.weeklySummary!!)
                                context.startActivity(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, summary)
                                        putExtra(Intent.EXTRA_SUBJECT, "Summary for ${uiState.child!!.name}")
                                    },
                                )
                            },
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share summary")
                        }
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
                    .background(Brush.verticalGradient(listOf(Rose50, Amber50))),
        ) {
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
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ErrorBanner(message = uiState.error!!)
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedButton(onClick = { viewModel.loadSummary() }) {
                            Text("Try again")
                        }
                    }
                }
                uiState.child != null && uiState.weeklySummary != null -> {
                    PediatricianSummaryContent(
                        childName = uiState.child!!.name,
                        summary = uiState.weeklySummary!!,
                        onShare = {
                            val summary = buildShareText(uiState.child!!.name, uiState.weeklySummary!!)
                            context.startActivity(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                    putExtra(Intent.EXTRA_SUBJECT, "Summary for ${uiState.child!!.name}")
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PediatricianSummaryContent(
    childName: String,
    summary: WeeklySummaryResponse,
    onShare: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = childName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = summary.period,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SummaryCard(title = "Feedings") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Count: ${summary.feedings.count}")
                Text("Total oz: ${summary.feedings.totalOz}")
            }
            Text("Bottle: ${summary.feedings.bottle}, Breast: ${summary.feedings.breast}")
        }
        SummaryCard(title = "Diapers") {
            Text("Count: ${summary.diapers.count}")
            Text("Wet: ${summary.diapers.wet}, Dirty: ${summary.diapers.dirty}, Both: ${summary.diapers.both}")
        }
        SummaryCard(title = "Sleep") {
            Text("Naps: ${summary.sleep.naps}")
            Text("Total: ${ChildDisplayUtils.formatMinutes(summary.sleep.totalMinutes)}, Avg: ${summary.sleep.avgDuration} min")
        }
        Spacer(modifier = Modifier.height(8.dp))
        GradientButton(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth(),
            text = "Share summary",
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

private fun buildShareText(childName: String, summary: WeeklySummaryResponse): String {
    return buildString {
        appendLine("Summary for $childName")
        appendLine(summary.period)
        appendLine()
        appendLine("Feedings: ${summary.feedings.count} (${summary.feedings.totalOz} oz total, bottle: ${summary.feedings.bottle}, breast: ${summary.feedings.breast})")
        appendLine("Diapers: ${summary.diapers.count} (wet: ${summary.diapers.wet}, dirty: ${summary.diapers.dirty}, both: ${summary.diapers.both})")
        appendLine("Sleep: ${summary.sleep.naps} naps, ${summary.sleep.totalMinutes} min total, avg ${summary.sleep.avgDuration} min")
    }
}

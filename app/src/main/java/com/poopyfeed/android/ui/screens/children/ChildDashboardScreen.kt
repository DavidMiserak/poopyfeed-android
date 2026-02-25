package com.poopyfeed.android.ui.screens.children

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToEditChild: (() -> Unit)? = null,
    onNavigateToDeleteChild: (() -> Unit)? = null,
    onNavigateToAddFeeding: () -> Unit = {},
    onNavigateToAddDiaper: () -> Unit = {},
    onNavigateToAddNap: () -> Unit = {},
    onNavigateToFeedingsList: () -> Unit = {},
    onNavigateToDiapersList: () -> Unit = {},
    onNavigateToNapsList: () -> Unit = {},
    onNavigateToSharing: () -> Unit = {},
    onNavigateToExport: () -> Unit = {},
    viewModel: ChildDashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by androidx.compose.runtime.mutableStateOf(false)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.child?.name ?: "Dashboard",
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.child != null && (onNavigateToEditChild != null || onNavigateToDeleteChild != null)) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                if (uiState.child?.canEdit == true && onNavigateToEditChild != null) {
                                    DropdownMenuItem(
                                        text = { androidx.compose.material3.Text("Edit child") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToEditChild()
                                        },
                                    )
                                }
                                if (onNavigateToDeleteChild != null) {
                                    DropdownMenuItem(
                                        text = { androidx.compose.material3.Text("Delete child") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToDeleteChild()
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    },
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Rose50, Amber50),
                            ),
                        )
                        .verticalScroll(rememberScrollState()),
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
                            ErrorBanner(message = uiState.error ?: "Unknown error")
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.loadDashboard() },
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                    uiState.child != null -> {
                        ChildDashboardContent(
                            child = uiState.child!!,
                            todaySummary = uiState.todaySummary,
                            onAddFeeding = onNavigateToAddFeeding,
                            onAddDiaper = onNavigateToAddDiaper,
                            onAddNap = onNavigateToAddNap,
                            onFeedingsList = onNavigateToFeedingsList,
                            onDiapersList = onNavigateToDiapersList,
                            onNapsList = onNavigateToNapsList,
                            onSharing = onNavigateToSharing,
                            canManageSharing = uiState.child!!.canManageSharing,
                            onExport = onNavigateToExport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildDashboardContent(
    child: Child,
    todaySummary: TodaySummaryResponse?,
    onAddFeeding: () -> Unit = {},
    onAddDiaper: () -> Unit = {},
    onAddNap: () -> Unit = {},
    onFeedingsList: () -> Unit = {},
    onDiapersList: () -> Unit = {},
    onNapsList: () -> Unit = {},
    onSharing: () -> Unit = {},
    canManageSharing: Boolean = false,
    onExport: () -> Unit = {},
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Child info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ChildDisplayUtils.getGenderEmoji(child.gender),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Column {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = ChildDisplayUtils.getChildAge(child.dateOfBirth),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Last activity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Last activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LastActivityRow("Feeding", "🍼", child.lastFeeding)
                LastActivityRow("Diaper", "🧷", child.lastDiaperChange)
                LastActivityRow("Nap", "😴", child.lastNap)
            }
        }

        // Today's summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today's summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (todaySummary != null &&
                    (
                        todaySummary.feedings.count > 0 ||
                            todaySummary.diapers.count > 0 ||
                            todaySummary.sleep.naps > 0
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SummaryChip("🍼", "Feedings", todaySummary.feedings.count.toString())
                        SummaryChip("💩", "Diapers", todaySummary.diapers.count.toString())
                        SummaryChip(
                            "😴",
                            "Naps",
                            "${todaySummary.sleep.naps} (${ChildDisplayUtils.formatMinutes(todaySummary.sleep.totalMinutes)})",
                        )
                    }
                } else {
                    Text(
                        text = "No activity recorded today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Quick actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Button(
                        onClick = onAddFeeding,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("🍼 Feeding")
                    }
                    androidx.compose.material3.Button(
                        onClick = onAddDiaper,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("💩 Diaper")
                    }
                    androidx.compose.material3.Button(
                        onClick = onAddNap,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("😴 Nap")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.TextButton(
                        onClick = onFeedingsList,
                    ) {
                        Text("View feedings")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = onDiapersList,
                    ) {
                        Text("View diapers")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = onNapsList,
                    ) {
                        Text("View naps")
                    }
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export data")
                }
                if (canManageSharing) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onSharing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Manage sharing")
                    }
                }
            }
        }
    }
}

@Composable
private fun LastActivityRow(
    label: String,
    emoji: String,
    timestamp: String?,
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = ChildDisplayUtils.formatTimestamp(timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryChip(
    emoji: String,
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildDashboardContentPreview() {
    val mockChild =
        Child(
            id = 1,
            name = "Baby Alice",
            dateOfBirth = "2024-01-15",
            gender = "F",
            userRole = "owner",
            canEdit = true,
            canManageSharing = true,
            createdAt = "2024-01-15T10:00:00Z",
            updatedAt = "2024-01-15T10:00:00Z",
            lastDiaperChange = "2024-01-15T14:30:00Z",
            lastNap = "2024-01-15T13:00:00Z",
            lastFeeding = "2024-01-15T12:00:00Z",
        )
    PoopyFeedTheme {
        ChildDashboardContent(
            child = mockChild,
            todaySummary = null,
        )
    }
}

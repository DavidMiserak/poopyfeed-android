package com.poopyfeed.android.ui.screens.children

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.AppShapes
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenListScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChildDashboard: (childId: Int) -> Unit = {},
    onNavigateToAddChild: () -> Unit = {},
    viewModel: ChildrenListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
                            text = "PoopyFeed",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    colors =
                        androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddChild,
                    shape = AppShapes.medium,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Child")
                }
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
                        ),
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null -> {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ErrorBanner(message = uiState.error ?: "Unknown error")
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.loadChildren() },
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                    uiState.children.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Text(
                                    text = "👶",
                                    style = MaterialTheme.typography.displayMedium,
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "No children yet",
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap + to add your first child and start tracking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(uiState.children) { child ->
                                ChildCard(
                                    child = child,
                                    onClick = { onNavigateToChildDashboard(child.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildCard(
    child: Child,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = AppShapes.accentStrip,
                        ),
            )
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ChildDisplayUtils.getGenderEmoji(child.gender),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = ChildDisplayUtils.getChildAge(child.dateOfBirth),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = getRoleLabel(child.userRole),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (child.lastDiaperChange != null ||
                    child.lastFeeding != null ||
                    child.lastNap != null
                ) {
                    Text(
                        text = getRecentActivityEmoji(child),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun getRoleLabel(role: String): String {
    return when (role.lowercase()) {
        "owner" -> "Owner"
        "co-parent" -> "Co-parent"
        "caregiver" -> "Caregiver"
        else -> role
    }
}

private fun getRecentActivityEmoji(child: Child): String {
    // Show most recent activity emoji
    return when {
        child.lastFeeding != null -> "🍼"
        child.lastDiaperChange != null -> "💩"
        child.lastNap != null -> "😴"
        else -> ""
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildrenListScreenPreview() {
    PoopyFeedTheme {
        // Preview without ViewModel
    }
}

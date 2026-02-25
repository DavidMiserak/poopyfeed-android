package com.poopyfeed.android.ui.screens.sharing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.data.remote.dto.Invite
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingScreen(
    onNavigateBack: () -> Unit,
    viewModel: SharingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage sharing") },
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error!!)
            }
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            } else {
                Text(
                    text = "Shared with",
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.shares) { share ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(share.userEmail, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        share.roleDisplay,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.revokeShare(share.id) },
                                ) {
                                    Text("Revoke")
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Invite links",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("CO" to "Co-parent", "CG" to "Caregiver").forEach { (value, label) ->
                        val selected = uiState.createInviteRole == value
                        androidx.compose.material3.FilterChip(
                            selected = selected,
                            onClick = { viewModel.onCreateInviteRoleChange(value) },
                            label = { Text(label) },
                        )
                    }
                }
                androidx.compose.material3.Button(
                    onClick = viewModel::createInvite,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isCreatingInvite,
                ) {
                    if (uiState.isCreatingInvite) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        Text("Create invite link")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.invites) { invite ->
                        InviteCard(
                            invite = invite,
                            onDelete = { viewModel.deleteInvite(invite.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteCard(
    invite: Invite,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = invite.roleDisplay + if (invite.isActive) " (active)" else " (inactive)",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = invite.inviteUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.TextButton(
                onClick = onDelete,
            ) {
                Text("Delete invite")
            }
        }
    }
}

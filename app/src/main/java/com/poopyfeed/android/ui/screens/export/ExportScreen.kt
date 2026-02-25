package com.poopyfeed.android.ui.screens.export

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.Rose50
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    fun openFile(file: File) {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/octet-stream")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export data") },
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
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error!!)
            }
            if (uiState.pdfProgress != null) {
                Text(uiState.pdfProgress!!)
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
            Text("Format")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("csv" to "CSV", "pdf" to "PDF").forEach { (value, label) ->
                    val selected = uiState.format == value
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { viewModel.onFormatChange(value) },
                        label = { Text(label) },
                    )
                }
            }
            Text("Days to include")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(7, 14, 30).forEach { d ->
                    val selected = uiState.days == d
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { viewModel.onDaysChange(d) },
                        label = { Text("$d days") },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            GradientButton(
                text = "Export",
                onClick = { viewModel.export(::openFile, ::openFile) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
        }
    }
}

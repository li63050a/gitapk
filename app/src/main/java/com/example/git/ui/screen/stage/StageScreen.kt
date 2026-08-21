package com.example.git.ui.screen.stage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.git.R
import com.example.git.vm.StageViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageScreen() {
    val viewModel: StageViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var repoPath by remember { mutableStateOf("/sdcard/Download/my-project") }
    var commitMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadStatus(repoPath) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.remote)) }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (commitMessage.isNotEmpty()) viewModel.commit(repoPath, commitMessage) }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            OutlinedTextField(
                value = repoPath,
                onValueChange = { repoPath = it },
                label = { Text("Repository Path") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Button(
                onClick = { viewModel.loadStatus(repoPath) },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) { Text(stringResource(id = R.string.reload)) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = commitMessage,
                onValueChange = { commitMessage = it },
                label = { Text("Commit Message") },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.addAll(repoPath) },
                    modifier = Modifier.weight(1f)
                ) { Text("Stage All") }
                OutlinedButton(
                    onClick = { viewModel.commit(repoPath, commitMessage) },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(id = R.string.confirm)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (uiState.stagedFiles.isNotEmpty()) {
                        item {
                            Text(
                                text = "Staged (${uiState.stagedFiles.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(uiState.stagedFiles) { file ->
                            StagedFileCard(file = file, onUnstage = { viewModel.unstage(repoPath, file) })
                        }
                    }
                    if (uiState.modifiedFiles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Modified (${uiState.modifiedFiles.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        items(uiState.modifiedFiles) { file ->
                            ModifiedFileCard(file = file, onStage = { viewModel.stage(repoPath, file) })
                        }
                    }
                    if (uiState.untrackedFiles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Untracked (${uiState.untrackedFiles.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        items(uiState.untrackedFiles) { file ->
                            UntrackedFileCard(file = file, onStage = { viewModel.stage(repoPath, file) })
                        }
                    }
                    if (uiState.modifiedFiles.isEmpty() && uiState.stagedFiles.isEmpty() && uiState.untrackedFiles.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("No changes") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StagedFileCard(file: String, onUnstage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = file, style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onUnstage) {
                Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ModifiedFileCard(file: String, onStage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "M $file", style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onStage) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@Composable
fun UntrackedFileCard(file: String, onStage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "? $file", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(onClick = onStage) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

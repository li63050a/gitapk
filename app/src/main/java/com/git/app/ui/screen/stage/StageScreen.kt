package com.git.app.ui.screen.stage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.git.app.R
import com.git.app.vm.StageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageScreen(repoPath: String) {
    val viewModel: StageViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var commitMessage by remember { mutableStateOf("") }

    LaunchedEffect(repoPath) {
        viewModel.loadStatus(repoPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.stage)) },
                actions = {
                    IconButton(onClick = { viewModel.loadStatus(repoPath) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (commitMessage.isNotEmpty()) {
                        viewModel.commit(repoPath, commitMessage)
                        commitMessage = ""
                    }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = commitMessage,
                onValueChange = { commitMessage = it },
                label = { Text(stringResource(id = R.string.commit_message)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Button(
                    onClick = { viewModel.addAll(repoPath) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.stage_all))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.loadStatus(repoPath) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.reload))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = context.getString(R.string.error, uiState.error),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { viewModel.loadStatus(repoPath) }) {
                                Text(stringResource(id = R.string.reload))
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.stagedFiles.isNotEmpty()) {
                            item {
                                Text(
                                    text = "${stringResource(id = R.string.staged_files)} (${uiState.stagedFiles.size})",
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${stringResource(id = R.string.modified_files)} (${uiState.modifiedFiles.size})",
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${stringResource(id = R.string.untracked_files)} (${uiState.untrackedFiles.size})",
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
                                ) {
                                    Text(stringResource(id = R.string.no_changes))
                                }
                            }
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
        modifier = Modifier.fillMaxWidth(),
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
        modifier = Modifier.fillMaxWidth(),
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

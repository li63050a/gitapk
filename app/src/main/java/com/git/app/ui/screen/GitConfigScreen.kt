package com.git.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.git.app.R
import com.git.app.vm.GitConfigViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitConfigScreen(
    repoPath: String,
    onBack: () -> Unit,
    onViewLogs: () -> Unit = {}
) {
    val viewModel: GitConfigViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(repoPath) {
        viewModel.loadConfig(repoPath)
    }

    var showRemoteDialog by remember { mutableStateOf(false) }
    var remoteName by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    var editingRemote by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.git_config)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User config section
            item {
                ConfigSection(title = stringResource(id = R.string.user_config)) {
                    OutlinedTextField(
                        value = uiState.userName,
                        onValueChange = { viewModel.setUserName(it) },
                        label = { Text(stringResource(id = R.string.user_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.userEmail,
                        onValueChange = { viewModel.setUserEmail(it) },
                        label = { Text(stringResource(id = R.string.user_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.saveUserConfig(repoPath) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(id = R.string.save))
                    }
                }
            }

            // Remote config section
            item {
                ConfigSection(title = stringResource(id = R.string.remote_config)) {
                    if (uiState.remotes.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.no_remotes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        uiState.remotes.forEach { remote ->
                            RemoteItem(
                                remote = remote,
                                onEdit = {
                                    editingRemote = remote.name
                                    remoteName = remote.name
                                    remoteUrl = remote.url
                                    showRemoteDialog = true
                                },
                                onDelete = { viewModel.deleteRemote(repoPath, remote.name) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            editingRemote = null
                            remoteName = ""
                            remoteUrl = ""
                            showRemoteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(id = R.string.add_remote))
                    }
                }
            }
        }
    }

    if (showRemoteDialog) {
        AlertDialog(
            onDismissRequest = { showRemoteDialog = false },
            title = { Text(if (editingRemote != null) stringResource(id = R.string.edit_remote) else stringResource(id = R.string.add_remote)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = remoteName,
                        onValueChange = { remoteName = it },
                        label = { Text(stringResource(id = R.string.remote_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text(stringResource(id = R.string.remote_url)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (remoteName.isNotEmpty() && remoteUrl.isNotEmpty()) {
                            viewModel.addOrUpdateRemote(repoPath, remoteName, remoteUrl)
                            showRemoteDialog = false
                        }
                    },
                    enabled = remoteName.isNotEmpty() && remoteUrl.isNotEmpty()
                ) {
                    Text(stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ConfigSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun RemoteItem(
    remote: com.git.app.git.Remote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = remote.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = remote.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

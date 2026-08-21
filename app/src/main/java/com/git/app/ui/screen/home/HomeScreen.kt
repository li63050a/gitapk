package com.git.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.git.app.R
import com.git.app.git.RepoInfo
import com.git.app.vm.HomeViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRepoSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRepos("/sdcard/Download")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    if (uiState.repos.isNotEmpty()) {
                        IconButton(onClick = { showBatchDialog = true }) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                        }
                    }
                    IconButton(onClick = { viewModel.loadRepos("/sdcard/Download") }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.repos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = context.getString(R.string.error, uiState.error),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.loadRepos("/sdcard/Download") }) {
                            Text(stringResource(id = R.string.reload))
                        }
                    }
                }
            } else if (uiState.repos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.no_repos),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.no_repos_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.repos) { repo ->
                        RepoCard(repo = repo, onClick = { onRepoSelected(repo.path) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRepoDialog(
            onDismiss = { showAddDialog = false },
            onClone = { url, path ->
                viewModel.cloneRepository(url, path)
                showAddDialog = false
            },
            onInit = { path ->
                viewModel.initRepository(path)
                showAddDialog = false
            }
        )
    }

    if (showBatchDialog) {
        BatchOperationDialog(
            repos = uiState.repos,
            onDismiss = { showBatchDialog = false },
            onPull = { paths -> viewModel.batchPull(paths); showBatchDialog = false },
            onPush = { paths -> viewModel.batchPush(paths); showBatchDialog = false },
            onFetch = { paths -> viewModel.batchFetch(paths); showBatchDialog = false }
        )
    }
}

@Composable
fun RepoCard(repo: RepoInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = repo.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun AddRepoDialog(
    onDismiss: () -> Unit,
    onClone: (String, String) -> Unit,
    onInit: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var url by remember { mutableStateOf("") }
    var localPath by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.add_repo)) },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(id = R.string.clone)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(id = R.string.init)) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(id = R.string.repo_url)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localPath,
                        onValueChange = { localPath = it },
                        label = { Text(stringResource(id = R.string.local_path)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = localPath,
                        onValueChange = { localPath = it },
                        label = { Text(stringResource(id = R.string.dir_path)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0 && url.isNotEmpty()) onClone(url, localPath)
                    else if (selectedTab == 1 && localPath.isNotEmpty()) onInit(localPath)
                },
                enabled = (selectedTab == 0 && url.isNotEmpty()) || (selectedTab == 1 && localPath.isNotEmpty())
            ) {
                Text(stringResource(id = R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun BatchOperationDialog(
    repos: List<RepoInfo>,
    onDismiss: () -> Unit,
    onPull: (List<String>) -> Unit,
    onPush: (List<String>) -> Unit,
    onFetch: (List<String>) -> Unit
) {
    var selectedRepos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var operation by remember { mutableStateOf("pull") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.batch_operation)) },
        text = {
            Column {
                Row {
                    FilterChip(
                        selected = operation == "pull",
                        onClick = { operation = "pull" },
                        label = { Text(stringResource(id = R.string.pull)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = operation == "push",
                        onClick = { operation = "push" },
                        label = { Text(stringResource(id = R.string.push)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = operation == "fetch",
                        onClick = { operation = "fetch" },
                        label = { Text(stringResource(id = R.string.fetch)) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    repos.forEach { repo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    selectedRepos = if (selectedRepos.contains(repo.path))
                                        selectedRepos - repo.path
                                    else
                                        selectedRepos + repo.path
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (selectedRepos.contains(repo.path)) Icons.Default.CheckCircle
                                else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (selectedRepos.contains(repo.path))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = repo.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paths = repos.filter { selectedRepos.contains(it.path) }.map { it.path }
                    when (operation) {
                        "pull" -> onPull(paths)
                        "push" -> onPush(paths)
                        "fetch" -> onFetch(paths)
                    }
                },
                enabled = selectedRepos.isNotEmpty()
            ) {
                Text(stringResource(id = R.string.execute))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

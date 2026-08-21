package com.example.git.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.git.R
import com.example.git.vm.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var scanPath by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRepos("/sdcard/Download")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.loadRepos(scanPath) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    IconButton(onClick = { showBatchDialog = true }) {
                        Icon(Icons.Default.Sync, contentDescription = null)
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(uiState.repos) { repo ->
                        RepoCard(repo = repo, onClick = { onNavigate("repo:${repo.path}") })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRepoDialog(
            onDismiss = { showAddDialog = false },
            onClone = { url, path -> viewModel.cloneRepository(url, path); showAddDialog = false },
            onInit = { path -> viewModel.initRepository(path); showAddDialog = false }
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
fun RepoCard(repo: com.example.git.git.RepoInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = repo.name, style = MaterialTheme.typography.titleMedium)
                Text(text = repo.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddRepoDialog(onDismiss: () -> Unit, onClone: (String, String) -> Unit, onInit: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var url by remember { mutableStateOf("") }
    var localPath by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Repository") }, text = {
        Column {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Clone") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Init") })
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedTab == 0) {
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Repository URL") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = localPath, onValueChange = { localPath = it }, label = { Text("Local Path") }, modifier = Modifier.fillMaxWidth())
            } else {
                OutlinedTextField(value = localPath, onValueChange = { localPath = it }, label = { Text("Directory Path") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }, confirmButton = {
        Button(onClick = { if (selectedTab == 0 && url.isNotEmpty()) onClone(url, localPath) else if (selectedTab == 1 && localPath.isNotEmpty()) onInit(localPath) }) {
            Text("Add")
        }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun BatchOperationDialog(repos: List<com.example.git.git.RepoInfo>, onDismiss: () -> Unit, onPull: (List<String>) -> Unit, onPush: (List<String>) -> Unit, onFetch: (List<String>) -> Unit) {
    var selectedRepos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var operation by remember { mutableStateOf("pull") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Batch Operation") }, text = {
        Column {
            Row {
                FilterChip(selected = operation == "pull", onClick = { operation = "pull" }, label = { Text("Pull") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = operation == "push", onClick = { operation = "push" }, label = { Text("Push") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = operation == "fetch", onClick = { operation = "fetch" }, label = { Text("Fetch") })
            }
            Spacer(modifier = Modifier.height(8.dp))
            repos.forEach { repo ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedRepos = if (selectedRepos.contains(repo.path)) selectedRepos - repo.path else selectedRepos + repo.path
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selectedRepos.contains(repo.path)) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (selectedRepos.contains(repo.path)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = repo.name)
                }
            }
        }
    }, confirmButton = {
        Button(onClick = {
            val paths = repos.filter { selectedRepos.contains(it.path) }.map { it.path }
            when (operation) {
                "pull" -> onPull(paths)
                "push" -> onPush(paths)
                "fetch" -> onFetch(paths)
            }
        }) { Text("Execute") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

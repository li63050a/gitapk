package com.git.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
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
    onNavigateToSettings: () -> Unit,
    onNavigateToConfig: (String) -> Unit
) {
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRepos(Environment.getExternalStorageDirectory().absolutePath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    }
                    if (uiState.repos.isNotEmpty()) {
                        IconButton(onClick = { showBatchDialog = true }) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                        }
                    }
                    IconButton(onClick = { viewModel.loadRepos(Environment.getExternalStorageDirectory().absolutePath) }) {
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
                        Button(onClick = { viewModel.loadRepos(Environment.getExternalStorageDirectory().absolutePath) }) {
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

/**
 * Convert a system file picker (SAF) tree Uri such as
 * content://.../tree/primary:Download/document/primary:Download
 * into a real filesystem path that JGit can use (/storage/emulated/0/Download).
 * Returns null when the path cannot be derived.
 */
fun treeUriToPath(uri: Uri): String? {
    val path = uri.path ?: return null
    if (path.contains("primary:")) {
        val rel = path.substringAfter("primary:")
            .substringBefore("/document")
            .substringBefore("/tree")
            .trimEnd('/')
        return "/storage/emulated/0/$rel"
    }
    return null
}

@Composable
fun AddRepoDialog(
    initialPath: String = "",
    onDismiss: () -> Unit,
    onClone: (String, String) -> Unit,
    onInit: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var url by remember { mutableStateOf("") }
    var localPath by remember { mutableStateOf(initialPath) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            treeUriToPath(it)?.let { path -> localPath = path }
        }
    }

    val appDir = remember {
        context.getExternalFilesDir(null)?.absolutePath
            ?: "/storage/emulated/0/Android/data/${context.packageName}/files"
    }
    val documentsDir = remember {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
    }
    val downloadDir = remember {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }
    fun pickLocation(p: String) {
        java.io.File(p).mkdirs()
        localPath = p
    }

    if (showFolderPicker) {
            FolderPickerDialog(
                initialPath = if (localPath.isNotEmpty()) localPath else Environment.getExternalStorageDirectory().absolutePath,
                onDismiss = { showFolderPicker = false },
            onSelect = { path ->
                localPath = path
                showFolderPicker = false
            }
        )
    }

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
                Text(
                    text = stringResource(id = R.string.quick_locations),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { pickLocation(appDir) },
                        label = { Text(stringResource(id = R.string.app_dir)) }
                    )
                    AssistChip(
                        onClick = { pickLocation(documentsDir) },
                        label = { Text(stringResource(id = R.string.documents)) }
                    )
                    AssistChip(
                        onClick = { pickLocation(downloadDir) },
                        label = { Text(stringResource(id = R.string.download)) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showFolderPicker = true }) {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                }
                                IconButton(onClick = { treeLauncher.launch(null) }) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null)
                                }
                            }
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = localPath,
                        onValueChange = { localPath = it },
                        label = { Text(stringResource(id = R.string.dir_path)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showFolderPicker = true }) {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                }
                                IconButton(onClick = { treeLauncher.launch(null) }) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.init_no_remote_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun FolderPickerDialog(
    initialPath: String = Environment.getExternalStorageDirectory().absolutePath,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var currentPath by remember { mutableStateOf(initialPath) }
    val directories = remember(currentPath) {
        kotlin.runCatching {
            val dir = java.io.File(currentPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { f -> f.isDirectory }
                    ?.map { it.absolutePath }
                    ?.sorted() ?: emptyList()
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_folder)) },
        confirmButton = {
            Button(onClick = { onSelect(currentPath) }) {
                Text(stringResource(id = R.string.select_folder))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        text = {
            Column {
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (currentPath != "/") {
                        item {
                            FolderRow(path = "../", label = stringResource(id = R.string.parent_dir)) {
                                currentPath = java.io.File(currentPath).parent ?: "/"
                            }
                        }
                    }
                    items(directories) { path ->
                        FolderRow(path = path, label = path) {
                            currentPath = path
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun FolderRow(path: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

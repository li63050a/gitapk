package com.git.app.ui.screen.file

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
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
import com.git.app.git.GitExecutor
import com.git.app.vm.FileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(repoPath: String) {
    val viewModel: FileViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(repoPath) {
        viewModel.loadFiles(repoPath)
    }

    if (uiState.selectedFile == null) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(context.getString(R.string.files)) })
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    uiState.error != null -> Text(
                        uiState.error ?: "",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                    uiState.files.isEmpty() -> Text(
                        context.getString(R.string.no_files),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(uiState.files) { path ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            viewModel.openFile(repoPath, path)
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = path, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    } else {
        val selectedFile = uiState.selectedFile
        if (selectedFile != null) {
            EditorScreen(
                repoPath = repoPath,
                relativePath = selectedFile,
                initialContent = uiState.fileContent,
                onBack = { viewModel.closeFile() },
                onSave = { content ->
                    scope.launch {
                        viewModel.saveFile(repoPath, selectedFile, content)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    repoPath: String,
    relativePath: String,
    initialContent: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var savedTip by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(relativePath, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(content)
                        savedTip = true
                    }) {
                        Icon(Icons.Default.Save, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (savedTip) {
                Text(
                    text = stringResource(id = R.string.saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxSize().padding(16.dp),
                label = { Text(stringResource(id = R.string.file_content)) },
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

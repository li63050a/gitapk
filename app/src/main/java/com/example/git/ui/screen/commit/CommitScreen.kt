package com.example.git.ui.screen.commit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.git.R
import com.example.git.vm.CommitViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitScreen() {
    val viewModel: CommitViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var repoPath by remember { mutableStateOf("/sdcard/Download/my-project") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadCommits(repoPath) }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(id = R.string.commits)) })
    }) { padding ->
        val selected = uiState.selectedCommit
        if (selected != null) {
            CommitDetailScreen(commit = selected, onBack = { viewModel.clearDetail() })
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isSearching) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search commits...") }, modifier = Modifier.fillMaxWidth().padding(16.dp), trailingIcon = {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                    })
                } else {
                    OutlinedTextField(value = repoPath, onValueChange = { repoPath = it }, label = { Text("Repository Path") }, modifier = Modifier.fillMaxWidth().padding(16.dp))
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Button(onClick = { viewModel.loadCommits(repoPath) }, modifier = Modifier.weight(1f)) { Text(stringResource(id = R.string.load)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = { isSearching = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Search, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text("Search") }
                    }
                    if (searchQuery.isNotEmpty()) {
                        Button(onClick = { viewModel.searchCommits(repoPath, searchQuery) }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Text("Search: $searchQuery") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (uiState.error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(uiState.commits) { commit ->
                            CommitCard(commit = commit, onClick = { viewModel.loadCommitDetail(repoPath, commit.id) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitDetailScreen(commit: com.example.git.git.CommitDetail, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Commit Detail") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(text = commit.commit.shortId, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${commit.commit.author} · ${commit.commit.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = commit.commit.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            if (commit.files.isNotEmpty()) {
                Text(text = "Files (${commit.files.size})", style = MaterialTheme.typography.titleSmall)
                commit.files.forEach { file ->
                    Text(text = "${file.status}: ${file.path} (+${file.additions}/-${file.deletions})", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (commit.diff.isNotEmpty()) {
                Text(text = "Diff", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = commit.diff, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun CommitCard(commit: com.example.git.git.Commit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = commit.shortId, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = commit.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = commit.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = commit.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

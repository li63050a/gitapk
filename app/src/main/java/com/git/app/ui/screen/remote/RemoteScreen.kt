package com.git.app.ui.screen.remote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.git.app.R
import com.git.app.vm.RemoteViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen() {
    val viewModel: RemoteViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var repoPath by remember { mutableStateOf("/sdcard/Download/my-project") }

    LaunchedEffect(Unit) { viewModel.loadRemotes(repoPath) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.remote)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = repoPath, onValueChange = { repoPath = it }, label = { Text("Repository Path") }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            Button(onClick = { viewModel.loadRemotes(repoPath) }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Text(stringResource(id = R.string.reload)) }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.pull(repoPath) }, enabled = !uiState.isPulling, modifier = Modifier.weight(1f)) {
                    if (uiState.isPulling) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary) else { Icon(Icons.Default.Download, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(id = R.string.pull)) }
                }
                Button(onClick = { viewModel.push(repoPath) }, enabled = !uiState.isPushing, modifier = Modifier.weight(1f)) {
                    if (uiState.isPushing) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary) else { Icon(Icons.Default.Upload, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(id = R.string.push)) }
                }
                Button(onClick = { viewModel.fetch(repoPath) }, enabled = !uiState.isFetching, modifier = Modifier.weight(1f)) {
                    if (uiState.isFetching) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary) else { Icon(Icons.Default.Refresh, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(id = R.string.fetch)) }
                }
            }
            uiState.lastAction?.let { Text(text = it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            uiState.error?.let { Text(text = "Error: $it", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(uiState.remotes) { remote -> RemoteCard(remote = remote) }
                }
            }
        }
    }
}

@Composable
fun RemoteCard(remote: com.git.app.git.Remote) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = remote.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = remote.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

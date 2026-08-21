package com.git.app.ui.screen.ssh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.git.app.R
import com.git.app.vm.SSHViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SSHScreen() {
    val viewModel: SSHViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var keyName by remember { mutableStateOf("") }
    var keyContent by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadKeys() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.ssh_keys)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Key, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.error, uiState.error), color = MaterialTheme.colorScheme.error)
                }
            } else if (uiState.keys.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_ssh_keys))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(uiState.keys) { key ->
                        SSHKeyCard(key = key, onDelete = { viewModel.deleteKey(key.name) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSSHKeyDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, content -> viewModel.addKey(name, content); showAddDialog = false },
            keyName = keyName,
            onKeyNameChange = { keyName = it },
            keyContent = keyContent,
            onKeyContentChange = { keyContent = it }
        )
    }
}

@Composable
fun SSHKeyCard(key: com.git.app.git.SSHKey, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = key.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "${key.type} · ${key.createdTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = key.content.take(100) + if (key.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddSSHKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    keyName: String,
    onKeyNameChange: (String) -> Unit,
    keyContent: String,
    onKeyContentChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.add_ssh_key)) },
        text = {
            Column {
                OutlinedTextField(
                    value = keyName,
                    onValueChange = onKeyNameChange,
                    label = { Text(stringResource(id = R.string.key_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyContent,
                    onValueChange = onKeyContentChange,
                    label = { Text(stringResource(id = R.string.key_content)) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyName.isNotEmpty() && keyContent.isNotEmpty()) onAdd(keyName, keyContent) }
            ) { Text(stringResource(id = R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

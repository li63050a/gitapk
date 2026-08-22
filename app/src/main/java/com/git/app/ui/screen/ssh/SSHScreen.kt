package com.git.app.ui.screen.ssh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
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
import com.git.app.vm.SSHViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SSHScreen(onBack: () -> Unit = {}) {
    val viewModel: SSHViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var keyName by remember { mutableStateOf("") }
    var keyContent by remember { mutableStateOf("") }
    var keyPassphrase by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadKeys()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.ssh_keys)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadKeys() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Key, contentDescription = null)
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = context.getString(R.string.error, uiState.error),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.loadKeys() }) {
                            Text(stringResource(id = R.string.reload))
                        }
                    }
                }
            }
            uiState.keys.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_ssh_keys))
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.keys) { key ->
                        SSHKeyCard(key = key, onDelete = { viewModel.deleteKey(key.name) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        LaunchedEffect(showAddDialog) {
            keyName = ""
            keyContent = ""
            keyPassphrase = ""
        }
        AddSSHKeyDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, content, passphrase ->
                viewModel.addKey(name, content, passphrase)
                showAddDialog = false
            },
            keyName = keyName,
            onKeyNameChange = { keyName = it },
            keyContent = keyContent,
            onKeyContentChange = { keyContent = it },
            keyPassphrase = keyPassphrase,
            onKeyPassphraseChange = { keyPassphrase = it }
        )
    }
}

@Composable
fun SSHKeyCard(key: com.git.app.git.SSHKey, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = key.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${key.type} · ${key.createdTime}" + if (key.hasPassphrase) " · 🔒" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = key.content.lineSequence().firstOrNull()?.take(80) + " (私钥仅显示第一行)",
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
    onAdd: (String, String, String) -> Unit,
    keyName: String,
    onKeyNameChange: (String) -> Unit,
    keyContent: String,
    onKeyContentChange: (String) -> Unit,
    keyPassphrase: String,
    onKeyPassphraseChange: (String) -> Unit
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyPassphrase,
                    onValueChange = onKeyPassphraseChange,
                    label = { Text(stringResource(id = R.string.key_passphrase)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyName.isNotEmpty() && keyContent.isNotEmpty()) onAdd(keyName, keyContent, keyPassphrase) },
                enabled = keyName.isNotEmpty() && keyContent.isNotEmpty()
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

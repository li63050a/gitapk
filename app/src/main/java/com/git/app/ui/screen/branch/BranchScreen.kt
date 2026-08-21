package com.git.app.ui.screen.branch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.git.app.R
import com.git.app.vm.BranchViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchScreen() {
    val viewModel: BranchViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var repoPath by remember { mutableStateOf("/sdcard/Download/my-project") }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadBranches(repoPath) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(id = R.string.branches)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = repoPath, onValueChange = { repoPath = it }, label = { Text("Repository Path") }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            Button(onClick = { viewModel.loadBranches(repoPath) }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Text(stringResource(id = R.string.reload)) }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showCreateDialog = true }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Text(stringResource(id = R.string.create_branch)) }
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(uiState.branches) { branch ->
                        BranchCard(branch = branch, onCheckout = { viewModel.checkoutBranch(repoPath, branch.name) }, onDelete = { viewModel.deleteBranch(repoPath, branch.name) })
                    }
                }
            }
        }
    }
    if (showCreateDialog) {
        CreateBranchDialog(onDismiss = { showCreateDialog = false }, onCreate = { name -> viewModel.createBranch(repoPath, name); showCreateDialog = false })
    }
}

@Composable
fun BranchCard(branch: com.git.app.git.Branch, onCheckout: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = if (branch.isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = branch.name, style = MaterialTheme.typography.titleMedium, color = if (branch.isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                if (branch.isCurrent) Text(text = "Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (!branch.isCurrent) {
                Row {
                    IconButton(onClick = onCheckout) { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun CreateBranchDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create Branch") }, text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Branch Name") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { if (name.isNotEmpty()) onCreate(name) }) { Text("Create") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

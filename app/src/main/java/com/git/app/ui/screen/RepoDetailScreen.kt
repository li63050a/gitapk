package com.git.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.git.app.R
import com.git.app.data.RepoManager
import com.git.app.ui.screen.branch.BranchScreen
import com.git.app.ui.screen.commit.CommitScreen
import com.git.app.ui.screen.remote.RemoteScreen
import com.git.app.ui.screen.settings.SettingsScreen
import com.git.app.ui.screen.ssh.SSHScreen
import com.git.app.ui.screen.stage.StageScreen
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repoPath: String,
    repoName: String,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    repoManager: RepoManager
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabItem(stringResource(id = R.string.commits), Icons.Default.Commit),
        TabItem(stringResource(id = R.string.stage), Icons.Default.Code),
        TabItem(stringResource(id = R.string.branches), Icons.Default.Sync),
        TabItem(stringResource(id = R.string.remote), Icons.Default.Key),
        TabItem(stringResource(id = R.string.settings), Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repoName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = repoPath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        Box(modifier = Modifier
                            .offset(x = tabPositions[selectedTab].left)
                            .width(tabPositions[selectedTab].right - tabPositions[selectedTab].left)
                            .height(3.dp))
                    }
                },
                divider = { Divider(thickness = 1.dp) }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = {
                            Text(
                                tab.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> CommitContent(repoPath = repoPath)
                1 -> StageContent(repoPath = repoPath)
                2 -> BranchContent(repoPath = repoPath)
                3 -> RemoteContent(repoPath = repoPath)
                4 -> SettingsContent()
            }
        }
    }
}

private data class TabItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun CommitContent(repoPath: String) {
    CommitScreen(repoPath = repoPath)
}

@Composable
fun StageContent(repoPath: String) {
    StageScreen(repoPath = repoPath)
}

@Composable
fun BranchContent(repoPath: String) {
    BranchScreen(repoPath = repoPath)
}

@Composable
fun RemoteContent(repoPath: String) {
    RemoteScreen(repoPath = repoPath)
}

@Composable
fun SettingsContent() {
    SettingsScreen()
}

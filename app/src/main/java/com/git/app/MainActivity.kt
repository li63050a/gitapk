package com.git.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.git.app.data.AppLanguage
import com.git.app.data.RepoManager
import com.git.app.ui.navigation.GitNavGraph
import com.git.app.ui.screen.HomeScreen
import com.git.app.ui.theme.GitAppTheme
import com.git.app.vm.HomeViewModel
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLanguage(this, AppLanguage.ZH)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            GitAppTheme {
                val viewModel: HomeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val repoManager = remember { RepoManager() }
                var drawerOpen by remember { mutableStateOf(false) }
                
                LaunchedEffect(uiState.repos) {
                    repoManager.refreshRepos(uiState.repos)
                }
                
                LaunchedEffect(Unit) {
                    viewModel.loadRepos("/sdcard/Download")
                }
                
                // Draw drawer overlay when open
                if (drawerOpen) {
                    DrawerOverlay(
                        repos = uiState.repos,
                        currentRepoPath = repoManager.selectedRepoPath,
                        onRepoSelected = { path ->
                            repoManager.selectRepo(path)
                            navController.navigate("repoDetail:$path") {
                                popUpTo(navController.graph.startDestinationId)
                            }
                            drawerOpen = false
                        },
                        onDismiss = { drawerOpen = false },
                        onNavigateToSettings = { /* navigate to settings */ }
                    )
                }
                
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(id = R.string.app_name)) },
                            navigationIcon = {
                                IconButton(onClick = { drawerOpen = true }) {
                                    Icon(Icons.Default.Menu, contentDescription = null)
                                }
                            }
                        )
                    }
                ) { padding ->
                    when (val route = navController.currentBackStackEntryAsState().value?.destination?.route) {
                        null, "home" -> {
                            HomeScreen(
                                onRepoSelected = { path ->
                                    repoManager.selectRepo(path)
                                    navController.navigate("repoDetail:$path")
                                },
                                onNavigateToSettings = { /* navigate to settings */ }
                            )
                        }
                        else -> {
                            GitNavGraph(
                                navController = navController,
                                repoManager = repoManager
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerOverlay(
    repos: List<com.git.app.git.RepoInfo>,
    currentRepoPath: String?,
    onRepoSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val drawerWidth = (screenWidth * 0.75f).coerceAtMost(320.dp)
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
        )
        
        // Drawer content
        Column(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${repos.size} repos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Repository list
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(repos) { repo ->
                    RepositoryItem(
                        repo = repo,
                        isSelected = repo.path == currentRepoPath,
                        onClick = { onRepoSelected(repo.path) }
                    )
                }
            }
            
            // Bottom actions
            Divider()
            Column(modifier = Modifier.padding(8.dp)) {
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.settings)) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { onNavigateToSettings() }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.log)) },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    selected = false,
                    onClick = { /* navigate to log */ }
                )
                if (repos.isNotEmpty()) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(id = R.string.add_repo)) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        selected = false,
                        onClick = { /* add repo */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryItem(
    repo: com.git.app.git.RepoInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = containerColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = repo.path,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun applyLanguage(context: Context, language: AppLanguage) {
    val tag = language.tag
    val locale = if (tag != null) Locale(tag) else Locale.getDefault()
    Locale.setDefault(locale)
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
}

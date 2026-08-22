package com.git.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.WindowCompat
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.git.app.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.git.app.data.AppLanguage
import com.git.app.data.SettingsRepository
import com.git.app.data.RepoManager
import com.git.app.log.Log
import com.git.app.ui.navigation.GitNavGraph
import com.git.app.ui.screen.HomeScreen
import com.git.app.ui.theme.GitAppTheme
import com.git.app.vm.HomeViewModel
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val tag = newBase.getSharedPreferences("settings_locale", Context.MODE_PRIVATE)
            .getString("tag", null)
        val locale = if (tag.isNullOrEmpty()) Locale.getDefault() else Locale(tag)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val initialSettings = runBlocking { SettingsRepository.getSettings(this@MainActivity).first() }
        Log.configure(initialSettings.logMaxBytes, initialSettings.logMaxFiles)

        setContent {
            val uiSettings by SettingsRepository.getSettings(this@MainActivity).collectAsState(initial = initialSettings)
            GitAppTheme(settings = uiSettings) {
                val viewModel: HomeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val repoManager = remember { RepoManager() }
                var showAllFilesDialog by remember { mutableStateOf(false) }
                var allFilesDenied by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val defaultRoot = Environment.getExternalStorageDirectory().absolutePath
                val scope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                // Request storage permissions
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.values.all { it }
                    if (!allGranted) {
                        // Permission denied - user won't be able to access repositories
                    }
                }

                // Launcher to open system settings for "All files access"
                val allFilesLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (PermissionHelper.hasAllFilesAccess()) {
                        viewModel.loadRepos(defaultRoot)
                    }
                }

                // Re-check permissions every time the app returns to the foreground so the
                // user is prompted again (until they grant, or explicitly dismiss).
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (!PermissionHelper.hasAllFilesAccess() && !allFilesDenied) {
                                showAllFilesDialog = true
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    val required = PermissionHelper.getRequiredPermissions()
                    val needsRequest = required.any { perm ->
                        ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                    }
                    if (needsRequest) {
                        permissionLauncher.launch(required)
                    }
                    // On Android 11+ we must ask the user to grant "All files access"
                    // or directory listing / git operations outside the sandbox will fail.
                    if (!PermissionHelper.hasAllFilesAccess() && !allFilesDenied) {
                        showAllFilesDialog = true
                    }
                }

                LaunchedEffect(uiState.repos) {
                    repoManager.refreshRepos(uiState.repos)
                }

                LaunchedEffect(Unit) {
                    viewModel.loadRepos(defaultRoot)
                }

                if (showAllFilesDialog) {
                    AlertDialog(
                        onDismissRequest = { showAllFilesDialog = false },
                        title = { Text("需要存储权限") },
                        text = {
                            Text(
                                "本应用需要“所有文件访问权限”才能读取和写入您设备上的 Git 仓库目录。" +
                                        "请在接下来的系统设置页面中开启该权限。"
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showAllFilesDialog = false
                                PermissionHelper.launchAllFilesAccess(context, allFilesLauncher)
                            }) {
                                Text("去设置")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAllFilesDialog = false
                                allFilesDenied = true
                            }) {
                                Text("取消")
                            }
                        }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        ModalDrawerSheet {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    scope.launch { drawerState.close() }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                            Divider()
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                items(uiState.repos) { repo ->
                                    RepositoryItem(
                                        repo = repo,
                                        isSelected = repo.path == repoManager.selectedRepoPath
                                    ) {
                                        repoManager.selectRepo(repo.path)
                                        navController.navigate("repoDetail:${java.net.URLEncoder.encode(repo.path, "UTF-8")}") {
                                            popUpTo(navController.graph.startDestinationId)
                                        }
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            }
                            Divider()
                            Column(modifier = Modifier.padding(8.dp)) {
                                NavigationDrawerItem(
                                    label = { Text(stringResource(id = R.string.settings)) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    selected = false,
                                    onClick = {
                                        navController.navigate("settings")
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text(stringResource(id = R.string.log)) },
                                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                                    selected = false,
                                    onClick = {
                                        navController.navigate("log")
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text(stringResource(id = R.string.developer)) },
                                    icon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                                    selected = false,
                                    onClick = {
                                        navController.navigate("about")
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                if (uiState.repos.isNotEmpty()) {
                                    NavigationDrawerItem(
                                        label = { Text(stringResource(id = R.string.add_repo)) },
                                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                        selected = false,
                                        onClick = {
                                            navController.navigate("home") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                            scope.launch { drawerState.close() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(id = R.string.app_name)) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = null)
                                    }
                                }
                            )
                        }
                    ) { padding ->
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

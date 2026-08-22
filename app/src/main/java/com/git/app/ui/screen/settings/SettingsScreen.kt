package com.git.app.ui.screen.settings

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.git.app.PermissionHelper
import com.git.app.R
import com.git.app.data.AccentPreset
import com.git.app.data.AppLanguage
import com.git.app.data.BgPreset
import com.git.app.data.SettingsRepository
import com.git.app.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLog: () -> Unit = {},
    onNavigateToSsh: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by SettingsRepository.getSettings(context).collectAsState(
        initial = com.git.app.data.UiSettings()
    )
    
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var bgPreset by remember { mutableStateOf(settings.bgPreset) }
    var accentPreset by remember { mutableStateOf(settings.accentPreset) }
    var language by remember { mutableStateOf(settings.language) }
    var customBgPath by remember { mutableStateOf(settings.customBgPath) }
    var bgAlphaState by remember { mutableStateOf(settings.bgAlpha) }
    var gitUserName by remember { mutableStateOf(settings.gitUserName) }
    var gitUserEmail by remember { mutableStateOf(settings.gitUserEmail) }
    var logMaxBytesKb by remember { mutableStateOf(0) }
    var logMaxCount by remember { mutableStateOf(0) }
    var initialized by remember { mutableStateOf(false) }

    // Sync local editing state from the persisted store exactly once, so reopening
    // the screen shows the saved values instead of the in-memory defaults.
    LaunchedEffect(Unit) {
        val s = SettingsRepository.getSettings(context).first()
        themeMode = s.themeMode
        bgPreset = s.bgPreset
        accentPreset = s.accentPreset
        language = s.language
        customBgPath = s.customBgPath
        bgAlphaState = s.bgAlpha
        gitUserName = s.gitUserName
        gitUserEmail = s.gitUserEmail
        logMaxBytesKb = if (s.logMaxBytes > 0) (s.logMaxBytes / 1024).toInt() else 0
        logMaxCount = s.logMaxFiles
        initialized = true
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val target = java.io.File(dir, "bg_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(input, null, bounds)
                        val maxDim = 1920
                        val sample = maxOf(1, (maxOf(bounds.outWidth, bounds.outHeight) / maxDim))
                        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                        context.contentResolver.openInputStream(it)?.use { input2 ->
                            val bmp = BitmapFactory.decodeStream(input2, null, opts)
                            target.outputStream().use { out ->
                                bmp?.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                            }
                        }
                    }
                    if (target.exists() && target.length() > 0) {
                        customBgPath = target.absolutePath
                        bgPreset = BgPreset.CUSTOM
                    }
                }
            }
        }
    }

    // Storage permission request (standard) + all-files access (Android 11+)
    var permTick by remember { mutableIntStateOf(0) }
    val storageGranted = remember(permTick) { PermissionHelper.hasStoragePermission(context) }
    val allFilesGranted = remember(permTick) { PermissionHelper.hasAllFilesAccess() }

    val storagePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permTick++ }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permTick++
    }

    LaunchedEffect(themeMode) { if (initialized) SettingsRepository.setThemeMode(context, themeMode) }
    LaunchedEffect(bgPreset) { if (initialized) SettingsRepository.setBgPreset(context, bgPreset) }
    LaunchedEffect(accentPreset) { if (initialized) SettingsRepository.setAccentPreset(context, accentPreset) }
    LaunchedEffect(language) {
        if (initialized) {
            SettingsRepository.setLanguage(context, language)
            val current = context.resources.configuration.locale?.language
            val want = language.tag ?: Locale.getDefault().language
            if (current != want) {
                (context as? ComponentActivity)?.recreate()
            }
        }
    }
    LaunchedEffect(gitUserName, gitUserEmail) {
        if (initialized) SettingsRepository.setGlobalGitUser(context, gitUserName, gitUserEmail)
    }

    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0.1"
        }.getOrElse { "0.0.0.1" }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.settings)) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingSection(title = context.getString(R.string.theme_mode)) {
                    ThemeModeSelector(current = themeMode, onSelect = { themeMode = it })
                }
            }
            
            item {
                SettingSection(title = context.getString(R.string.bg_color)) {
                    BgPresetSelector(current = bgPreset, onSelect = {
                        bgPreset = it
                        if (it != BgPreset.CUSTOM) {
                            customBgPath = null
                        }
                    })
                }
            }
            
            item {
                SettingSection(title = context.getString(R.string.accent_color)) {
                    AccentPresetSelector(current = accentPreset, onSelect = { accentPreset = it })
                }
            }
            
            item {
                SettingSection(title = context.getString(R.string.language)) {
                    LanguageSelector(current = language, onSelect = { language = it })
                }
            }

            item {
                SettingSection(title = context.getString(R.string.global_git_user)) {
                    OutlinedTextField(
                        value = gitUserName,
                        onValueChange = { gitUserName = it },
                        label = { Text(context.getString(R.string.user_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gitUserEmail,
                        onValueChange = { gitUserEmail = it },
                        label = { Text(context.getString(R.string.user_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.global_git_user_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        scope.launch {
                            SettingsRepository.setGlobalGitUser(context, gitUserName, gitUserEmail)
                        }
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.saved),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
            
            item {
                SettingSection(title = context.getString(R.string.background_image)) {
                    CustomBgSelector(
                        currentPath = customBgPath,
                        onSelect = { launcher.launch("image/*") },
                        onClear = {
                            customBgPath = null
                            bgPreset = BgPreset.DEFAULT
                        }
                    )
                    if (customBgPath != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "${context.getString(R.string.bg_opacity)}: ${(bgAlphaState * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = bgAlphaState,
                            onValueChange = { bgAlphaState = it },
                            valueRange = 0f..1f
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch {
                            SettingsRepository.setCustomBg(context, customBgPath)
                            SettingsRepository.setBgPreset(context, bgPreset)
                            SettingsRepository.setBgAlpha(context, bgAlphaState)
                        }
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.saved),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
            
            item {
                SettingSection(title = context.getString(R.string.storage_permission)) {
                    PermissionRow(
                        title = context.getString(R.string.normal_storage_permission),
                        granted = storageGranted,
                        onRequest = { storagePermLauncher.launch(PermissionHelper.getRequiredPermissions()) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PermissionRow(
                        title = context.getString(R.string.all_files_access_permission),
                        granted = allFilesGranted,
                        onRequest = { PermissionHelper.launchAllFilesAccess(context, allFilesLauncher) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.all_files_access_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.ssh_key_management),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToSsh() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = context.getString(R.string.ssh_keys), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.settings_log),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToLog() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = context.getString(R.string.log), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                SettingSection(title = context.getString(R.string.log_settings)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = if (logMaxBytesKb == 0) "" else logMaxBytesKb.toString(),
                            onValueChange = { v ->
                                logMaxBytesKb = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                            },
                            label = { Text(context.getString(R.string.log_max_size)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = if (logMaxCount == 0) "" else logMaxCount.toString(),
                            onValueChange = { v ->
                                logMaxCount = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                            },
                            label = { Text(context.getString(R.string.log_max_count)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = {
                            val bytes = if (logMaxBytesKb > 0) logMaxBytesKb * 1024L else 0L
                            scope.launch {
                                SettingsRepository.setLogLimits(context, bytes, logMaxCount)
                                com.git.app.log.Log.configure(bytes, logMaxCount)
                            }
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.log_settings_saved),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Text(context.getString(R.string.save))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = context.getString(R.string.about),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = context.getString(R.string.version), style = MaterialTheme.typography.bodyMedium)
                                Text(text = versionName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (granted) stringResource(id = R.string.granted) else stringResource(id = R.string.not_granted),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onRequest) {
            Text(if (granted) stringResource(id = R.string.recheck) else stringResource(id = R.string.request_permission))
        }
    }
}

@Composable
fun ThemeModeSelector(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            val label = when (mode) {
                ThemeMode.SYSTEM -> R.string.system_mode
                ThemeMode.LIGHT -> R.string.light_mode
                ThemeMode.DARK -> R.string.dark_mode
            }
            FilterChip(
                selected = current == mode,
                onClick = { onSelect(mode) },
                label = { Text(stringResource(id = label)) }
            )
        }
    }
}

@Composable
fun BgPresetSelector(current: BgPreset, onSelect: (BgPreset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BgPreset.entries.forEach { preset ->
            val isSelected = preset == current
            val bgColor = Color(android.graphics.Color.parseColor(preset.light))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(preset) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = if (isSelected) MaterialTheme.colorScheme.primary else bgColor, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(id = preset.labelRes))
            }
        }
    }
}

@Composable
fun AccentPresetSelector(current: AccentPreset, onSelect: (AccentPreset) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AccentPreset.entries.forEach { preset ->
            val isSelected = preset == current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(preset) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(preset.light)),
                            shape = MaterialTheme.shapes.large
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = preset.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun LanguageSelector(current: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { lang ->
            val labelRes = when (lang) {
                AppLanguage.ZH -> R.string.lang_zh
                AppLanguage.EN -> R.string.lang_en
                else -> R.string.lang_zh
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(lang) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (lang == current) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (lang == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = stringResource(id = labelRes))
            }
        }
    }
}

@Composable
fun CustomBgSelector(currentPath: String?, onSelect: () -> Unit, onClear: () -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSelect) { Text(stringResource(id = R.string.select_image)) }
            if (currentPath != null) {
                OutlinedButton(onClick = onClear) { Text(stringResource(id = R.string.clear_image)) }
            }
        }
        if (currentPath != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val bitmap = remember(currentPath) { try { BitmapFactory.decodeFile(currentPath) } catch (e: Exception) { null } }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(text = currentPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

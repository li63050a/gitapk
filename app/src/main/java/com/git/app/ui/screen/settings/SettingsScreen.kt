package com.git.app.ui.screen.settings

import android.content.Context
import android.graphics.BitmapFactory
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.git.app.R
import com.git.app.data.AccentPreset
import com.git.app.data.AppLanguage
import com.git.app.data.BgPreset
import com.git.app.data.SettingsRepository
import com.git.app.data.ThemeMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings by SettingsRepository.getSettings(context).collectAsState(
        initial = com.git.app.data.UiSettings()
    )
    
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var bgPreset by remember { mutableStateOf(settings.bgPreset) }
    var accentPreset by remember { mutableStateOf(settings.accentPreset) }
    var language by remember { mutableStateOf(settings.language) }
    var customBgPath by remember { mutableStateOf(settings.customBgPath) }

    var pendingBgPath by remember { mutableStateOf<String?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.path?.let { path ->
            customBgPath = path
            pendingBgPath = path
        }
    }

    LaunchedEffect(pendingBgPath) {
        pendingBgPath?.let { SettingsRepository.setCustomBg(context, it) }
        pendingBgPath = null
    }

    LaunchedEffect(themeMode) { SettingsRepository.setThemeMode(context, themeMode) }
    LaunchedEffect(bgPreset) { SettingsRepository.setBgPreset(context, bgPreset) }
    LaunchedEffect(accentPreset) { SettingsRepository.setAccentPreset(context, accentPreset) }
    LaunchedEffect(language) { 
        SettingsRepository.setLanguage(context, language)
        applyLanguage(context, language)
    }
    LaunchedEffect(customBgPath) { SettingsRepository.setCustomBg(context, customBgPath) }

    var versionName by remember { mutableStateOf("0.0.0.1") }

    LaunchedEffect(Unit) {
        try {
            versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0.1"
        } catch (e: Exception) {
            versionName = "0.0.0.1"
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.settings)) }) }
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
                    ThemeModeSelector(
                        current = themeMode,
                        onSelect = { themeMode = it }
                    )
                }
            }
            
            item {
                SettingSection(title = context.getString(R.string.bg_color)) {
                    BgPresetSelector(current = bgPreset, onSelect = { bgPreset = it })
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
                SettingSection(title = context.getString(R.string.background_image)) {
                    CustomBgSelector(
                        currentPath = customBgPath,
                        onSelect = { launcher.launch("image/*") },
                        onClear = { 
                            customBgPath = null
                            pendingBgPath = null
                        }
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
                            modifier = Modifier.fillMaxWidth().clickable { /* navigate to SSH */ },
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

fun applyLanguage(context: Context, language: AppLanguage) {
    val tag = language.tag
    val locale = if (tag != null) Locale(tag) else Locale.getDefault()
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

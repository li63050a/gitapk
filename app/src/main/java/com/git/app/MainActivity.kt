package com.git.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.git.app.data.AppLanguage
import com.git.app.ui.navigation.GitNavGraph
import com.git.app.ui.theme.GitAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 首次启动强制使用中文
        applyLanguage(this, AppLanguage.ZH)
        enableEdgeToEdge()
        setContent {
            GitAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    GitNavGraph(navController = navController)
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

package com.example.git

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
import com.example.git.data.AppLanguage
import com.example.git.ui.navigation.GitNavGraph
import com.example.git.ui.theme.GitAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

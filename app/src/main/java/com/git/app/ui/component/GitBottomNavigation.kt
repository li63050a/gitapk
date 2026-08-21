package com.git.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.git.app.R

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("home", R.string.nav_home, Icons.Default.Folder),
    BottomNavItem("commit", R.string.nav_commits, Icons.Default.Description),
    BottomNavItem("stage", R.string.nav_stage, Icons.Default.Code),
    BottomNavItem("branch", R.string.nav_branch, Icons.Default.Sync),
    BottomNavItem("remote", R.string.nav_remote, Icons.Default.Key),
    BottomNavItem("settings", R.string.nav_settings, Icons.Default.Settings)
)

@Composable
fun GitBottomNavigation(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(id = item.labelRes)) },
                label = { Text(stringResource(id = item.labelRes)) },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

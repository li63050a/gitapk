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

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("home", "Home", Icons.Default.Folder),
    BottomNavItem("commit", "Commits", Icons.Default.Description),
    BottomNavItem("stage", "Stage", Icons.Default.Code),
    BottomNavItem("branch", "Branches", Icons.Default.Sync),
    BottomNavItem("remote", "Remote", Icons.Default.Key),
    BottomNavItem("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun GitBottomNavigation(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) }
            )
        }
    }
}

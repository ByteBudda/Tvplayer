package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val screen: AppViewModel.Screen,
    val testTag: String
)

val navItemsList = listOf(
    NavItem("Эфир ТВ", Icons.Filled.Tv, AppViewModel.Screen.PLAYER, "nav_item_player"),
    NavItem("Плейлисты", Icons.Filled.PlaylistPlay, AppViewModel.Screen.PLAYLISTS, "nav_item_playlists"),
    NavItem("Замочек", Icons.Filled.FamilyRestroom, AppViewModel.Screen.PARENTAL, "nav_item_parental")
)

@Composable
fun AdaptiveBottomBar(
    currentScreen: AppViewModel.Screen,
    onNavigate: (AppViewModel.Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("app_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        navItemsList.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
fun AdaptiveNavigationRail(
    currentScreen: AppViewModel.Screen,
    onNavigate: (AppViewModel.Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.testTag("app_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            // Elegant brand indicator
            Icon(
                imageVector = Icons.Filled.ConnectedTv,
                contentDescription = "Бренд",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    ) {
        navItemsList.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

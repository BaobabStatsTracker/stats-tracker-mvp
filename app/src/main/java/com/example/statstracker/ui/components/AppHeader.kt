package com.example.statstracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    isTablet: Boolean,
    onMenuClick: () -> Unit
) {
    if (isTablet) {
        Surface(shadowElevation = 4.dp) {
        TopAppBar(
            title = {
                Text(
                    text = "EasyBuckets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            actions = {
                IconButton(onClick = { /* TODO: notifications */ }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications"
                    )
                }
                IconButton(onClick = { /* TODO: live broadcast */ }) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = "Live Broadcast"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color.Black,
                actionIconContentColor = Color.Black
            )
        )
        }
    } else {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "EasyBuckets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            actions = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            }
        )
    }
}

data class DrawerMenuItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val drawerMenuItems = listOf(
    DrawerMenuItem("Games", Icons.Default.SportsBasketball, "games"),
    DrawerMenuItem("Players", Icons.Default.Person, "players"),
    DrawerMenuItem("Teams", Icons.Default.Groups, "teams"),
    DrawerMenuItem("Settings", Icons.Default.Settings, "settings")
)

@Composable
fun AppSidebar(
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    PermanentDrawerSheet(
        modifier = Modifier.width(240.dp),
        drawerContainerColor = Color.White
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        drawerMenuItems.forEach { item ->
            NavigationDrawerItem(
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.label)
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "EasyBuckets",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        drawerMenuItems.forEach { item ->
            NavigationDrawerItem(
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.label)
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

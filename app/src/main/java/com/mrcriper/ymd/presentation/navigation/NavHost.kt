package com.mrcriper.ymd.presentation.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mrcriper.ymd.R
import com.mrcriper.ymd.presentation.screens.about.AboutScreen
import com.mrcriper.ymd.presentation.screens.auth.AuthScreen
import com.mrcriper.ymd.presentation.screens.download.DownloadScreen
import com.mrcriper.ymd.presentation.screens.home.HomeScreen
import com.mrcriper.ymd.presentation.screens.library.LibraryScreen
import com.mrcriper.ymd.presentation.screens.settings.SettingsScreen

private data class TabItem(val route: Routes, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.Home, R.string.tab_home, Icons.Filled.Home),
    TabItem(Routes.Download, R.string.tab_download, Icons.Filled.Download),
    TabItem(Routes.Library, R.string.tab_library, Icons.Filled.LibraryMusic),
)

@Composable
fun YmdApp() {
    val navController = rememberNavController()
    Scaffold(bottomBar = { BottomBar(navController) }) { padding ->
        NavGraph(navController, Modifier.padding(padding))
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current?.hierarchy?.any { it.route == tab.route.route } == true,
                onClick = { navController.navigate(tab.route.route) { launchSingleTop = true } },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

/** Side rail variant — call from `Row { SideRail(...); content }` when expanded layout is needed. */
@Composable
fun SideRail(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination
    NavigationRail {
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = current?.hierarchy?.any { it.route == tab.route.route } == true,
                onClick = { navController.navigate(tab.route.route) { launchSingleTop = true } },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onOpenAuth = { navController.navigate(Routes.Auth.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) },
                onOpenAbout = { navController.navigate(Routes.About.route) },
                onOpenDownload = { navController.navigate(Routes.Download.route) },
            )
        }
        composable(Routes.Download.route) { DownloadScreen() }
        composable(Routes.Library.route) { LibraryScreen() }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onOpenAuth = { navController.navigate(Routes.Auth.route) },
                onOpenAbout = { navController.navigate(Routes.About.route) },
            )
        }
        composable(Routes.Auth.route) { AuthScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.About.route) { AboutScreen(onBack = { navController.popBackStack() }) }
    }
}

@Suppress("unused")
@Composable
private fun keepImports(modifier: Modifier) = Row(modifier = Modifier.fillMaxSize()) {}

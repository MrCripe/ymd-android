package com.mrcriper.ymd.presentation.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Download : Routes("download")
    data object Library : Routes("library")
    data object Settings : Routes("settings")
    data object Auth : Routes("auth")
    data object About : Routes("about")
}

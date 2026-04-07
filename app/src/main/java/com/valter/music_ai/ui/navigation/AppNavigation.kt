package com.valter.music_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.valter.music_ai.SplashScreen
import com.valter.music_ai.ui.features.home.HomeScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen()
        }
        composable("home") {
            HomeScreen()
        }
    }
}

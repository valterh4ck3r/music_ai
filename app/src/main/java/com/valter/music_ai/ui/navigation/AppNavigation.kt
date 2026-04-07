package com.valter.music_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.valter.music_ai.SplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.valter.music_ai.ui.features.home.HomeScreen
import com.valter.music_ai.ui.features.home.HomeViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen()
        }
        composable("home") {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(viewModel = homeViewModel)
        }
    }
}

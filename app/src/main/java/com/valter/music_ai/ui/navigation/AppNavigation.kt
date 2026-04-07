package com.valter.music_ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.valter.music_ai.ui.features.home.HomeScreen
import com.valter.music_ai.ui.features.home.HomeViewModel
import com.valter.music_ai.ui.features.song.SongScreen
import com.valter.music_ai.ui.features.song.SongViewModel
import com.valter.music_ai.ui.features.splash.SplashScreen

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
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSong = { songBase64 ->
                    navController.navigate("song/$songBase64")
                }
            )
        }
        composable("song/{songBase64}") {
            val songViewModel: SongViewModel = hiltViewModel()
            SongScreen(
                viewModel = songViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
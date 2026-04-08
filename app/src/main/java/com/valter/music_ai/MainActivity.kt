package com.valter.music_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.valter.music_ai.ui.core.connectivity.ConnectivityBanner
import com.valter.music_ai.ui.core.connectivity.ConnectivityStatus
import com.valter.music_ai.ui.core.connectivity.ConnectivityViewModel
import com.valter.music_ai.ui.navigation.AppNavigation
import com.valter.music_ai.ui.theme.Music_aiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // Install Splash Screen
        installSplashScreen()

        // Continue with regular onCreate
        super.onCreate(savedInstanceState)

        // Set up edge-to-edge display
        enableEdgeToEdge()

        setContent {
            Music_aiTheme {
                val mainViewModel: MainViewModel = viewModel()
                val showSplash by mainViewModel.showSplash.collectAsState(initial = true)
                val navController = rememberNavController()

                // Connectivity setup via Hilt
                val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
                val connectivityStatus by connectivityViewModel.status.collectAsState()

                LaunchedEffect(showSplash) {
                    if (!showSplash) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                SafeArea(connectivityStatus = connectivityStatus , navController = navController)
            }
        }
    }
}

@Composable
fun SafeArea(connectivityStatus : ConnectivityStatus, navController : NavHostController ) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica o padding seguro
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ConnectivityBanner(status = connectivityStatus)
                AppNavigation(navController = navController)
            }
        }
    }
}
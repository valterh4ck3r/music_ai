package com.valter.music_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.valter.music_ai.data.connectivity.AndroidNetworkConnectivityObserver
import com.valter.music_ai.ui.core.connectivity.ConnectivityBanner
import com.valter.music_ai.ui.core.connectivity.ConnectivityViewModel
import com.valter.music_ai.ui.navigation.AppNavigation
import com.valter.music_ai.ui.theme.Music_aiTheme

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

                // Connectivity setup
                val connectivityObserver = remember {
                    AndroidNetworkConnectivityObserver(this@MainActivity)
                }
                val connectivityViewModel: ConnectivityViewModel = viewModel(
                    factory = ConnectivityViewModel.Factory(connectivityObserver)
                )
                val connectivityStatus by connectivityViewModel.status.collectAsState()

                LaunchedEffect(showSplash) {
                    if (!showSplash) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    ConnectivityBanner(status = connectivityStatus)
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background image — stretched to fill the entire screen
        Image(
            painter = painterResource(id = R.drawable.ic_background_no_logo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Logo centered on top of the background
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Music AI Logo",
            modifier = Modifier.size(120.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    Music_aiTheme {
        SplashScreen()
    }
}
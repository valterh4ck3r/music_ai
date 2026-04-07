package com.valter.music_ai.ui.features.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valter.music_ai.R
import com.valter.music_ai.ui.theme.Music_aiTheme

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
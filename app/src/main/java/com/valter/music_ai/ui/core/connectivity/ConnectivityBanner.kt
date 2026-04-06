package com.valter.music_ai.ui.core.connectivity

import androidx.compose.animation.AnimatedVisibility
import com.valter.music_ai.ui.theme.OfflineGrey
import com.valter.music_ai.ui.theme.OnlineGreen
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ConnectivityBanner(
    status: ConnectivityStatus,
    modifier: Modifier = Modifier
) {
    val isVisible = status != ConnectivityStatus.HIDDEN

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val (backgroundColor, message) = when (status) {
            ConnectivityStatus.DISCONNECTED -> OfflineGrey to "No Internet"
            ConnectivityStatus.CONNECTED    -> OnlineGreen to "Connected"
            ConnectivityStatus.HIDDEN       -> Color.Transparent to ""
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

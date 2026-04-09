package com.valter.music_ai.ui.features.song

import android.Manifest
import android.content.ComponentName
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.common.util.concurrent.MoreExecutors
import com.valter.music_ai.playback.MusicPlayerService
import com.valter.music_ai.ui.features.song.components.SongOptionsBottomSheet
import com.valter.music_ai.ui.theme.DarkBackground
import com.valter.music_ai.ui.theme.OnDarkTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongScreen(
    viewModel: SongViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAlbum: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tabletWidth = 600
    val isTablet = LocalWindowInfo.current.containerSize.width >= tabletWidth

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlayerService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                controllerFuture.addListener({
                    try {
                        viewModel.setPlayer(controllerFuture.get())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, MoreExecutors.directExecutor())
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.setPlayer(null)
                MediaController.releaseFuture(controllerFuture)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setPlayer(null)
            MediaController.releaseFuture(controllerFuture)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val song = uiState.song

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val highResArtwork = song?.artworkUrl100?.replace("100x100bb", "600x600bb")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Now playing",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { showSheet = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        when (uiState.status) {
            SongUiStatus.ERROR -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Error",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Playback Error",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            else -> {
                // Success / Loading / Playing Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = highResArtwork,
                        contentDescription = "Song artwork: ${song?.trackName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .then(
                                if (isTablet) Modifier.size(340.dp) 
                                else Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
                            )
                            .clip(RoundedCornerShape(28.dp))
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Song Info
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text(
                        text = song?.trackName ?: "Unknown Track",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song?.artistName ?: "Unknown Artist",
                        color = OnDarkTextSecondary,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Progress Bar
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Slider(
                        value = uiState.progressMs.toFloat(),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..(uiState.totalMs.coerceAtLeast(1L).toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .semantics { contentDescription = "Song progress" }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(uiState.progressMs),
                            color = OnDarkTextSecondary.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "-" + formatTime(uiState.totalMs - uiState.progressMs),
                            color = OnDarkTextSecondary.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play/Pause button container
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        when (uiState.status) {
                            SongUiStatus.LOADING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                            SongUiStatus.PLAYING -> {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.1f))

                    // Central Skip Controls
                    Row {
                        IconButton(onClick = { viewModel.previousSong() }) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Previous song",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(onClick = { viewModel.nextSong() }) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Next song",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.3f))

                    // Loop/Repeat
                    IconButton(onClick = { viewModel.toggleRepeat() }) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Toggle repeat",
                            tint = if (uiState.isRepeatEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(0.3f))
            }
        }
    }

    if (showSheet) {
        SongOptionsBottomSheet(
            song = song,
            sheetState = sheetState,
            onDismissRequest = { showSheet = false },
            onViewAlbumClick = {
                song?.let { s ->
                    val json = com.google.gson.Gson().toJson(s)
                    val base64 = android.util.Base64.encodeToString(
                        json.toByteArray(),
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                    )
                    onNavigateToAlbum(base64)
                }
                showSheet = false
            }
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

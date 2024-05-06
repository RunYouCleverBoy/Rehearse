package com.playgrounds.mirrormirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.playgrounds.mirrormirror.ui.theme.MirrorMirrorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    var onPermissionEvent = { _: Collection<String> -> }
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys
            onPermissionEvent(deniedPermissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel by viewModels<MirrorViewModel>()
        setContent {
            LaunchedEffect(Unit) {
                onPermissionEvent = { permissions ->
                    viewModel.dispatchEvent(MainEvent.PermissionsMissing(permissions.toList()))
                }

                launch {
                    delay(100.milliseconds)
                    viewModel.dispatchEvent(MainEvent.AppStarted)
                }

                viewModel.actions.collect { action ->
                    when (action) {
                        is MainAction.RequestPermissions -> checkPermissions(action.permissions)
                    }
                }
            }

            MirrorMirrorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()
                    MirrorMirrorScreen(state, viewModel::dispatchEvent)
                }
            }
        }
    }

    private fun checkPermissions(permissions: Array<String>) {
        if (permissions.all { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            onPermissionEvent(emptyList())
        } else {
            activityResultLauncher.launch(permissions)
        }
    }

    @Composable
    private fun MirrorMirrorScreen(state: MirrorState, onSendEvent: (MainEvent) -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            VideoScreen(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(), state = state
            )
            Controls(modifier = Modifier.fillMaxWidth(), state = state, onEvent = onSendEvent)
        }
    }

    @Composable
    private fun VideoScreen(modifier: Modifier, state: MirrorState) {
        Box(modifier = modifier) {
            val showPreview = remember(state.recordingScreenConfiguration, state.cameraData) { state.recordingScreenConfiguration.shouldShowPreview }
            when {
                showPreview -> {
                    state.cameraData?.preview?.let { preview ->
                        CameraPreview(preview)
                    }
                }

                state.recordingScreenConfiguration is RecordingScreenConfiguration.Replaying -> ReplayScreen(state.recordingScreenConfiguration.duration, state.lastRecordingFile)
                else -> ColorBox(color = Color.Gray, text = state.recordingConfigurationName)
            }
        }
    }

    @Composable
    private fun ReplayScreen(duration: Duration, replayFile: File?) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            replayFile?.let { file -> VideoPlayer(modifier = Modifier.fillMaxSize(), file = file) }
            Text(
                text = "Replaying for ${duration.inWholeMinutes} minutes",
                style = TextStyle(fontSize = 20.sp, color = Color.White)
            )
        }
    }

    @Composable
    private fun ColorBox(color: Color, text: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color), contentAlignment = Alignment.Center
        ) {
            Text(text = text)
        }
    }

    @Composable
    private fun Controls(modifier: Modifier, state: MirrorState, onEvent: (MainEvent) -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            InterchangeableIcon(state.recPauseIcon) { onEvent(MainEvent.StartStopClicked(context, lifecycleOwner)) }
            InterchangeableIcon(state.replayIcon) { onEvent(MainEvent.ReplayClicked(context)) }
            InterchangeableIcon(state.deleteIcon) { onEvent(MainEvent.DeleteClicked(context)) }
        }
    }

    @Composable
    private fun InterchangeableIcon(state: IconAndText, onClick: () -> Unit) {
        IconButton(onClick = onClick, enabled = state.enabled) {
            Image(painter = painterResource(id = state.icon), contentDescription = stringResource(id = state.text))
        }
    }

}

@Composable
fun VideoPlayer(modifier: Modifier = Modifier, file: File) {
    val context = LocalContext.current
    val exoPlayer = ExoPlayer.Builder(context).build()

    // Create a MediaSource
    val mediaSource = remember(file) {
        MediaItem.fromUri(file.toUri())
    }

    // Set MediaSource to ExoPlayer
    LaunchedEffect(mediaSource) {
        exoPlayer.setMediaItem(mediaSource)
        exoPlayer.prepare()
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply { player = exoPlayer }
        },
        modifier = modifier // Set your desired height
    )

    // Manage lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

}



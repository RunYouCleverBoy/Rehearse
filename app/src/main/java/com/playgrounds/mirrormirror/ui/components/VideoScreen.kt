package com.playgrounds.mirrormirror.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.playgrounds.mirrormirror.CameraPreview
import com.playgrounds.mirrormirror.MainEvent
import com.playgrounds.mirrormirror.MirrorState

@Composable
fun ViewFinderSubScreen(modifier: Modifier, state: MirrorState, onEvent: (MainEvent) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        CameraPreview(configuration = state.cameraData)
        ViewFinderOverlay(colorScheme, state, onEvent)
    }
}

@Composable
private fun ViewFinderOverlay(colorScheme: ColorScheme, state: MirrorState, onEvent: (MainEvent) -> Unit) {
    val context = LocalContext.current
    val recPauseIcon = state.recPauseIcon
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        IconButton(
            modifier = Modifier
                .size(80.dp)
                .background(colorScheme.background.copy(alpha = 0.5f), CircleShape),
            enabled = recPauseIcon.enabled,
            onClick = {
                onEvent(MainEvent.StartStopClicked(context))
            }
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                painter = painterResource(id = recPauseIcon.icon),
                contentDescription = stringResource(id = recPauseIcon.text),
                tint = colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.padding(50.dp))
    }
}

package com.playgrounds.mirrormirror.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playgrounds.mirrormirror.CameraPreview
import com.playgrounds.mirrormirror.MainEvent
import com.playgrounds.mirrormirror.MirrorState
import com.playgrounds.mirrormirror.RecordingScreenConfiguration
import com.playgrounds.mirrormirror.ui.utils.ColorBox

@Composable
fun VideoScreen(modifier: Modifier, state: MirrorState, onEvent: (MainEvent) -> Unit) {
    Box(modifier = modifier) {
        val showPreview = remember(state.recordingScreenConfiguration, state.cameraData) { state.recordingScreenConfiguration.shouldShowPreview }
        when {
            showPreview && state.cameraData != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    CameraPreview(state.cameraData) {
                        onEvent(MainEvent.PreviewConfigurationApplied(it))
                    }
                    if (state.recordingScreenConfiguration is RecordingScreenConfiguration.Recording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.2f))
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = state.recordingScreenConfiguration.formattedTime, color = Color.White, style = TextStyle(fontSize = 24.sp))
                        }
                    }
                }
            }

            state.recordingScreenConfiguration is RecordingScreenConfiguration.Replaying -> ReplayScreen(state.recordingScreenConfiguration.duration, state.lastRecordingFile)
            else -> ColorBox(color = Color.Gray, text = state.recordingConfigurationName)
        }
    }
}
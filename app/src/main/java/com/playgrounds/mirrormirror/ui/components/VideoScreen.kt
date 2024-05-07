package com.playgrounds.mirrormirror.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                CameraPreview(state.cameraData) {
                    onEvent(MainEvent.PreviewConfigurationApplied(it))
                }
            }

            state.recordingScreenConfiguration is RecordingScreenConfiguration.Replaying -> ReplayScreen(state.recordingScreenConfiguration.duration, state.lastRecordingFile)
            else -> ColorBox(color = Color.Gray, text = state.recordingConfigurationName)
        }
    }
}
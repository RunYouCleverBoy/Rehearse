package com.playgrounds.mirrormirror.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.io.File

@Composable
fun ReplayScreen(replayFile: File?) {
    replayFile?.let { file -> VideoPlayer(modifier = Modifier.fillMaxSize(), file = file) }
}
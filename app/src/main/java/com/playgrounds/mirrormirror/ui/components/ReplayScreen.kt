package com.playgrounds.mirrormirror.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.time.Duration

@Composable
fun ReplayScreen(duration: Duration, replayFile: File?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        replayFile?.let { file -> VideoPlayer(modifier = Modifier.fillMaxSize(), file = file) }
        Text(
            text = "Replaying for ${duration.inWholeMinutes} minutes",
            style = TextStyle(fontSize = 20.sp, color = Color.White)
        )
    }
}
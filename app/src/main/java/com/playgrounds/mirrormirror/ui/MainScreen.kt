package com.playgrounds.mirrormirror

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.playgrounds.mirrormirror.ui.utils.InterchangeableIcon
import com.playgrounds.mirrormirror.ui.components.VideoScreen

@Composable
fun MirrorMirrorScreen(state: MirrorState, onSendEvent: (MainEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        VideoScreen(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(), state = state,
            onSendEvent
        )
        Controls(modifier = Modifier.fillMaxWidth(), state = state, onEvent = onSendEvent)
    }
}

@Composable
fun Controls(modifier: Modifier, state: MirrorState, onEvent: (MainEvent) -> Unit) {
    val context = LocalContext.current
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        InterchangeableIcon(state.recPauseIcon) { onEvent(MainEvent.StartStopClicked(context)) }
        InterchangeableIcon(state.replayIcon) { onEvent(MainEvent.ReplayClicked(context)) }
        InterchangeableIcon(state.deleteIcon) { onEvent(MainEvent.DeleteClicked(context)) }
    }
}


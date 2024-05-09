package com.playgrounds.mirrormirror.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playgrounds.mirrormirror.R
import com.playgrounds.mirrormirror.ui.main.MainEvent
import com.playgrounds.mirrormirror.ui.main.MirrorViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WelcomeScreen(onEvent: (MainEvent) -> Unit) {
    val viewmodel = viewModel<MirrorViewModel>()
    val state by viewmodel.state.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.recordingScreenConfiguration == RecordingScreenConfiguration.NotAvailable)  {
            Image(painter = painterResource(id = R.drawable.road_lock), contentDescription = stringResource(id = R.string.not_available), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
    LaunchedEffect(state.recordingScreenConfiguration) {
        delay(100.milliseconds)
        onEvent(MainEvent.WelcomeScreenDone)
    }
}
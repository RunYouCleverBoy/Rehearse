package com.rycbar.rehearse.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rycbar.rehearse.R
import com.rycbar.rehearse.ui.main.mvi.MainEvent
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun WelcomeScreen(permissionsStillMissing: Boolean, onMainEvent: (MainEvent) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.recording_room),
            contentDescription = stringResource(id = R.string.welcome_screen),
            contentScale = ContentScale.Crop
        )

        LaunchedEffect(key1 = Unit) {
            val delay = if (permissionsStillMissing) 2.seconds else 0.7.seconds
            delay(delay)
            onMainEvent(MainEvent.WelcomeScreenComplete(permissionsStillMissing))
        }
    }
}
package com.rycbar.rehearse.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rycbar.rehearse.R
import com.rycbar.rehearse.repos.permission.PermissionHandler
import com.rycbar.rehearse.ui.main.mvi.MainEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PermissionScreen(permissionHandler: PermissionHandler, onMainEvent: (MainEvent) -> Unit) {
    val permissionViewModel = viewModel<PermissionViewModel>()
    val state by permissionViewModel.state.collectAsState()

    var showingDialog by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    if (showingDialog != null) {
        permissionHandler.ShowPermissionRationaleDialog {
            showingDialog?.complete(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.showPermissionGraphics)  {
            Image(painter = painterResource(id = R.drawable.road_lock), contentDescription = stringResource(id = R.string.not_available), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
    LaunchedEffect(Unit) {
        launch {
            permissionViewModel.action.collect { action ->
                when (action) {
                    PermissionViewModel.Action.GoToSettings -> permissionHandler.goToSettings()
                    PermissionViewModel.Action.MoveToNextScreen -> {
                        onMainEvent(MainEvent.ReadyForMainScreen)
                        return@collect
                    }

                    PermissionViewModel.Action.WithRationale -> {
                        showingDialog = CompletableDeferred()
                        showingDialog?.await()
                        showingDialog = null
                    }
                    PermissionViewModel.Action.WithoutRationale -> Unit
                }
                val remedy = permissionHandler.requestMissingPermissions().await()
                permissionViewModel.dispatchEvent(PermissionViewModel.Event.PermissionsMissing(remedy))
            }
        }
        delay(100.milliseconds)
//        onMainEvent(MainEvent.WelcomeScreenDone)
    }
}


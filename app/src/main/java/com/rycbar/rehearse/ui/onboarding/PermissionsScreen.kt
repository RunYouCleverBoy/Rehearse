package com.rycbar.rehearse.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun PermissionsScreen(permissionHandler: PermissionHandler, onMainEvent: (MainEvent) -> Unit) {
    val welcomeViewModel = viewModel<PermissionsViewModel>()

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
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = R.drawable.road_lock),
            contentScale = ContentScale.Crop,
            contentDescription = stringResource(id = R.string.not_available)
        )
    }
    LaunchedEffect(Unit) {
        launch {
            welcomeViewModel.action.collect { action ->
                when (action) {
                    PermissionsViewModel.Action.GoToSettings -> permissionHandler.goToSettings()
                    PermissionsViewModel.Action.MoveToNextScreen -> {
                        onMainEvent(MainEvent.ReadyForMainScreen)
                        return@collect
                    }

                    PermissionsViewModel.Action.WithRationale -> {
                        showingDialog = CompletableDeferred()
                        showingDialog?.await()
                        showingDialog = null
                    }

                    PermissionsViewModel.Action.WithoutRationale -> Unit
                }
                val remedy = permissionHandler.requestMissingPermissions().await()
                welcomeViewModel.dispatchEvent(PermissionsViewModel.Event.PermissionsMissing(remedy))
            }
        }
        delay(100.milliseconds)
    }
}


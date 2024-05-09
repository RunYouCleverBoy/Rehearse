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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playgrounds.mirrormirror.R
import com.playgrounds.mirrormirror.repos.permission.PermissionHandler
import com.playgrounds.mirrormirror.ui.main.MainEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WelcomeScreen(permissionHandler: PermissionHandler, onMainEvent: (MainEvent) -> Unit) {
    val welcomeViewModel = viewModel<WelcomeScreenViewModel>()
    val state by welcomeViewModel.state.collectAsState()

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
            welcomeViewModel.action.collect { action ->
                when (action) {
                    WelcomeScreenViewModel.Action.GoToSettings -> permissionHandler.goToSettings()
                    WelcomeScreenViewModel.Action.MoveToNextScreen -> {
                        onMainEvent(MainEvent.WelcomeScreenDone)
                        return@collect
                    }

                    WelcomeScreenViewModel.Action.WithRationale -> {
                        showingDialog = CompletableDeferred()
                        showingDialog?.await()
                        showingDialog = null
                    }
                    WelcomeScreenViewModel.Action.WithoutRationale -> Unit
                }
                val remedy = permissionHandler.requestMissingPermissions().await()
                welcomeViewModel.dispatchEvent(WelcomeScreenViewModel.Event.PermissionsMissing(remedy))
            }
        }
        delay(100.milliseconds)
//        onMainEvent(MainEvent.WelcomeScreenDone)
    }
}

class WelcomeScreenViewModel : ViewModel() {
    data class State(val showPermissionGraphics: Boolean)
    private val _state = MutableStateFlow(State(showPermissionGraphics = true))
    val state = _state.asStateFlow()

    private val _action = MutableSharedFlow<Action>(1, 1)
    val action = _action.asSharedFlow()

    sealed class Action {
        data object MoveToNextScreen: Action()
        data object WithoutRationale: Action()
        data object WithRationale: Action()
        data object GoToSettings: Action()
    }

    sealed class Event {
        data class PermissionsMissing(val remedy: PermissionHandler.Remedy): Event()
    }

    init {
        emit(Action.WithoutRationale)
    }

    fun dispatchEvent(event: Event) {
        when (event) {
            is Event.PermissionsMissing -> onPermissionResult(event.remedy)
        }
    }

    private fun onPermissionResult(remedy: PermissionHandler.Remedy) {
        when (remedy) {
            PermissionHandler.Remedy.AllGranted -> emit(Action.MoveToNextScreen)
            is PermissionHandler.Remedy.ShouldShowRationale -> emit(Action.WithRationale)
            is PermissionHandler.Remedy.CanRequest -> emit(Action.WithoutRationale)
            is PermissionHandler.Remedy.ShouldGoToSettings -> emit(Action.GoToSettings)
        }

        _state.update { it.copy(showPermissionGraphics = remedy !is PermissionHandler.Remedy.AllGranted) }
    }

    private fun emit(action: Action) {
        viewModelScope.launch { _action.emit(action) }
    }

}
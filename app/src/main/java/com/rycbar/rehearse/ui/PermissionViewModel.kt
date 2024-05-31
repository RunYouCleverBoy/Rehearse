package com.rycbar.rehearse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rycbar.rehearse.repos.permission.PermissionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PermissionViewModel : ViewModel() {
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
package com.playgrounds.mirrormirror

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class MirrorViewModel : ViewModel() {
    private val actionsMutable = MutableSharedFlow<MainAction>(replay = 1, extraBufferCapacity = 1)
    val actions: SharedFlow<MainAction> = actionsMutable
    private val stateMutable = MutableStateFlow(MirrorState())
    val state: StateFlow<MirrorState> = stateMutable

    fun dispatchEvent(event: MainEvent) {
        when (event) {
            is MainEvent.AppStarted -> requestPermissions()
            is MainEvent.PermissionsMissing -> if (event.permissions.isEmpty()) {
                setRecordingState(RecordingState.Idle)
            }

            MainEvent.StartStopClicked -> onStartStop()
            MainEvent.ReplayClicked -> TODO()
            MainEvent.DeleteClicked -> TODO()
            is MainEvent.PreviewLifeCycle -> onLifecycleEvent(event.state)
        }
    }

    private fun onLifecycleEvent(state: CameraState) {
        when (state) {
            CameraState.Idle -> setRecordingState(RecordingState.Idle)
            else -> Unit
        }
    }

    private fun setRecordingState(state: RecordingState) {
        stateMutable.update { it.copy(recordingState = state, recPauseIcon = state.toIconAndText(), replayIcon = state.toReplayButton()) }
    }

    private fun requestPermissions() {
        actionsMutable.tryEmit(MainAction.RequestPermissions(PermissionHandler.REQUIRED_PERMISSIONS))
    }

    private fun onStartStop() {
        val currentState = state.value.recordingState
        val newState = when (currentState) {
            is RecordingState.NotAvailable -> RecordingState.NotAvailable
            is RecordingState.Idle -> RecordingState.Recording(System.nanoTime())
            is RecordingState.Starting -> RecordingState.Idle
            is RecordingState.Recording -> RecordingState.Stopping
            is RecordingState.Stopping -> RecordingState.Idle
            is RecordingState.Replaying -> RecordingState.Idle
        }
        setRecordingState(newState)
    }

    private fun RecordingState.toIconAndText(): IconAndText {
        return when (this) {
            is RecordingState.NotAvailable -> IconAndText(R.drawable.ic_rec, R.string.recording_not_available, false)
            is RecordingState.Idle -> IconAndText(R.drawable.ic_rec, R.string.stopped, true)
            is RecordingState.Starting -> IconAndText(R.drawable.ic_stop, R.string.recording_starting, false)
            is RecordingState.Recording -> IconAndText(R.drawable.ic_stop, R.string.recording_in_progress, true)
            is RecordingState.Stopping -> IconAndText(R.drawable.ic_rec, R.string.recording_stopping, false)
            is RecordingState.Replaying -> IconAndText(R.drawable.ic_rec, R.string.replaying, false)
        }
    }

    private fun RecordingState.toReplayButton(): IconAndText = if (this is RecordingState.Replaying) {
        IconAndText(R.drawable.ic_stop, R.string.stop_playback, true)
    } else {
        IconAndText(R.drawable.ic_replay, R.string.replay, true)
    }
}

data class IconAndText(val icon: Int, val text: Int, val enabled: Boolean = true)

data class MirrorState(
    val replayFile: File? = null,
    val recordingState: RecordingState = RecordingState.NotAvailable,
    val recPauseIcon: IconAndText = IconAndText(R.drawable.ic_rec, R.string.recording_pause),
    val replayIcon: IconAndText = IconAndText(R.drawable.ic_replay, R.string.replay),
    val deleteIcon: IconAndText = IconAndText(R.drawable.ic_delete, R.string.delete)
) {
    val name: String
        get() = recordingState.javaClass.simpleName
}

sealed class MainEvent {
    data object AppStarted : MainEvent()
    data class PermissionsMissing(val permissions: List<String>) : MainEvent()
    data object StartStopClicked : MainEvent()
    data object ReplayClicked : MainEvent()
    data object DeleteClicked : MainEvent()
    data class PreviewLifeCycle(val state: CameraState) : MainEvent()
}

sealed class MainAction {
    data class RequestPermissions(val permissions: Array<String>) : MainAction() {
        override fun equals(other: Any?): Boolean = when {
            this === other -> true
            other !is RequestPermissions -> false
            else -> permissions.contentEquals(other.permissions)
        }

        override fun hashCode(): Int = permissions.contentHashCode()
    }
}

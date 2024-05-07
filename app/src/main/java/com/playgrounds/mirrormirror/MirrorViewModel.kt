package com.playgrounds.mirrormirror

import android.content.Context
import android.util.Log
import androidx.camera.video.RecordingStats
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class MirrorViewModel : ViewModel() {
    private val actionsMutable = MutableSharedFlow<MainAction>(replay = 1, extraBufferCapacity = 1)
    val actions: SharedFlow<MainAction> = actionsMutable
    private val stateMutable = MutableStateFlow(MirrorState())
    val state: StateFlow<MirrorState> = stateMutable

    private val cameraRecorder = CameraRecorder()

    init {
        pipelineIncomingFlows()
    }

    fun dispatchEvent(event: MainEvent) {
        when (event) {
            is MainEvent.AppStarted -> requestPermissions()
            is MainEvent.PermissionsMissing -> if (event.permissions.isNotEmpty()) {
                setRecordingState(RecordingScreenConfiguration.Idle)
            }

            is MainEvent.StartStopClicked -> onStartStop(event.context)
            is MainEvent.ReplayClicked -> onReplayClicked(event.context)
            is MainEvent.DeleteClicked -> onDeleteClicked(event.context)
            is MainEvent.PreviewConfigurationApplied -> setRecordingState(RecordingScreenConfiguration.Recording(), event.configuration)
        }
    }

    private fun pipelineIncomingFlows() {
        fun <T> Flow<T>.launchCollect(action: (T) -> Unit) {
            viewModelScope.launch {
                collect { action(it) }
            }
        }

        state.launchCollect(::logScreenConfiguration)
        cameraRecorder.recorderStatistics.launchCollect(::dispatchRecordingStatistics)
        cameraRecorder.recorderState.launchCollect(::dispatchVideoState)
    }

    private fun logScreenConfiguration(it: MirrorState) {
        Log.d("CameraViewModel", "Screen configuration: ${it.recordingConfigurationName}")
    }

    private fun dispatchRecordingStatistics(statistics: RecordingStats?) {
        fun Long.twoDigit() = toString().padStart(2, '0')
        fun Long.format(): String {
            val sec = nanoseconds.inWholeSeconds
            val min = sec / 60
            return "${min.twoDigit()}:${(sec % 60).twoDigit()}"
        }

        val recordingScreenConfiguration = state.value.recordingScreenConfiguration
        if (recordingScreenConfiguration is RecordingScreenConfiguration.Recording) {
            setRecordingState(recordingScreenConfiguration.copy(formattedTime = statistics?.recordedDurationNanos?.format() ?: ""), cameraData = cameraRecorder.cameraData)
        }
    }

    private fun dispatchVideoState(state: CameraState) {
        when (state) {
            CameraState.Active -> setRecordingState(RecordingScreenConfiguration.Recording(""))
            is CameraState.Error -> setRecordingState(RecordingScreenConfiguration.NotAvailable)
            CameraState.Idle -> setRecordingState(RecordingScreenConfiguration.Idle)
            CameraState.Starting -> setRecordingState(RecordingScreenConfiguration.Starting)
            CameraState.Stopping -> setRecordingState(RecordingScreenConfiguration.Stopping)
        }
    }

    private fun setRecordingState(state: RecordingScreenConfiguration, cameraData: CameraRecorder.CameraData? = null) {
        stateMutable.update {
            it.copy(
                recordingScreenConfiguration = state,
                recPauseIcon = state.toIconAndText(),
                replayIcon = state.toReplayButton(),
                cameraData = cameraData ?: it.cameraData
            )
        }
    }

    private fun requestPermissions() {
        actionsMutable.tryEmit(MainAction.RequestPermissions(PermissionHandler.REQUIRED_PERMISSIONS))
    }

    private fun onStartStop(context: Context) {
        viewModelScope.launch {
            val newState = when (val currentState = state.value.recordingScreenConfiguration) {
                is RecordingScreenConfiguration.Idle -> {
                    startRecording(context)
                    RecordingScreenConfiguration.Recording()
                }

                is RecordingScreenConfiguration.Recording -> {
                    cameraRecorder.stopRecording()
                    RecordingScreenConfiguration.Idle
                }

                else -> currentState
            }
            setRecordingState(newState, cameraData = cameraRecorder.cameraData)
        }
    }

    private suspend fun startRecording(context: Context) {
        val file = withContext(Dispatchers.IO) {
            getSaveFile(context).also { it.delete() }
        }

        cameraRecorder.setupCamera(context, targetFile = file)
    }

    private fun getSaveFile(context: Context): File {
        fun filename(index: Int) = "recording_${index.toString().padStart(2, '0')}.mp4"
        val dir = File(context.cacheDir, "recordings").also { it.mkdirs() }
        return File(dir, filename(1))
    }


    private fun onReplayClicked(context: Context) {
        val currentState = state.value
        if (currentState.lastRecordingFile == null) {
            stateMutable.update { it.copy(lastRecordingFile = getSaveFile(context)) }
        }
        when (currentState.recordingScreenConfiguration) {
            is RecordingScreenConfiguration.Replaying -> {
                cameraRecorder.stopRecording()
                setRecordingState(RecordingScreenConfiguration.Idle)
            }

            is RecordingScreenConfiguration.Idle -> {
                setRecordingState(RecordingScreenConfiguration.Replaying(0L.seconds))
            }

            else -> Unit
        }
    }

    private fun onDeleteClicked(context: Context) {
        val file = state.value.lastRecordingFile ?: getSaveFile(context).also {
            stateMutable.update { it.copy(lastRecordingFile = it.lastRecordingFile) }
        }
        file.delete()
    }

    private fun RecordingScreenConfiguration.toIconAndText(): IconAndText {
        return when (this) {
            is RecordingScreenConfiguration.NotAvailable -> IconAndText(R.drawable.ic_rec, R.string.recording_not_available, false)
            is RecordingScreenConfiguration.Idle -> IconAndText(R.drawable.ic_rec, R.string.stopped, true)
            is RecordingScreenConfiguration.Starting -> IconAndText(R.drawable.ic_stop, R.string.recording_starting, true)
            is RecordingScreenConfiguration.Recording -> IconAndText(R.drawable.ic_stop, R.string.recording_in_progress, true)
            is RecordingScreenConfiguration.Stopping -> IconAndText(R.drawable.ic_rec, R.string.recording_stopping, false)
            is RecordingScreenConfiguration.Replaying -> IconAndText(R.drawable.ic_rec, R.string.replaying, false)
        }
    }

    private fun RecordingScreenConfiguration.toReplayButton(): IconAndText = if (this is RecordingScreenConfiguration.Replaying) {
        IconAndText(R.drawable.ic_stop, R.string.stop_playback, true)
    } else {
        IconAndText(R.drawable.ic_replay, R.string.replay, true)
    }
}

data class IconAndText(val icon: Int, val text: Int, val enabled: Boolean = true)

data class MirrorState(
    val lastRecordingFile: File? = null,
    val recordingScreenConfiguration: RecordingScreenConfiguration = RecordingScreenConfiguration.NotAvailable,
    val recPauseIcon: IconAndText = IconAndText(R.drawable.ic_rec, R.string.recording_pause),
    val replayIcon: IconAndText = IconAndText(R.drawable.ic_replay, R.string.replay),
    val deleteIcon: IconAndText = IconAndText(R.drawable.ic_delete, R.string.delete),
    val cameraData: CameraRecorder.CameraData? = null
) {
    val recordingConfigurationName: String
        get() = recordingScreenConfiguration.javaClass.simpleName
}

sealed class MainEvent {
    data object AppStarted : MainEvent()
    data class PermissionsMissing(val permissions: List<String>) : MainEvent()
    data class StartStopClicked(val context: Context) : MainEvent()
    data class ReplayClicked(val context: Context) : MainEvent()
    data class DeleteClicked(val context: Context) : MainEvent()
    data class PreviewConfigurationApplied(val configuration: CameraRecorder.CameraData) : MainEvent()
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

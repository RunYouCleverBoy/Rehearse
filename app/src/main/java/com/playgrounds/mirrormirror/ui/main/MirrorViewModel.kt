package com.playgrounds.mirrormirror.ui.main

import android.content.Context
import android.util.Log
import androidx.camera.video.RecordingStats
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import com.playgrounds.mirrormirror.R
import com.playgrounds.mirrormirror.repos.camera.CameraRecorder
import com.playgrounds.mirrormirror.repos.camera.CameraState
import com.playgrounds.mirrormirror.ui.RecordingScreenConfiguration
import com.playgrounds.mirrormirror.ui.Screens
import com.playgrounds.mirrormirror.ui.TabInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.nanoseconds

class MirrorViewModel : ViewModel() {
    private val actionsMutable = MutableSharedFlow<MainAction>(replay = 1, extraBufferCapacity = 2)
    val actions: SharedFlow<MainAction> = actionsMutable
    private val stateMutable = MutableStateFlow(MirrorState())
    val state: StateFlow<MirrorState> = stateMutable

    private val cameraRecorder = CameraRecorder()

    init {
        pipelineIncomingFlows()
    }

    fun dispatchEvent(event: MainEvent) {
        when (event) {
            MainEvent.WelcomeScreenDone -> onWelcomeScreenDone()
            is MainEvent.BackStackEntryChanged -> onBackStackEntryChanged(event.entry)
            is MainEvent.StartStopClicked -> onStartStop(event.context)
            is MainEvent.DeleteClicked -> onDeleteClicked(event.context)
            is MainEvent.TabSelected -> onTabSelected(event)
        }
    }

    private fun onWelcomeScreenDone() {
        emit(MainAction.NavigateTo(Screens.Viewfinder))
    }

    private fun onBackStackEntryChanged(entry: NavBackStackEntry) {
        val route = entry.destination.route
        val screen = Screens.entries.find { it.path == route } ?: return
        stateMutable.update { it.copy(
            tabsOpacity = when (screen) {
                Screens.Viewfinder -> 0.5f
                Screens.Replay -> 1f
                else -> 0f
            },
            selectedTabIndex = state.value.tabs.indexOfFirst { tab -> tab.screen.path == route }) }
    }

    private fun onTabSelected(event: MainEvent.TabSelected) {
        val uiState = state.value
        val destination = uiState.tabs[event.index].screen
        if (uiState.selectedTabIndex == event.index) return
        emit(MainAction.NavigateTo(destination))

        when (destination) {
            Screens.Viewfinder -> setRecordingState(RecordingScreenConfiguration.Idle)
            Screens.Replay -> stateMutable.update { it.copy(lastRecordingFile = it.lastRecordingFile ?: getSaveFile(event.context)) }
            else -> Unit
        }
    }

    @OptIn(FlowPreview::class)
    private fun pipelineIncomingFlows() {
        fun <T> Flow<T>.launchCollect(action: suspend CoroutineScope.(T) -> Unit) {
            viewModelScope.launch {
                collect { action(it) }
            }
        }

        state.launchCollect { logScreenConfiguration(it) }
        cameraRecorder.recorderStatistics.debounce(800).launchCollect { dispatchRecordingStatistics(it) }
        cameraRecorder.recorderState.launchCollect{dispatchVideoState(it)}
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
            is CameraState.Error -> setRecordingState(RecordingScreenConfiguration.Idle)
            CameraState.Idle -> setRecordingState(RecordingScreenConfiguration.Idle)
            CameraState.Starting -> Unit
            CameraState.Stopping -> Unit
        }
    }

    private fun setRecordingState(recordingMode: RecordingScreenConfiguration, cameraData: CameraRecorder.CameraData = state.value.cameraData) {
        stateMutable.update {
            it.copy(
                recordingScreenConfiguration = recordingMode,
                recPauseIcon = recordingMode.toIconAndText(),
                cameraData = cameraData
            )
        }
    }

    private fun onStartStop(context: Context) {
        viewModelScope.launch {
            when (state.value.recordingScreenConfiguration) {
                is RecordingScreenConfiguration.Idle -> startRecording(context)
                is RecordingScreenConfiguration.Recording -> cameraRecorder.stopRecording()
                else -> Unit
            }
        }
    }

    private suspend fun startRecording(context: Context) {
        val file = withContext(Dispatchers.IO) {
            getSaveFile(context).also { it.delete() }
        }

        cameraRecorder.setupCamera(context, targetFile = file)
        setRecordingState(RecordingScreenConfiguration.ShouldRecord, cameraRecorder.cameraData)
    }

    private fun getSaveFile(context: Context): File {
        fun filename(index: Int) = "recording_${index.toString().padStart(2, '0')}.mp4"
        val dir = File(context.cacheDir, "recordings").also { it.mkdirs() }
        return File(dir, filename(1))
    }


    private fun onDeleteClicked(context: Context) {
        val file = state.value.lastRecordingFile ?: getSaveFile(context).also {
            stateMutable.update { it.copy(lastRecordingFile = it.lastRecordingFile) }
        }
        file.delete()
    }

    private fun emit(action: MainAction) = viewModelScope.launch { actionsMutable.emit(action) }

    private fun RecordingScreenConfiguration.toIconAndText(): IconAndText {
        return when (this) {
            is RecordingScreenConfiguration.NotAvailable -> IconAndText(R.drawable.ic_rec, R.string.recording_not_available, false)
            is RecordingScreenConfiguration.Idle -> IconAndText(R.drawable.ic_rec, R.string.stopped, true)
            is RecordingScreenConfiguration.ShouldRecord -> IconAndText(R.drawable.ic_stop, R.string.recording_in_progress, false)
            is RecordingScreenConfiguration.Recording -> IconAndText(R.drawable.ic_stop, R.string.recording_in_progress, true)
        }
    }
}

data class IconAndText(val icon: Int, val text: Int, val enabled: Boolean = true)

data class MirrorState(
    val lastRecordingFile: File? = null,
    val recordingScreenConfiguration: RecordingScreenConfiguration = RecordingScreenConfiguration.NotAvailable,
    val recPauseIcon: IconAndText = IconAndText(R.drawable.ic_rec, R.string.recording_pause),
    val replayIcon: IconAndText = IconAndText(R.drawable.ic_replay, R.string.replay),
    val deleteIcon: IconAndText = IconAndText(R.drawable.ic_delete, R.string.delete),
    val cameraData: CameraRecorder.CameraData = CameraRecorder.previewCameraData,
    val tabs: List<TabInfo> = listOf(
        TabInfo(R.string.record, R.drawable.ic_rec, Screens.Viewfinder),
        TabInfo(R.string.replay, R.drawable.ic_replay, Screens.Replay)
    ),
    val tabsOpacity: Float = 0.5f,
    val selectedTabIndex: Int = 0,
) {
    val recordingConfigurationName: String
        get() = recordingScreenConfiguration.javaClass.simpleName
}

sealed class MainEvent {
    data object WelcomeScreenDone : MainEvent()
    data class BackStackEntryChanged(val entry: NavBackStackEntry) : MainEvent()
    data class StartStopClicked(val context: Context) : MainEvent()
    data class DeleteClicked(val context: Context) : MainEvent()
    data class TabSelected(val context: Context, val index: Int) : MainEvent()
}

sealed class MainAction {
    data class NavigateTo(val destination: Screens) : MainAction()
}

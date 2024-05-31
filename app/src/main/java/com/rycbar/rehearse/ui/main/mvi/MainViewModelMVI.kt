package com.rycbar.rehearse.ui.main.mvi

import android.content.Context
import androidx.navigation.NavBackStackEntry
import com.rycbar.rehearse.R
import com.rycbar.rehearse.repos.camera.CameraRecorder
import com.rycbar.rehearse.ui.RecordingScreenConfiguration
import com.rycbar.rehearse.ui.Screens
import com.rycbar.rehearse.ui.TabInfo
import com.rycbar.rehearse.ui.main.IconAndText
import java.io.File

data class RehearseState(
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
    data object ReadyForMainScreen : MainEvent()
    data class BackStackEntryChanged(val entry: NavBackStackEntry) : MainEvent()
    data class WelcomeScreenComplete(val permissionsLeft: Boolean) : MainEvent()
    data class StartStopClicked(val context: Context) : MainEvent()
    data class DeleteClicked(val context: Context) : MainEvent()
    data class TabSelected(val context: Context, val index: Int) : MainEvent()
}

sealed class MainAction {
    data class NavigateTo(val destination: Screens) : MainAction()
}
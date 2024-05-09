package com.playgrounds.mirrormirror.ui


data class TabInfo(val title: Int, val companionIcon: Int, val screen: Screens)

sealed class RecordingScreenConfiguration {
    data object NotAvailable : RecordingScreenConfiguration()
    data object Idle : RecordingScreenConfiguration()
    data object ShouldRecord : RecordingScreenConfiguration()
    data class Recording(val formattedTime: String = "") : RecordingScreenConfiguration()
}

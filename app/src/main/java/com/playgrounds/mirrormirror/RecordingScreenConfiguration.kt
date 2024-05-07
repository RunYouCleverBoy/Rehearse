package com.playgrounds.mirrormirror

import kotlin.time.Duration


sealed class RecordingScreenConfiguration(val shouldShowPreview: Boolean) {
    data object NotAvailable : RecordingScreenConfiguration(false)
    data object Idle : RecordingScreenConfiguration(false)
    data object Starting : RecordingScreenConfiguration(true)
    data class Recording(val startTime: Long) : RecordingScreenConfiguration(true)
    data object Stopping : RecordingScreenConfiguration(true)
    data class Replaying(val duration: Duration) : RecordingScreenConfiguration(false)
}
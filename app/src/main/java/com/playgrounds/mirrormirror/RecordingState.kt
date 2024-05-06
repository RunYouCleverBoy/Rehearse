package com.playgrounds.mirrormirror

import kotlin.time.Duration


sealed class RecordingState {
    data object NotAvailable : RecordingState()
    data object Idle : RecordingState()
    data object Starting : RecordingState()
    data class Recording(val startTime: Long) : RecordingState()
    data object Stopping : RecordingState()
    data class Replaying(val duration: Duration) : RecordingState()
}
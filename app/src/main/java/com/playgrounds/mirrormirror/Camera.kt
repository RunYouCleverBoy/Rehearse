package com.playgrounds.mirrormirror

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.RecordingStats
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

sealed class CameraState {
    data object Idle : CameraState()
    data object Starting : CameraState()
    data object Active : CameraState()
    data object Stopping : CameraState()
    data class Error(val message: String) : CameraState()
}

@Composable
fun CameraPreview(configuration: CameraRecorder.CameraData) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context)
    }

    LaunchedEffect(lifecycleOwner, configuration) {
        val cameraxSelector = CameraSelector.Builder().requireLensFacing(configuration.lensFacing).build()
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }

        cameraProvider.unbindAll()
        if (configuration.videoCapture != null) {
            Log.v("CameraPreview", "Binding video capture")
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, configuration.videoCapture, configuration.preview)
        } else {
            Log.v("CameraPreview", "Binding PREVIEW capture")
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, configuration.preview)
        }

        configuration.preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

class CameraRecorder(private val fileSizeLimitMB: Int = 512, private val durationLimit: Duration = 1.hours) {
    data class CameraData(val lensFacing: Int, val videoCapture: VideoCapture<Recorder>?, val preview: Preview?)

    private var recording: Recording? = null
    private val recorderTag = "CameraRecorder"
    private val recorderStatsMutableStateFlow = MutableStateFlow<RecordingStats?>(null)
    val recorderStatistics: StateFlow<RecordingStats?> = recorderStatsMutableStateFlow
    private val recorderMutableStateFlow = MutableStateFlow<CameraState>(CameraState.Idle)
    val recorderState: StateFlow<CameraState> = recorderMutableStateFlow

    var cameraData: CameraData = previewCameraData
        private set

    private val qualitySelector = QualitySelector.from(
        Quality.SD,
        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
    )

    init {
        CoroutineScope(Dispatchers.Main).launch {
            recorderState.collect{
                Log.d(recorderTag, "Camera state: $it")
            }
        }
    }

    fun setupCamera(
        context: Context,
        targetFile: File? = null
    ) {
        recorderMutableStateFlow.value = CameraState.Starting
        val videoCapture: VideoCapture<Recorder>? = targetFile?.let {
            getVideoCapture().also { videoCapture -> videoCapture.setMediaSession(context, targetFile) }
        }

        cameraData = CameraData(CameraSelector.LENS_FACING_FRONT, videoCapture, cameraData.preview ?: Preview.Builder().build())
        Log.v(recorderTag, "Camera setup complete")
    }

    fun stopRecording() {
        Log.v(recorderTag, "Stopping recording")
        recording?.close()
        recording = null

        cameraData = cameraData.copy(videoCapture = null)
    }

    private fun getVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        return VideoCapture.withOutput(recorder)
    }

    @SuppressLint("MissingPermission")
    private fun VideoCapture<Recorder>.setMediaSession(context: Context, file: File) {
        val fileOutputOptions = FileOutputOptions.Builder(file)
            .setFileSizeLimit(fileSizeLimitMB.toLong() * 1024L * 1024L)
            .setDurationLimitMillis(durationLimit.inWholeMilliseconds)
            .build()
        recording = output.prepareRecording(context, fileOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context), stateKeeper())
    }

    private fun stateKeeper(): Consumer<VideoRecordEvent> {
        val recordingListener = Consumer<VideoRecordEvent> { event ->
            Log.v(recorderTag, "Event: $event")
            when (event) {
                is VideoRecordEvent.Start -> recorderMutableStateFlow.value = CameraState.Active
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        // update app internal state
                        Log.v(recorderTag, "Video capture succeeded: ${event.outputResults.outputUri}")
                        recorderMutableStateFlow.value = CameraState.Idle
                    } else {
                        Log.v(recorderTag, "Video capture failed: ${event.error}")
                        recorderMutableStateFlow.value = CameraState.Error("Video capture failed: ${event.error}")
                        // update app state when the capture failed.
                        recording?.close()
                        recording = null
                    }
                }
                is VideoRecordEvent.Status -> {
                    recorderStatsMutableStateFlow.value = event.recordingStats
                }
                else -> Log.v(recorderTag, "Unhandled event: $event")
            }
        }

        return recordingListener
    }

    companion object {
        val previewCameraData = CameraData(CameraSelector.LENS_FACING_FRONT, null, Preview.Builder().build())
    }
}

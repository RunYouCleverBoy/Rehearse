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
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.hours

sealed class CameraState {
    data object Idle : CameraState()
    data object Starting : CameraState()
    data object Active : CameraState()
    data object Stopping : CameraState()
    data class Error(val message: String) : CameraState()
}

sealed class CameraUseCase {
    data class Preview(val preview: androidx.camera.core.Preview) : CameraUseCase()
    data class Recorder(val cameraRecorder: CameraRecorder, val preview: androidx.camera.core.Preview) : CameraUseCase()

}

@Composable
fun CameraPreviewScreen(stopping: State<Boolean>, cameraUseCase: CameraUseCase) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context)
    }
    val isStopping by stopping

    val preview: Preview by remember { derivedStateOf {
        when (cameraUseCase) {
            is CameraUseCase.Preview -> cameraUseCase.preview
            is CameraUseCase.Recorder -> cameraUseCase.preview
        }
    } }

    LaunchedEffect(cameraUseCase) {
        val lensFacing = CameraSelector.LENS_FACING_FRONT
        setupCamera(lensFacing, context, cameraUseCase, lifecycleOwner, preview)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val alpha: Float by animateFloatAsState(if (isStopping) 0.9f else 0f, label = "alpha", animationSpec = tween(800))
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (isStopping) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
                    .background(Color.White)
            )
        }
    }
}

class CameraRecorder {
    private var recording: Recording? = null
    private val recorderTag = "CameraRecorder"
    private val recorderMutableStateFlow = MutableStateFlow<CameraState>(CameraState.Idle)
    val recorderState: StateFlow<CameraState> = recorderMutableStateFlow
    val previewUseCase = Preview.Builder().build()

    private val Number.megabytes: Long get() = this.toLong() * 1024 * 1024
    private val fileSizeLimit = 512.megabytes
    private val durationLimit = 1.hours

    private val qualitySelector = QualitySelector.from(
        Quality.UHD,
        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
    )

    private suspend fun setupCamera(
        lensFacing: Int,
        context: Context,
        cameraUseCase: CameraUseCase,
        lifecycleOwner: LifecycleOwner,
        preview: Preview
    ) {
        val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }

        cameraProvider.unbindAll()
        val videoCapture = CameraRecorder().getVideoCapture()
        when (cameraUseCase) {
            is CameraUseCase.Recorder -> cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, videoCapture, preview)
            is CameraUseCase.Preview -> cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview)
        }
    }

    private fun getVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        return VideoCapture.withOutput(recorder)
    }

    @SuppressLint("MissingPermission")
    fun addRecorderMediaToVideoCapture(videoCapture: VideoCapture<Recorder>, context: Context, file: File) {
        val fileOutputOptions = FileOutputOptions.Builder(file)
            .setFileSizeLimit(fileSizeLimit)
            .setDurationLimitMillis(durationLimit.inWholeMilliseconds)
            .build()
        recording = videoCapture.output.prepareRecording(context, fileOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context), stateKeeper())
    }

    private fun stateKeeper(): Consumer<VideoRecordEvent> {
        val recordingListener = Consumer<VideoRecordEvent> { event ->
            when (event) {
                is VideoRecordEvent.Start -> recorderMutableStateFlow.value = CameraState.Starting
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

                else -> Log.v(recorderTag, "Unhandled event: $event")
            }
        }

        return recordingListener
    }
}

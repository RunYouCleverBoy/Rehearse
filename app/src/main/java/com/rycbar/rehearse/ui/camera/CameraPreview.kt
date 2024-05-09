package com.rycbar.rehearse.ui.camera

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
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
import com.rycbar.rehearse.repos.camera.CameraRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
package com.playgrounds.mirrormirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.playgrounds.mirrormirror.ui.theme.MirrorMirrorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    var onPermissionEvent = { _: Collection<String> -> }
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys
            onPermissionEvent(deniedPermissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel by viewModels<MirrorViewModel>()
        setContent {
            LaunchedEffect(Unit) {
                onPermissionEvent = { permissions ->
                    viewModel.dispatchEvent(MainEvent.PermissionsMissing(permissions.toList()))
                }

                launch {
                    delay(100.milliseconds)
                    viewModel.dispatchEvent(MainEvent.AppStarted)
                }

                viewModel.actions.collect { action ->
                    when (action) {
                        is MainAction.RequestPermissions -> checkPermissions(action.permissions)
                    }
                }
            }

            MirrorMirrorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by viewModel.state.collectAsState()
                    MirrorMirrorScreen(state, viewModel::dispatchEvent)
                }
            }
        }
    }

    private fun checkPermissions(permissions: Array<String>) {
        if (permissions.all { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            onPermissionEvent(emptyList())
        } else {
            activityResultLauncher.launch(permissions)
        }
    }
}


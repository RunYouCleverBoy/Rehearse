package com.playgrounds.mirrormirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playgrounds.mirrormirror.ui.MirrorMirrorScreen
import com.playgrounds.mirrormirror.ui.theme.MirrorMirrorTheme
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var onPermissionEvent = { _: Collection<String> -> }
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys
            onPermissionEvent(deniedPermissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<MirrorViewModel>()
            LaunchedEffect(Unit) {
                onPermissionEvent = { permissions ->
                    viewModel.dispatchEvent(MainEvent.PermissionsMissing(permissions.toList()))
                }

                launch {
                    viewModel.actions.filterIsInstance<MainAction.RequestPermissions>().collect{
                        checkPermissions(it.permissions)
                    }
                }

                viewModel.dispatchEvent(MainEvent.AppStarted)
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


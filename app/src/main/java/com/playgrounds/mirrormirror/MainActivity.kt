package com.playgrounds.mirrormirror

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
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
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playgrounds.mirrormirror.ui.MirrorMirrorScreen
import com.playgrounds.mirrormirror.ui.theme.MirrorMirrorTheme
import kotlinx.coroutines.flow.filterIsInstance

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

                viewModel.dispatchEvent(MainEvent.AppStarted)
                viewModel.actions.filterIsInstance<MainAction.RequestPermissions>().collect{
                    checkPermissions(it.permissions)
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

        hideSystemUI()
    }

    private fun checkPermissions(permissions: Array<String>) {
        if (permissions.all { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
            onPermissionEvent(emptyList())
        } else {
            activityResultLauncher.launch(permissions)
        }
    }

    private fun hideSystemUI() {
        //Hides the ugly action bar at the top
        actionBar?.hide()

        //Hide the status bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.insetsController?.apply {
                hide(WindowInsets.Type.navigationBars())
                hide(WindowInsets.Type.statusBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

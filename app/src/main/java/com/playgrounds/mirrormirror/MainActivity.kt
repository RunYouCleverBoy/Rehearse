package com.playgrounds.mirrormirror

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playgrounds.mirrormirror.repos.permission.PermissionHandler
import com.playgrounds.mirrormirror.ui.main.MainAction
import com.playgrounds.mirrormirror.ui.main.MainEvent
import com.playgrounds.mirrormirror.ui.main.MirrorMirrorScreen
import com.playgrounds.mirrormirror.ui.main.MirrorViewModel
import com.playgrounds.mirrormirror.ui.theme.MirrorMirrorTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filterIsInstance

class MainActivity : ComponentActivity() {
    private val permissionHandler = PermissionHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<MirrorViewModel>()
            var showingDialog by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
            if (showingDialog != null) {
                AlertDialog(
                    text = { stringResource(id = R.string.permission_rationale) }, onDismissRequest = { showingDialog?.complete(false) },
                    confirmButton = { showingDialog?.complete(true) })
            }
            LaunchedEffect(Unit) {
                viewModel.actions.filterIsInstance<MainAction.RequestPermissions>().collect { requiredAction ->
                    when (requiredAction) {
                        MainAction.RequestPermissions.GoToSettings -> {
                            // TODO: Do it better and nicer
                            permissionHandler.goToSettings()
                        }
                        MainAction.RequestPermissions.WithRationale -> {
                            showingDialog = CompletableDeferred()
                            showingDialog?.await()
                            showingDialog = null
                        }

                        MainAction.RequestPermissions.WithoutRationale -> Unit
                    }

                    val remedy = permissionHandler.requestMissingPermissions().await()
                    viewModel.dispatchEvent(MainEvent.PermissionsMissing(remedy))
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

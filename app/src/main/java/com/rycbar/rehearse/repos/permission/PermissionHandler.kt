package com.rycbar.rehearse.repos.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.rycbar.rehearse.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class PermissionHandler(private val componentActivity: ComponentActivity) {
    private var onPermissionEvent = { _: Collection<String> -> }
    private val activityResultLauncher =
        componentActivity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            onPermissionResponse(permissions)
        }

    sealed class Remedy {
        data object AllGranted : Remedy()
        data class CanRequest(val deniedPermissions: Set<String>) : Remedy()
        data class ShouldShowRationale(val deniedPermissions: Set<String>) : Remedy()
        data class ShouldGoToSettings(val deniedPermissions: Set<String>) : Remedy()
    }

    fun requestMissingPermissions(): Deferred<Remedy> {
        val remedyDeferred = CompletableDeferred<Remedy>()
        val requestPermissions = missingPermissions()
        if (requestPermissions.isEmpty()) {
            remedyDeferred.complete(Remedy.AllGranted)
        } else {
            onPermissionEvent = generateDeniedPermissionHandler(requestPermissions, remedyDeferred)
            activityResultLauncher.launch(requestPermissions.toTypedArray())
        }

        return remedyDeferred
    }

    fun missingPermissions(): Set<String> {
        val requestPermissions = REQUIRED_PERMISSIONS.filter { componentActivity.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toSet()
        return requestPermissions
    }

    fun goToSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", componentActivity.packageName, null)
            data = uri
        }
        componentActivity.startActivity(intent)
    }

    @Composable
    fun ShowPermissionRationaleDialog(onDismiss: () -> Unit) {
        AlertDialog(
            text = {
                Text(
                    text = stringResource(id = R.string.permission_rationale)
                )
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(text = stringResource(R.string.ok))
                }
            })
    }

    private fun generateDeniedPermissionHandler(
        requestPermissions: Set<String>,
        missingPermissions: CompletableDeferred<Remedy>
    ): (Collection<String>) -> Unit = {
        val showRationale = requestPermissions.any { componentActivity.shouldShowRequestPermissionRationale(it) }
        val shouldGoToSettings = !recommendedSettingsOnce &&
                requestPermissions.any {
                    !ActivityCompat.shouldShowRequestPermissionRationale(componentActivity, it) &&
                            componentActivity.checkSelfPermission(it) == PackageManager.PERMISSION_DENIED
                }

        val remedy = when {
            showRationale -> Remedy.ShouldShowRationale(requestPermissions)
            shouldGoToSettings -> Remedy.ShouldGoToSettings(requestPermissions)
            else -> Remedy.CanRequest(requestPermissions)
        }

        recommendedSettingsOnce = recommendedSettingsOnce || remedy is Remedy.ShouldGoToSettings

        missingPermissions.complete(remedy)
    }

    private fun onPermissionResponse(permissions: Map<String, @JvmSuppressWildcards Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys
        onPermissionEvent(deniedPermissions)
    }

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        private var recommendedSettingsOnce = false
    }
}
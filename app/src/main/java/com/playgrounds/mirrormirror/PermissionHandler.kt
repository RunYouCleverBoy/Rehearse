package com.playgrounds.mirrormirror

import android.Manifest

class PermissionHandler {
    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

}
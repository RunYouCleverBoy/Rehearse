package com.rycbar.rehearse.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rycbar.rehearse.repos.permission.PermissionHandler
import com.rycbar.rehearse.ui.Screens
import com.rycbar.rehearse.ui.camera.ViewFinderSubScreen
import com.rycbar.rehearse.ui.main.mvi.MainEvent
import com.rycbar.rehearse.ui.main.mvi.RehearseState
import com.rycbar.rehearse.ui.onboarding.WelcomeScreen
import com.rycbar.rehearse.ui.player.ReplayScreen

@Composable
fun NavGraph(
    modifier: Modifier,
    navController: NavHostController,
    permissionHandler: PermissionHandler,
    onSendEvent: (MainEvent) -> Unit,
    state: RehearseState
) {
    NavHost(modifier = modifier, navController = navController, startDestination = Screens.Permission.path) {
        composable(Screens.Permission.path) {
            WelcomeScreen(permissionHandler) { event -> onSendEvent(event) }
        }
        composable(Screens.Viewfinder.path) {
            ViewFinderSubScreen(
                modifier = Modifier.fillMaxSize(), state = state,
                onEvent = onSendEvent
            )
        }
        composable(Screens.Replay.path) {
            ReplayScreen(state.lastRecordingFile)
        }
    }
}
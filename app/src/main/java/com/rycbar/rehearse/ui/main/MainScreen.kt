package com.rycbar.rehearse.ui.main

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.rycbar.rehearse.repos.permission.PermissionHandler
import com.rycbar.rehearse.ui.Screens
import com.rycbar.rehearse.ui.main.mvi.MainAction
import com.rycbar.rehearse.ui.main.mvi.MainEvent
import com.rycbar.rehearse.ui.main.mvi.RehearseState
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun MainScreen(permissionHandler: PermissionHandler, state: RehearseState, onSendEvent: (MainEvent) -> Unit) {
    val contentVerticalPadding by remember(state.tabsOpacity) { derivedStateOf { stateByOpacity(state.tabsOpacity) } }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ContentScreen(permissionHandler,
            Modifier
                .fillMaxWidth()
                .padding(vertical = contentVerticalPadding), onSendEvent, state
        )
        if (state.tabsOpacity > 0f) {
            MainTabRow(state, onSendEvent)
        }
    }
}

private fun stateByOpacity(opacity: Float) = if (opacity < 1f) 0.dp else 40.dp

@Composable
private fun ContentScreen(permissionHandler: PermissionHandler, modifier: Modifier, onSendEvent: (MainEvent) -> Unit, state: RehearseState) {
    val viewModel = viewModel<MainViewModel>()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        onSendEvent(MainEvent.BackStackEntryChanged(backStackEntry ?: return@LaunchedEffect))
        Log.v("BackStackEntry", "Changed to: ${backStackEntry?.destination?.route}")
    }

    LaunchedEffect(Unit) {
        navController.enableOnBackPressed(false)
        viewModel.actions.filterIsInstance<MainAction.NavigateTo>().collect {
            val currentDestination = navController.currentDestination
            val route = currentDestination?.route ?: Screens.Viewfinder.path
            navController.clearBackStack(route)
            navController.navigate(
                route = it.destination.path,
                navOptions = navOptions {
                    launchSingleTop = true
                    popUpTo(
                        route = route,
                        popUpToBuilder = { inclusive = true })
                })
        }
    }

    NavGraph(modifier, navController, permissionHandler, onSendEvent, state)
}


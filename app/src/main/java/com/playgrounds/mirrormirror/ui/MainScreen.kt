package com.playgrounds.mirrormirror.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.playgrounds.mirrormirror.MainAction
import com.playgrounds.mirrormirror.MainEvent
import com.playgrounds.mirrormirror.MirrorState
import com.playgrounds.mirrormirror.MirrorViewModel
import com.playgrounds.mirrormirror.ui.components.ReplayScreen
import com.playgrounds.mirrormirror.ui.components.ViewFinderSubScreen
import kotlinx.coroutines.flow.filterIsInstance

@Composable
fun MirrorMirrorScreen(state: MirrorState, onSendEvent: (MainEvent) -> Unit) {
    val contentVerticalPadding by remember(state.tabsOpacity) { derivedStateOf { stateByOpacity(state.tabsOpacity) } }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ContentScreen(
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
private fun ContentScreen(modifier: Modifier, onSendEvent: (MainEvent) -> Unit, state: MirrorState) {
    val viewModel = viewModel<MirrorViewModel>()
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

    NavHost(modifier = modifier, navController = navController, startDestination = Screens.Welcome.path) {
        composable(Screens.Welcome.path) {
            WelcomeScreen { event -> onSendEvent(event) }
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

@Composable
private fun MainTabRow(state: MirrorState, onSendEvent: (MainEvent) -> Unit) {
    TabRow(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(state.tabsOpacity),
        selectedTabIndex = state.selectedTabIndex
    ) {
        val context = LocalContext.current
        state.tabs.forEachIndexed { index, tabInfo ->
            LeadingIconTab(
                text = { Text(text = stringResource(id = tabInfo.title)) },
                icon = {
                    Icon(painter = painterResource(id = tabInfo.companionIcon), contentDescription = stringResource(id = tabInfo.title))
                },
                selected = state.selectedTabIndex == index,
                onClick = { onSendEvent(MainEvent.TabSelected(context, index)) }
            )
        }
    }
}

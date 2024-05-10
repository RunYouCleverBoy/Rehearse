package com.rycbar.rehearse.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rycbar.rehearse.ui.main.mvi.MainEvent
import com.rycbar.rehearse.ui.main.mvi.RehearseState

@Composable
fun MainTabRow(state: RehearseState, onSendEvent: (MainEvent) -> Unit) {
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
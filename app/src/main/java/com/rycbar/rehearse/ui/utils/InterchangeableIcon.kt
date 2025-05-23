package com.rycbar.rehearse.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.rycbar.rehearse.ui.main.IconAndText

@Composable
fun InterchangeableIcon(state: IconAndText, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = state.enabled) {
        val primary = colorScheme.primary
        Image(painter = painterResource(id = state.icon), contentDescription = stringResource(id = state.text), colorFilter = ColorFilter.tint(primary))
    }
}
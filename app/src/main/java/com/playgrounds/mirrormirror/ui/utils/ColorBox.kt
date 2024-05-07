package com.playgrounds.mirrormirror.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ColorBox(color: Color, text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color), contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}
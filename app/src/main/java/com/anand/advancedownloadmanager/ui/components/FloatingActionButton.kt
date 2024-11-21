package com.anand.advancedownloadmanager.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.anand.advancedownloadmanager.models.File

@Composable
fun FloatingActionButton(
    isScrolling: Boolean,
    fabHeight: Dp,
    onCLick: () -> Unit
) {
    ExtendedFloatingActionButton(
        modifier = Modifier.height(fabHeight),
        expanded = isScrolling.not(),
        onClick = onCLick,
        icon = { Icon(Icons.Filled.Add, "New Download") },
        text = { Text(text = "Download") },
    )
}
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
    onCLick: (file: File) -> Unit
) {
    ExtendedFloatingActionButton(
        modifier = Modifier.height(fabHeight),
        expanded = isScrolling.not(),
        onClick = {
            onCLick(
                File(
                    name = buildString {
                        append("test_file_")
                        append(System.currentTimeMillis())
                    },
                    url = listOf(
                        "https://filesampleshub.com/download/video/mp4/sample3.mp4",
                        "https://videos.pexels.com/video-files/10189089/10189089-hd_1920_1080_25fps.mp4",
                        "https://www.learningcontainer.com/wp-content/uploads/2019/09/sample-pdf-download-10-mb.pdf",
                        "https://tourism.gov.in/sites/default/files/2019-04/dummy-pdf_2.pdf"
                    ).random()
                )
            )
        },
        icon = { Icon(Icons.Filled.Add, "New Download") },
        text = { Text(text = "Download") },
    )
}
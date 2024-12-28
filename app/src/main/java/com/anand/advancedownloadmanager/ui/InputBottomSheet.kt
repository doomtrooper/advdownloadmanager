package com.anand.advancedownloadmanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anand.advancedownloadmanager.models.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputBottomSheet(bottomSheetToggle: (state: Boolean) -> Unit, onCLick: (file: File) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = {
            bottomSheetToggle(false)
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp).imePadding()
        ) {
            var fileDownloadUrl by remember { mutableStateOf(listOf(
                "https://sample-videos.com/video321/mp4/240/big_buck_bunny_240p_30mb.mp4",
                "https://filesampleshub.com/download/video/mp4/sample3.mp4",
                "https://videos.pexels.com/video-files/10189089/10189089-hd_1920_1080_25fps.mp4",
                "https://www.learningcontainer.com/wp-content/uploads/2019/09/sample-pdf-download-10-mb.pdf",
                "https://tourism.gov.in/sites/default/files/2019-04/dummy-pdf_2.pdf"
            ).random()) }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = fileDownloadUrl,
                onValueChange = { fileDownloadUrl = it },
                label = { Text(color = Color.Gray, text = "url") }
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
            var fileName by remember { mutableStateOf(buildString {
                append("test_file_")
                append(System.currentTimeMillis())
            }) }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text(color = Color.Gray, text = "file name") }
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
            FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    onCLick(
                        File(
                            name = fileName,
                            url = fileDownloadUrl
                        )
                    )
                    bottomSheetToggle(false)
                }
            }) {
                Text("Download")
            }

        }
    }
}
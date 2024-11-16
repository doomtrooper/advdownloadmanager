package com.anand.advancedownloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.work.WorkManager
import com.anand.advancedownloadmanager.ui.theme.AdvanceDownloadManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val admViewModel by viewModels<AdmViewModel>() {
            AdmViewModelFactory(
                WorkManager.getInstance(
                    this
                )
            )
        }
        setContent {
            AdvanceDownloadManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            admViewModel.startDownloadingFile(
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
                }) { innerPadding ->
                    val homeUiStateState by admViewModel.uiStateFlow.collectAsState()
                    HomePage(homeUiStateState, innerPadding)
                }
            }
        }
    }
}

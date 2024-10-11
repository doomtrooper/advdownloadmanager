package com.anand.advancedownloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
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
                    Home(admViewModel.uiStateFlow.collectAsState().value, innerPadding)
                }
            }
        }
    }

    @Composable
    private fun Home(homeUiState: AdmHomeUiState, innerPadding: PaddingValues) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn {
                items(homeUiState.files.size) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(text = homeUiState.files[it].name)
                            Text(text = homeUiState.files[it].status.name)
                            Text(text = homeUiState.files[it].progress.toString())
                        }
                    }
                }
            }
        }
    }
}

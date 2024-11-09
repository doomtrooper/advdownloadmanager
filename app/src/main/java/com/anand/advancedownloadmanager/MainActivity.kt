package com.anand.advancedownloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkManager
import com.anand.advancedownloadmanager.ui.theme.AdvanceDownloadManagerTheme
import com.anand.advancedownloadmanager.ui.theme.CircularProgressIndicatorSegment
import com.anand.advancedownloadmanager.ui.theme.SegmentedCircularProgressIndicator

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
//                                        "https://videos.pexels.com/video-files/10189089/10189089-hd_1920_1080_25fps.mp4",
//                                        "https://www.learningcontainer.com/wp-content/uploads/2019/09/sample-pdf-download-10-mb.pdf",
//                                        "https://tourism.gov.in/sites/default/files/2019-04/dummy-pdf_2.pdf"
                                    ).random()
                                )
                            )
                        },
                        icon = { Icon(Icons.Filled.Add, "New Download") },
                        text = { Text(text = "Download") },
                    )
                }) { innerPadding ->
                    val homeUiStateState by admViewModel.uiStateFlow.collectAsState()
                    Home(homeUiStateState, innerPadding)
                }
            }
        }
    }

    @Composable
    private fun Home(homeUiState: AdmHomeUiState, innerPadding: PaddingValues) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(innerPadding)
        ) {
            LazyColumn(modifier = Modifier.padding(10.dp)) {
                item {
                    val gradientColors = listOf(Cyan, Blue, Color.Magenta)
                    Text(
                        text = "Welcome to Advance Download manager",
                        style = TextStyle(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            fontFamily = FontFamily.SansSerif,
                            brush = Brush.linearGradient(
                                colors = gradientColors
                            )
                        )
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth()
                    )
                }
                items(homeUiState.files.size) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ElevatedCard(
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 6.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = Gray,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(10.dp)) {
                                Row {
                                    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
                                        Text(
                                            text = homeUiState.files[it].name,
                                            color = White,
                                            fontSize = 20.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            lineHeight = 20.sp
                                        )
                                        Text(
                                            text = homeUiState.files[it].status.name,
                                            color = White,
                                            fontSize = 16.sp,
                                            lineHeight = 20.sp,
                                            fontFamily = FontFamily.SansSerif,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(150.dp)
                                            .border(BorderStroke(.5.dp, Color.Red)),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (homeUiState.files[it].progress.isNotEmpty()) {
                                            SegmentedCircularProgressIndicator(
                                                modifier = Modifier.fillMaxSize(),
                                                segments = homeUiState.files[it].progress.map { progress ->
                                                    CircularProgressIndicatorSegment(
                                                        segment = progress.partWeight,
                                                        segmentProgress = progress.progress,
                                                        segmentIndex = progress.partIndex
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

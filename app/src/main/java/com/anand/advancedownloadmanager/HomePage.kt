package com.anand.advancedownloadmanager

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomePage(homeUiState: AdmHomeUiState, innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(innerPadding)
    ) {
        LazyColumn(modifier = Modifier.padding(10.dp)) {
            item {
                val gradientColors = listOf(Color.Cyan, Color.Blue, Color.Magenta)
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
                            containerColor = Color.Gray,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, Color.Black))
                                    .padding(1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .border(BorderStroke(1.dp, Color.White))
                                ) {
                                    Text(
                                        text = homeUiState.files[it].name,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        lineHeight = 20.sp
                                    )
                                    Text(
                                        text = homeUiState.files[it].status.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        fontFamily = FontFamily.SansSerif,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .border(BorderStroke(.5.dp, Color.Red))
                                ) {
                                    if (homeUiState.files[it].progress.isNotEmpty()) {
                                        SegmentedCircularProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(5.dp),
                                            segments = homeUiState.files[it].progress.map { progress ->
                                                CircularProgressIndicatorSegment(
                                                    segment = progress.partWeight,
                                                    segmentProgress = progress.progress,
                                                    segmentIndex = progress.partIndex,
                                                    index = it
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
package com.anand.advancedownloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.anand.advancedownloadmanager.ui.theme.AdvanceDownloadManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdvanceDownloadManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            startDownloadingFile(
                                File(
                                    id = "10",
                                    name = "test vid",
//                                    url = "https://videos.pexels.com/video-files/10189089/10189089-hd_1920_1080_25fps.mp4",
                                    url = "https://www.learningcontainer.com/wp-content/uploads/2019/09/sample-pdf-download-10-mb.pdf",
//                                    url = "https://tourism.gov.in/sites/default/files/2019-04/dummy-pdf_2.pdf",
                                )
                            )
                        },
                        icon = { Icon(Icons.Filled.Add, "New Download") },
                        text = { Text(text = "Download") },
                    )
                }) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {

                    }
                }
            }
        }
    }


    private fun startDownloadingFile(
        file: File,
    ) {
        val data = Data.Builder()

        data.apply {
            putString(FileParams.KEY_FILE_NAME, file.name)
            putString(FileParams.KEY_FILE_URL, file.url)
        }

        val constraints = Constraints.Builder()
            .build()

        val fileDownloadWorker = OneTimeWorkRequestBuilder<FileDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(data.build())
            .build()

        WorkManager
            .getInstance(this)
            .enqueueUniqueWork(
                "oneFileDownloadWork_${System.currentTimeMillis()}",
                ExistingWorkPolicy.KEEP,
                fileDownloadWorker
            )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AdvanceDownloadManagerTheme {
        Greeting("Android")
    }
}
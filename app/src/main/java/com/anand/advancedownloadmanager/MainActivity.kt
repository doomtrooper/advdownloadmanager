package com.anand.advancedownloadmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.work.WorkManager
import com.anand.advancedownloadmanager.ui.HomePage
import com.anand.advancedownloadmanager.ui.components.FloatingActionButton
import com.anand.advancedownloadmanager.ui.theme.AdvanceDownloadManagerTheme
import com.anand.advancedownloadmanager.utils.EXTENDED_FAB_HEIGHT
import com.anand.advancedownloadmanager.viewmodels.AdmViewModel
import com.anand.advancedownloadmanager.viewmodels.AdmViewModelFactory


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val admViewModel by viewModels<AdmViewModel>() {
            AdmViewModelFactory(WorkManager.getInstance(this))
        }
        setContent {
            val listState = rememberLazyListState()
            val isScrolling by remember { derivedStateOf { listState.isScrollInProgress && listState.firstVisibleItemIndex != 1 } }
            AdvanceDownloadManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
                    FloatingActionButton(isScrolling, admViewModel, EXTENDED_FAB_HEIGHT)
                }) { innerPadding ->
                    val homeUiStateState by admViewModel.uiStateFlow.collectAsState()
                    HomePage(homeUiStateState, innerPadding, EXTENDED_FAB_HEIGHT, listState)
                }
            }
        }
    }
}

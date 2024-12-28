package com.anand.advancedownloadmanager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.anand.advancedownloadmanager.models.File
import com.anand.advancedownloadmanager.models.FileDownloadUpdate
import com.anand.advancedownloadmanager.redux.AdmStore
import com.anand.advancedownloadmanager.viewmodels.reducers.FileDownloadUpdatePartCountReducer
import com.anand.advancedownloadmanager.viewmodels.reducers.FileDownloadUpdateProgressReducer
import com.anand.advancedownloadmanager.viewmodels.reducers.FileDownloadUpdateUnSupportedReducer
import com.anand.advancedownloadmanager.redux.IReducer
import com.anand.advancedownloadmanager.utils.FileParams
import com.anand.advancedownloadmanager.utils.FileUtils
import com.anand.advancedownloadmanager.workers.FileDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AdmViewModel(private val workManager: WorkManager) : ViewModel() {
    private var uiState = MutableStateFlow(AdmHomeUiState())
    private var reducers: List<IReducer<AdmHomeUiState, FileDownloadUpdate>> = listOf(
        FileDownloadUpdatePartCountReducer,
        FileDownloadUpdateProgressReducer,
        FileDownloadUpdateUnSupportedReducer
    )

    private val store: AdmStore = AdmStore(uiState, reducers)
    var uiStateFlow = uiState.asStateFlow()

    fun startDownloadingFile(file: File) {
        viewModelScope.launch {
            val data = Data.Builder().apply {
                putString(FileParams.KEY_FILE_NAME, file.name)
                putString(FileParams.KEY_FILE_URL, file.url)
            }
            val constraints = Constraints.Builder().build()
            val fileDownloadWorker = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                .setConstraints(constraints)
                .setInputData(data.build())
                .build()
            workManager.enqueueUniqueWork(
                "oneFileDownloadWork_${file.name}",
                ExistingWorkPolicy.REPLACE,
                fileDownloadWorker
            )
            uiState.update {
                uiState.value.copy(
                    files = uiState.value.files
                        .toMutableList()
                        .apply { add(file.copy(workerId = fileDownloadWorker.id)) }
                )
            }
            UUID.fromString(fileDownloadWorker.id.toString())
            workManager.getWorkInfoByIdFlow(fileDownloadWorker.id).collect { workInfo: WorkInfo ->
                println("[ADM-VM] $workInfo")
                store.dispatch(FileUtils.adapter(workInfo.progress.keyValueMap, workInfo))
            }
        }
    }
}

class AdmViewModelFactory(private val workManager: WorkManager) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AdmViewModel(workManager) as T
    }
}

data class AdmHomeUiState(
    val files: List<File> = emptyList()
)

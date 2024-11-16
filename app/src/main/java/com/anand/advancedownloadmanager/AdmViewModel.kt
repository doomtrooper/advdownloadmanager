package com.anand.advancedownloadmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AdmViewModel(private val workManager: WorkManager) : ViewModel() {
    private var uiState = MutableStateFlow(AdmHomeUiState())
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
                uiState.update {
                    val fileDownloadUpdate =
                        FileUtils.adapter(workInfo.progress.keyValueMap, workInfo)
                    when (fileDownloadUpdate) {
                        is FileDownloadUpdatePartCount -> {
                            val filteredList = uiState.value.files.toMutableList()
                                .filter { it.workerId != fileDownloadUpdate.workerId }
                            val matchedFile = uiState.value.files
                                .find { it.workerId == fileDownloadUpdate.workerId }
                                ?.copy(partCount = fileDownloadUpdate.totalParts)
                            val files = buildList {
                                addAll(filteredList)
                                matchedFile?.let { add(it) }
                            }
                            return@update uiState.value.copy(
                                files = files.sortedBy { it.startTime }
                            )
                        }

                        is FileDownloadUpdateProgress -> {
                            val filteredList = uiState.value.files
                                .toMutableList()
                                .filter { it.workerId != fileDownloadUpdate.workerId }
                            val matchedFile = uiState.value.files
                                .find { it.workerId == fileDownloadUpdate.workerId }
                                ?.let {
                                    val fileDownloadProgress = FileDownloadProgress(
                                        fileDownloadUpdate.filePartIndex,
                                        fileDownloadUpdate.percentCompleted,
                                        fileDownloadUpdate.weight
                                    )
                                    val filterProgress = it.progress.toList()
                                        .filter { p -> p.partIndex != fileDownloadProgress.partIndex }
//                                    it.progress.toList().find { p -> p.partIndex == fileDownloadProgress.partIndex }?.copy(progress = fileDownloadUpdate.percentCompleted>p)
                                    val buildList = buildList {
                                        addAll(filterProgress)
                                        add(fileDownloadProgress)
                                    }
                                    it.copy(progress = buildList)
                                }
                            val files = buildList {
                                addAll(filteredList)
                                matchedFile?.let {
                                    add(it)
                                }
                            }
                            uiState.value.copy(
                                files = files.sortedBy { it.startTime }
                            )
                        }

                        is FileDownloadUpdateUnSupported -> {
                            val filteredList = uiState.value.files
                                .toMutableList()
                                .filter { it.workerId != fileDownloadUpdate.workerId }
                            val matchedFile = uiState.value.files
                                .find { it.workerId == fileDownloadUpdate.workerId }
                                ?.copy(status = fileDownloadUpdate.state)
                            val files = buildList {
                                addAll(filteredList)
                                matchedFile?.let { add(it) }
                            }
                            return@update uiState.value.copy(
                                files = files.sortedBy { it.startTime }
                            )
                        }
                    }
                }
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

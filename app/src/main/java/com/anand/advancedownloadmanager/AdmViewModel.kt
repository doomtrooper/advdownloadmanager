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
            val operation = workManager.enqueueUniqueWork(
                "oneFileDownloadWork_${file.name}",
                ExistingWorkPolicy.REPLACE,
                fileDownloadWorker
            )
            uiState.update {
                uiState.value.copy(
                    files = buildList {
                        addAll(uiState.value.files)
                        add(file)
                    }
                )
            }
            workManager.getWorkInfoByIdFlow(fileDownloadWorker.id).collect { workInfo: WorkInfo ->
                uiState.update {
                    println(workInfo)
                    val fileDownloadUpdate = FileUtils.adapter(workInfo.progress.keyValueMap)
                    println(fileDownloadUpdate)
                    when (fileDownloadUpdate) {
                        is FileDownloadUpdatePartCount -> {
                            return@update uiState.value.copy(
                                files = uiState.value.files.toMutableList().apply {
                                    forEach { file: File ->
                                        run {
                                            if (file.url == fileDownloadUpdate.fileUrl) {
                                                file.partCount = fileDownloadUpdate.totalParts
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        is FileDownloadUpdateProgress -> {
                            return@update uiState.value.copy(
                                files = uiState.value.files.toMutableList().apply {
                                    forEach { file: File ->
                                        run {
                                            if (file.url == fileDownloadUpdate.fileUrl) {
                                                val fileDownloadProgress = FileDownloadProgress(
                                                    fileDownloadUpdate.filePartIndex,
                                                    fileDownloadUpdate.percentCompleted
                                                )
                                                file.progress?.run {
                                                    add(
                                                        fileDownloadProgress
                                                    )
                                                } ?: run {
                                                    file.progress =
                                                        mutableSetOf(fileDownloadProgress)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        FileDownloadUpdateUnSupported -> return@update uiState.value.copy(
                            files = buildList {
                                addAll(uiState.value.files.filter { it.name != file.name })
                                add(
                                    file.copy(
                                        status = workInfo.state,
                                    )
                                )
                            }
                        )
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

data class AdmHomeUiState(val files: List<File> = listOf())
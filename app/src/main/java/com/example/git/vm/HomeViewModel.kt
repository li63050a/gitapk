package com.example.git.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.git.git.BatchOperationResult
import com.example.git.git.GitExecutor
import com.example.git.git.RepoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val repos: List<RepoInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val batchResults: List<BatchOperationResult> = emptyList(),
    val isBatchRunning: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadRepos(scanPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val repos = scanRepositories(scanPath)
                _uiState.value = _uiState.value.copy(repos = repos, isLoading = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun scanRepositories(path: String): List<RepoInfo> {
        val repos = mutableListOf<RepoInfo>()
        val dir = java.io.File(path)
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && java.io.File(file, ".git").exists()) {
                    repos.add(RepoInfo(path = file.absolutePath, name = file.name, lastModified = file.lastModified()))
                }
            }
        }
        return repos.sortedByDescending { it.lastModified }
    }

    fun cloneRepository(url: String, localPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.cloneRepository(url, localPath)
            if (result.success) loadRepos(localPath.substringBeforeLast("/"))
            else _uiState.value = _uiState.value.copy(error = result.error)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun initRepository(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.initRepository(path)
            if (result.success) loadRepos(path.substringBeforeLast("/"))
            else _uiState.value = _uiState.value.copy(error = result.error)
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun batchPull(paths: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchRunning = true)
            val result = gitExecutor.batchPull(paths)
            if (result.success) {
                _uiState.value = _uiState.value.copy(batchResults = result.data ?: emptyList(), isBatchRunning = false)
            } else {
                _uiState.value = _uiState.value.copy(error = result.error, isBatchRunning = false)
            }
        }
    }

    fun batchPush(paths: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchRunning = true)
            val result = gitExecutor.batchPush(paths)
            if (result.success) {
                _uiState.value = _uiState.value.copy(batchResults = result.data ?: emptyList(), isBatchRunning = false)
            } else {
                _uiState.value = _uiState.value.copy(error = result.error, isBatchRunning = false)
            }
        }
    }

    fun batchFetch(paths: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBatchRunning = true)
            val result = gitExecutor.batchFetch(paths)
            if (result.success) {
                _uiState.value = _uiState.value.copy(batchResults = result.data ?: emptyList(), isBatchRunning = false)
            } else {
                _uiState.value = _uiState.value.copy(error = result.error, isBatchRunning = false)
            }
        }
    }
}

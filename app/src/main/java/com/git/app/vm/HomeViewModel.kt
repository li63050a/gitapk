package com.git.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.data.RepoPathsStore
import com.git.app.git.BatchOperationResult
import com.git.app.git.GitExecutor
import com.git.app.git.RepoInfo
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadRepos(scanPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val roots = mutableListOf(scanPath)
                getApplication<Application>().getExternalFilesDir(null)?.absolutePath
                    ?.let { roots.add(it) }
                val repos = roots.flatMap { scanRepositories(it) }
                    .associateBy { it.path }.toMutableMap()
                // Merge previously added repo paths so they always show up, even when
                // the public storage root cannot be listed without "All files access".
                val persisted = RepoPathsStore.getPaths(getApplication())
                for (p in persisted) {
                    if (!repos.containsKey(p)) {
                        val f = java.io.File(p)
                        if (f.isDirectory) {
                            repos[p] = RepoInfo(
                                path = p,
                                name = f.name,
                                lastModified = f.lastModified()
                            )
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    repos = repos.values.sortedByDescending { it.lastModified },
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun registerRepo(path: String) {
        viewModelScope.launch {
            RepoPathsStore.addPath(getApplication(), path)
        }
        val f = java.io.File(path)
        val repo = RepoInfo(path = path, name = f.name, lastModified = f.lastModified())
        val current = _uiState.value.repos.toMutableList()
        current.removeIf { it.path == path }
        current.add(repo)
        _uiState.value = _uiState.value.copy(
            repos = current.sortedByDescending { it.lastModified },
            isLoading = false,
            error = null
        )
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
            if (result.success) registerRepo(localPath)
            else _uiState.value = _uiState.value.copy(error = result.error, isLoading = false)
        }
    }

    fun initRepository(path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.initRepository(path)
            if (result.success) registerRepo(path)
            else _uiState.value = _uiState.value.copy(error = result.error, isLoading = false)
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

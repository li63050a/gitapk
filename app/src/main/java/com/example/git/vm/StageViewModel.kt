package com.example.git.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.git.git.GitExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StageUiState(
    val stagedFiles: List<String> = emptyList(),
    val modifiedFiles: List<String> = emptyList(),
    val untrackedFiles: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StageViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(StageUiState())
    val uiState: StateFlow<StageUiState> = _uiState.asStateFlow()

    fun loadStatus(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.getStatus(repoPath)
            if (result.success) {
                val status = result.data ?: return@launch
                _uiState.value = _uiState.value.copy(
                    stagedFiles = status.stagedFiles,
                    modifiedFiles = status.modifiedFiles,
                    untrackedFiles = status.untrackedFiles,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun stage(repoPath: String, filePath: String) {
        viewModelScope.launch {
            gitExecutor.addFile(repoPath, filePath)
            loadStatus(repoPath)
        }
    }

    fun unstage(repoPath: String, filePath: String) {
        viewModelScope.launch {
            runCatching {
                val git = org.eclipse.jgit.api.Git.open(java.io.File(repoPath))
                git.reset().call()
                git.close()
            }
            loadStatus(repoPath)
        }
    }

    fun addAll(repoPath: String) {
        viewModelScope.launch {
            gitExecutor.addAll(repoPath)
            loadStatus(repoPath)
        }
    }

    fun commit(repoPath: String, message: String) {
        viewModelScope.launch {
            if (message.isBlank()) return@launch
            gitExecutor.commit(repoPath, message)
            loadStatus(repoPath)
        }
    }
}

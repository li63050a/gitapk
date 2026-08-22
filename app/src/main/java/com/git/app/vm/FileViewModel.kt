package com.git.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.git.GitExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FileUiState(
    val files: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFile: String? = null,
    val fileContent: String = ""
)

class FileViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(FileUiState())
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    fun loadFiles(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = gitExecutor.getWorkingTreeFiles(repoPath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    files = result.data ?: emptyList(),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun openFile(repoPath: String, relativePath: String) {
        viewModelScope.launch {
            val result = gitExecutor.readFile(repoPath, relativePath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    selectedFile = relativePath,
                    fileContent = result.data ?: ""
                )
            } else {
                _uiState.value = _uiState.value.copy(error = result.error)
            }
        }
    }

    fun saveFile(repoPath: String, relativePath: String, content: String) {
        viewModelScope.launch {
            gitExecutor.writeFile(repoPath, relativePath, content)
        }
    }

    fun closeFile() {
        _uiState.value = _uiState.value.copy(selectedFile = null, fileContent = "")
    }
}

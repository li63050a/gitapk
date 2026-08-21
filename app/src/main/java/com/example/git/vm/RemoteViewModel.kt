package com.example.git.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.git.git.GitExecutor
import com.example.git.git.Remote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RemoteUiState(
    val remotes: List<Remote> = emptyList(),
    val isLoading: Boolean = false,
    val isPulling: Boolean = false,
    val isPushing: Boolean = false,
    val isFetching: Boolean = false,
    val lastAction: String? = null,
    val error: String? = null
)

class RemoteViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun loadRemotes(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.getRemotes(repoPath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    remotes = result.data ?: emptyList(),
                    isLoading = false,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error
                )
            }
        }
    }

    fun pull(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPulling = true)
            val result = gitExecutor.pull(repoPath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    lastAction = "Pull: ${result.data}",
                    isPulling = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isPulling = false,
                    error = result.error
                )
            }
        }
    }

    fun push(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPushing = true)
            val result = gitExecutor.push(repoPath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    lastAction = "Push: ${result.data}",
                    isPushing = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isPushing = false,
                    error = result.error
                )
            }
        }
    }

    fun fetch(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetching = true)
            val result = gitExecutor.fetch(repoPath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    lastAction = "Fetch: ${result.data}",
                    isFetching = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isFetching = false,
                    error = result.error
                )
            }
        }
    }
}

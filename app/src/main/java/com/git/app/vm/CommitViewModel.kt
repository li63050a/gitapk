package com.git.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.git.Commit
import com.git.app.git.CommitDetail
import com.git.app.git.GitExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommitUiState(
    val commits: List<Commit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCommit: CommitDetail? = null,
    val isDetailLoading: Boolean = false
)

class CommitViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(CommitUiState())
    val uiState: StateFlow<CommitUiState> = _uiState.asStateFlow()

    fun loadCommits(repoPath: String, count: Int = 50) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.getCommits(repoPath, count)
            if (result.success) {
                _uiState.value = _uiState.value.copy(commits = result.data ?: emptyList(), isLoading = false, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun loadCommitDetail(repoPath: String, commitId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetailLoading = true)
            val result = gitExecutor.getCommitDetail(repoPath, commitId)
            if (result.success) {
                _uiState.value = _uiState.value.copy(selectedCommit = result.data, isDetailLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(isDetailLoading = false, error = result.error)
            }
        }
    }

    fun searchCommits(repoPath: String, query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadCommits(repoPath)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.searchCommitsByMessage(repoPath, query)
            if (result.success) {
                _uiState.value = _uiState.value.copy(commits = result.data ?: emptyList(), isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun clearDetail() {
        _uiState.value = _uiState.value.copy(selectedCommit = null)
    }
}

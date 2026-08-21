package com.git.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.git.GitExecutor
import com.git.app.git.Branch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BranchUiState(
    val branches: List<Branch> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BranchViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()

    private val _uiState = MutableStateFlow(BranchUiState())
    val uiState: StateFlow<BranchUiState> = _uiState.asStateFlow()

    fun loadBranches(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.getBranches(repoPath)
            if (result.success) {
                _uiState.value = _uiState.value.copy(
                    branches = result.data ?: emptyList(),
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

    fun createBranch(repoPath: String, branchName: String) {
        viewModelScope.launch {
            val result = gitExecutor.createBranch(repoPath, branchName)
            if (result.success) {
                loadBranches(repoPath)
            }
        }
    }

    fun deleteBranch(repoPath: String, branchName: String) {
        viewModelScope.launch {
            val result = gitExecutor.deleteBranch(repoPath, branchName)
            if (result.success) {
                loadBranches(repoPath)
            }
        }
    }

    fun checkoutBranch(repoPath: String, branchName: String) {
        viewModelScope.launch {
            val result = gitExecutor.checkoutBranch(repoPath, branchName)
            if (result.success) {
                loadBranches(repoPath)
            }
        }
    }
}

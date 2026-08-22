package com.git.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.git.GitExecutor
import com.git.app.git.Remote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GitConfigUiState(
    val userName: String = "",
    val userEmail: String = "",
    val remotes: List<Remote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GitConfigViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(GitConfigUiState())
    val uiState: StateFlow<GitConfigUiState> = _uiState.asStateFlow()

    fun loadConfig(repoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val nameResult = gitExecutor.getConfig(repoPath, "user.name")
                val emailResult = gitExecutor.getConfig(repoPath, "user.email")
                val remotesResult = gitExecutor.getRemotes(repoPath)
                
                _uiState.value = _uiState.value.copy(
                    userName = nameResult.data ?: "",
                    userEmail = emailResult.data ?: "",
                    remotes = remotesResult.data ?: emptyList(),
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setUserName(name: String) {
        _uiState.value = _uiState.value.copy(userName = name)
    }

    fun setUserEmail(email: String) {
        _uiState.value = _uiState.value.copy(userEmail = email)
    }

    fun saveUserConfig(repoPath: String) {
        viewModelScope.launch {
            gitExecutor.setConfig(repoPath, "user.name", _uiState.value.userName)
            gitExecutor.setConfig(repoPath, "user.email", _uiState.value.userEmail)
        }
    }

    fun addOrUpdateRemote(repoPath: String, name: String, url: String) {
        viewModelScope.launch {
            gitExecutor.addOrUpdateRemote(repoPath, name, url)
            loadConfig(repoPath)
        }
    }

    fun deleteRemote(repoPath: String, name: String) {
        viewModelScope.launch {
            gitExecutor.removeRemote(repoPath, name)
            loadConfig(repoPath)
        }
    }
}

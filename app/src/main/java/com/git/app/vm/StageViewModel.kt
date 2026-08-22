package com.git.app.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.data.SettingsRepository
import com.git.app.git.GitExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
                git.reset().addPath(filePath).call()
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

    fun commit(repoPath: String, message: String, context: Context) {
        viewModelScope.launch {
            if (message.isBlank()) return@launch
            // Use the repository-level identity if set, otherwise fall back to the
            // app-wide global default configured in Settings.
            val name = gitExecutor.getConfig(repoPath, "user.name").data
            val email = gitExecutor.getConfig(repoPath, "user.email").data
            val (authorName, authorEmail) = if (!name.isNullOrBlank() && !email.isNullOrBlank()) {
                name to email
            } else {
                val s = SettingsRepository.getSettings(context).first()
                s.gitUserName to s.gitUserEmail
            }
            gitExecutor.commit(repoPath, message, authorName, authorEmail)
            loadStatus(repoPath)
        }
    }
}

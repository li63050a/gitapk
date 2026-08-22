package com.git.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.git.app.git.GitExecutor
import com.git.app.git.SSHKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SSHUiState(
    val keys: List<SSHKey> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SSHViewModel : ViewModel() {
    private val gitExecutor = GitExecutor()
    private val _uiState = MutableStateFlow(SSHUiState())
    val uiState: StateFlow<SSHUiState> = _uiState.asStateFlow()

    fun loadKeys() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gitExecutor.listSSHKeys()
            if (result.success) {
                _uiState.value = _uiState.value.copy(keys = result.data ?: emptyList(), isLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.error)
            }
        }
    }

    fun addKey(name: String, content: String, passphrase: String = "") {
        viewModelScope.launch {
            val result = gitExecutor.addSSHKey(name, content, passphrase)
            if (result.success) loadKeys()
        }
    }

    fun deleteKey(name: String) {
        viewModelScope.launch {
            val result = gitExecutor.deleteSSHKey(name)
            if (result.success) loadKeys()
        }
    }
}

package com.git.app.data

import com.git.app.git.RepoInfo

class RepoManager {
    private val _repos = mutableListOf<RepoInfo>()
    private var _selectedRepo: RepoInfo? = null
    
    val repos: List<RepoInfo> get() = _repos
    val selectedRepo: RepoInfo? get() = _selectedRepo
    val selectedRepoPath: String? get() = _selectedRepo?.path

    fun selectRepo(path: String) {
        _selectedRepo = _repos.find { it.path == path }
    }

    fun refreshRepos(newRepos: List<RepoInfo>) {
        _repos.clear()
        _repos.addAll(newRepos)
        if (_selectedRepo == null && newRepos.isNotEmpty()) {
            _selectedRepo = newRepos.first()
        }
    }
}

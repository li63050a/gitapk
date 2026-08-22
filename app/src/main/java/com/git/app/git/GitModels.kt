package com.git.app.git

data class GitResult<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

data class Commit(
    val id: String,
    val shortId: String,
    val author: String,
    val date: String,
    val message: String,
    val branch: String? = null
)

data class CommitDetail(
    val commit: Commit,
    val files: List<CommitFile> = emptyList(),
    val diff: String = ""
)

data class CommitFile(
    val path: String,
    val status: String,
    val additions: Int = 0,
    val deletions: Int = 0
)

data class FileChange(
    val path: String,
    val staged: Boolean,
    val modified: Boolean,
    val untracked: Boolean
)

data class SearchCommitResult(
    val commits: List<Commit> = emptyList(),
    val files: List<String> = emptyList()
)

data class SSHKey(
    val name: String,
    val type: String,
    val fingerprint: String,
    val createdTime: String,
    val content: String,
    val hasPassphrase: Boolean = false
)

data class BatchOperationResult(
    val repoName: String,
    val operation: String,
    val success: Boolean,
    val message: String
)

data class Branch(
    val name: String,
    val isCurrent: Boolean,
    val commitId: String? = null
)

data class Remote(
    val name: String,
    val url: String
)

data class RepoStatus(
    val branch: String,
    val hasChanges: Boolean,
    val modifiedFiles: List<String> = emptyList(),
    val stagedFiles: List<String> = emptyList(),
    val untrackedFiles: List<String> = emptyList(),
    val conflictedFiles: List<String> = emptyList()
)

data class RepoInfo(
    val path: String,
    val name: String,
    val lastModified: Long = 0L
)

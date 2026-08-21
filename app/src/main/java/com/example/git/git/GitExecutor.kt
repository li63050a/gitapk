package com.example.git.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class GitExecutor {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun openRepository(repoPath: String): Repository? {
        return try {
            val builder = FileRepositoryBuilder()
            builder
                .setGitDir(java.io.File(repoPath, ".git"))
                .readEnvironment()
                .findGitDir()
                ?.let { builder.build() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cloneRepository(url: String, localPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(java.io.File(localPath))
                .call()
            GitResult(success = true, data = localPath)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Clone failed")
        }
    }

    suspend fun initRepository(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Git.init().setDirectory(java.io.File(repoPath)).call()
            GitResult(success = true, data = repoPath)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Init failed")
        }
    }

    suspend fun getCommits(repoPath: String, count: Int = 50): GitResult<List<Commit>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val commits = git.log().setMaxCount(count).call()
            val result = commits.map { it.toCommit() }.toList()
            git.close()
            GitResult(success = true, data = result)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get commits")
        }
    }

    suspend fun getCommitDetail(repoPath: String, commitId: String): GitResult<CommitDetail> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val revWalk = org.eclipse.jgit.revwalk.RevWalk(git.repository)
            val commitIdObj = git.repository.resolve(commitId) ?: run {
                git.close()
                return@withContext GitResult(success = false, error = "Commit not found: $commitId")
            }
            val commit = revWalk.parseCommit(commitIdObj)
            
            val parentTree = if (commit.parentCount > 0) {
                org.eclipse.jgit.treewalk.CanonicalTreeParser().also { tp ->
                    tp.reset(git.repository.newObjectReader(), commit.getParent(0).tree)
                }
            } else null
            
            val newTree = org.eclipse.jgit.treewalk.CanonicalTreeParser().also { tp ->
                tp.reset(git.repository.newObjectReader(), commit.tree)
            }
            
            val diffEntries = git.diff()
                .setOldTree(parentTree)
                .setNewTree(newTree)
                .call()
            
            val files = diffEntries.map { entry ->
                CommitFile(
                    path = entry.newPath,
                    status = entry.changeType.name,
                    additions = 0,
                    deletions = 0
                )
            }
            
            val diffOut = ByteArrayOutputStream()
            val diffFormatter = org.eclipse.jgit.diff.DiffFormatter(diffOut)
            diffFormatter.setRepository(git.repository)
            diffEntries.forEach { diffFormatter.format(it) }
            val diffText = diffOut.toString("UTF-8")
            diffFormatter.close()
            
            revWalk.close()
            git.close()
            
            val commitTimeStr = try {
                dateFormatter.format(Date(commit.commitTime.toLong() * 1000))
            } catch (e: Exception) {
                commit.commitTime.toString()
            }
            
            val detailCommit = Commit(
                id = commit.name,
                shortId = commit.name.substring(0, minOf(7, commit.name.length)),
                author = commit.authorIdent.name,
                date = commitTimeStr,
                message = commit.fullMessage.trim(),
                branch = null
            )
            
            GitResult(success = true, data = CommitDetail(
                commit = detailCommit,
                files = files,
                diff = diffText
            ))
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get commit detail")
        }
    }

    suspend fun searchCommits(repoPath: String, query: String, count: Int = 50): GitResult<List<Commit>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val commits = git.log().addPath(query).setMaxCount(count).call()
            val result = commits.map { it.toCommit() }.toList()
            git.close()
            GitResult(success = true, data = result)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Search failed")
        }
    }

    suspend fun searchCommitsByMessage(repoPath: String, query: String, count: Int = 50): GitResult<List<Commit>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val allCommits = git.log().setMaxCount(count * 3).call().map { it.toCommit() }.toList()
            val filtered = allCommits.filter { 
                it.message.contains(query, ignoreCase = true) || 
                it.author.contains(query, ignoreCase = true) ||
                it.shortId.startsWith(query, ignoreCase = true)
            }.take(count)
            git.close()
            GitResult(success = true, data = filtered)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Search failed")
        }
    }

    suspend fun searchFiles(repoPath: String, query: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val allFiles = git.status().call().added.toList() + git.status().call().modified.toList() + git.status().call().untracked.toList()
            val filtered = allFiles.filter { it.contains(query, ignoreCase = true) }.take(50)
            git.close()
            GitResult(success = true, data = filtered)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Search failed")
        }
    }

    suspend fun getBranches(repoPath: String): GitResult<List<Branch>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val branches = git.branchList().call()
            val currentBranch = git.repository.branch
            val result = branches.map { branch ->
                Branch(
                    name = branch.name.removePrefix("refs/heads/"),
                    isCurrent = branch.name == "refs/heads/$currentBranch",
                    commitId = branch.objectId?.name
                )
            }
            git.close()
            GitResult(success = true, data = result)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get branches")
        }
    }

    suspend fun getCurrentBranch(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val branch = git.repository.branch
            git.close()
            GitResult(success = true, data = branch)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get current branch")
        }
    }

    suspend fun addFile(repoPath: String, filePath: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.add().addFilepattern(filePath).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Add failed")
        }
    }

    suspend fun removeFile(repoPath: String, filePath: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.rm().addFilepattern(filePath).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Remove failed")
        }
    }

    suspend fun addAll(repoPath: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.add().addFilepattern(".").call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Add all failed")
        }
    }

    suspend fun commit(repoPath: String, message: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val result = git.commit().setMessage(message).call()
            git.close()
            GitResult(success = true, data = result.name)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Commit failed")
        }
    }

    suspend fun createBranch(repoPath: String, branchName: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.branchCreate().setName(branchName).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Create branch failed")
        }
    }

    suspend fun deleteBranch(repoPath: String, branchName: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.branchDelete().setBranchNames(branchName).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Delete branch failed")
        }
    }

    suspend fun checkoutBranch(repoPath: String, branchName: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.checkout().setName(branchName).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Checkout failed")
        }
    }

    suspend fun getStatus(repoPath: String): GitResult<RepoStatus> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val status = git.status().call()
            val branch = git.repository.branch
            val modified = status.modified.toList()
            val added = status.added.toList()
            val removed = status.removed.toList()
            val changed = status.changed.toList()
            val missing = status.missing.toList()
            val untracked = status.untracked.toList()
            git.close()

            val allChanged = (modified + added + removed + changed + missing).distinct()
            GitResult(success = true, data = RepoStatus(
                branch = branch,
                hasChanges = allChanged.isNotEmpty(),
                modifiedFiles = modified,
                stagedFiles = added,
                untrackedFiles = untracked,
                conflictedFiles = emptyList()
            ))
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get status failed")
        }
    }

    suspend fun pull(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.pull().call()
            git.close()
            GitResult(success = true, data = "Pull completed successfully")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Pull failed: ${e.message}")
        }
    }

    suspend fun push(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.push().call()
            git.close()
            GitResult(success = true, data = "Push completed successfully")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Push failed: ${e.message}")
        }
    }

    suspend fun fetch(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.fetch().call()
            git.close()
            GitResult(success = true, data = "Fetch completed successfully")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Fetch failed: ${e.message}")
        }
    }

    suspend fun getRemotes(repoPath: String): GitResult<List<Remote>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val remoteNames = git.remoteList().call().map { it.name }
            val remotes = remoteNames.map { name ->
                val config = git.repository.config
                val url = config.getString("remote", name, "url") ?: ""
                Remote(name = name, url = url)
            }
            git.close()
            GitResult(success = true, data = remotes)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get remotes failed")
        }
    }

    suspend fun getFileDiff(repoPath: String, filePath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val os = ByteArrayOutputStream()
            val diffFmt = org.eclipse.jgit.diff.DiffFormatter(os)
            val status = git.status().call()
            if (status.modified.contains(filePath) || status.added.contains(filePath) || status.changed.contains(filePath)) {
                val diff = git.diff().call()
                diff.forEach { diffEntry ->
                    if (diffEntry.newPath == filePath || diffEntry.oldPath == filePath) {
                        diffFmt.format(diffEntry)
                    }
                }
                diffFmt.close()
                git.close()
                GitResult(success = true, data = os.toString())
            } else {
                GitResult(success = false, error = "No changes for $filePath")
            }
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get diff failed")
        }
    }

    suspend fun getStagedFiles(repoPath: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val status = git.status().call()
            val staged = status.added.toList() + status.modified.toList()
            git.close()
            GitResult(success = true, data = staged)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get staged files failed")
        }
    }

    suspend fun batchPull(repos: List<String>): GitResult<List<BatchOperationResult>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val results = repos.map { repoPath ->
                val repoName = java.io.File(repoPath).name
                val result = pull(repoPath)
                BatchOperationResult(
                    repoName = repoName,
                    operation = "pull",
                    success = result.success,
                    message = result.data ?: result.error ?: "Unknown"
                )
            }
            GitResult(success = true, data = results)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Batch pull failed")
        }
    }

    suspend fun batchPush(repos: List<String>): GitResult<List<BatchOperationResult>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val results = repos.map { repoPath ->
                val repoName = java.io.File(repoPath).name
                val result = push(repoPath)
                BatchOperationResult(
                    repoName = repoName,
                    operation = "push",
                    success = result.success,
                    message = result.data ?: result.error ?: "Unknown"
                )
            }
            GitResult(success = true, data = results)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Batch push failed")
        }
    }

    suspend fun batchFetch(repos: List<String>): GitResult<List<BatchOperationResult>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val results = repos.map { repoPath ->
                val repoName = java.io.File(repoPath).name
                val result = fetch(repoPath)
                BatchOperationResult(
                    repoName = repoName,
                    operation = "fetch",
                    success = result.success,
                    message = result.data ?: result.error ?: "Unknown"
                )
            }
            GitResult(success = true, data = results)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Batch fetch failed")
        }
    }

    // SSH Key Management
    suspend fun listSSHKeys(): GitResult<List<SSHKey>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val homeDir = System.getProperty("user.home") ?: "/root"
            val sshDir = java.io.File(homeDir, ".ssh")
            if (!sshDir.exists()) {
                return@withContext GitResult(success = true, data = emptyList())
            }
            
            val keys = mutableListOf<SSHKey>()
            sshDir.listFiles { file -> 
                file.isFile && (file.name == "id_rsa" || file.name == "id_ed25519" || 
                    file.name == "id_ecdsa" || file.name == "id_dsa" || file.name.startsWith("id_"))
            }?.forEach { file ->
                val keyType = when {
                    file.name.contains("rsa") -> "RSA"
                    file.name.contains("ed25519") -> "ED25519"
                    file.name.contains("ecdsa") -> "ECDSA"
                    file.name.contains("dsa") -> "DSA"
                    else -> "UNKNOWN"
                }
                val content = file.readText().trim()
                keys.add(SSHKey(
                    name = file.name,
                    type = keyType,
                    fingerprint = "sha256:xxxx",
                    createdTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(file.lastModified())),
                    content = content
                ))
            }
            GitResult(success = true, data = keys)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to list SSH keys")
        }
    }

    suspend fun addSSHKey(name: String, content: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val homeDir = System.getProperty("user.home") ?: "/root"
            val sshDir = java.io.File(homeDir, ".ssh")
            if (!sshDir.exists()) sshDir.mkdirs()
            val keyFile = java.io.File(sshDir, name)
            keyFile.writeText(content)
            keyFile.setReadable(true, false)
            keyFile.setExecutable(false, false)
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to add SSH key")
        }
    }

    suspend fun deleteSSHKey(name: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val homeDir = System.getProperty("user.home") ?: "/root"
            val keyFile = java.io.File(homeDir, ".ssh/$name")
            if (keyFile.exists()) keyFile.delete()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to delete SSH key")
        }
    }

    private fun RevCommit.toCommit(): Commit {
        return Commit(
            id = this.name,
            shortId = this.id.name.substring(0, minOf(7, this.id.name.length)),
            author = this.authorIdent.name,
            date = try {
                dateFormatter.format(Date(this.commitTime.toLong() * 1000))
            } catch (e: Exception) {
                this.commitTime.toString()
            },
            message = this.fullMessage.trim(),
            branch = null
        )
    }
}

package com.git.app.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.sshd.KeyPasswordProvider
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class GitExecutor {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private fun sshCallback(): TransportConfigCallback = TransportConfigCallback { transport ->
        if (transport is SshTransport) {
            transport.setSshSessionFactory(sshFactory())
        }
    }

    private fun sshFactory(): SshSessionFactory {
        return object : SshdSessionFactory() {
            override fun createKeyPasswordProvider(credentialsProvider: CredentialsProvider?): KeyPasswordProvider {
                val homeDir = System.getProperty("user.home") ?: "/root"
                val sshDir = java.io.File(homeDir, ".ssh")
                val passphrases: List<CharArray> = if (sshDir.exists()) {
                    sshDir.listFiles { f -> f.isFile && f.name.endsWith(".pass") }
                        ?.mapNotNull { f ->
                            val text = f.readText().trim()
                            if (text.isBlank()) null else text.toCharArray()
                        } ?: emptyList()
                } else emptyList()
                return object : KeyPasswordProvider {
                    private var attempt = 0
                    override fun getPassphrase(uri: URIish?, attempt: Int): CharArray? {
                        if (passphrases.isEmpty()) return null
                        return passphrases[attempt % passphrases.size]
                    }
                    override fun setAttempts(attempts: Int) { this.attempt = attempts }
                    override fun getAttempts(): Int = attempt
                    override fun keyLoaded(uri: URIish?, attempt: Int, exc: Exception?): Boolean = exc == null
                }
            }
        }
    }

    fun openRepository(repoPath: String): Repository? {
        return try {
            FileRepositoryBuilder().setGitDir(java.io.File(repoPath, ".git")).build()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun cloneRepository(url: String, localPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Git.cloneRepository()
                .setURI(url)
                .setDirectory(java.io.File(localPath))
                .setTransportConfigCallback(sshCallback())
                .call()
            GitResult(success = true, data = localPath)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Clone failed")
        }
    }

    suspend fun initRepository(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dir = java.io.File(repoPath)
            dir.mkdirs()
            // If the directory already holds a Git repository, reuse it instead of
            // creating a brand new .git folder (which would be redundant).
            if (!java.io.File(dir, ".git").exists()) {
                Git.init().setDirectory(dir).call()
            }
            GitResult(success = true, data = repoPath)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Init failed")
        }
    }

    /** Read the URL of a named remote (default: origin). */
    suspend fun getRemoteUrl(repoPath: String, remote: String = "origin"): GitResult<String> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val git = Git.open(java.io.File(repoPath))
                val url = git.remoteList().call()
                    .firstOrNull { it.name == remote }
                    ?.urIs?.firstOrNull()?.toString()
                git.close()
                if (url != null) GitResult(success = true, data = url)
                else GitResult(success = false, error = "no remote")
            } catch (e: Exception) {
                GitResult(success = false, error = e.message ?: "Failed to read remote")
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
            val status = git.status().call()
            val allFiles = status.added.toList() + status.modified.toList() + status.untracked.toList()
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

    suspend fun commit(
        repoPath: String,
        message: String,
        authorName: String? = null,
        authorEmail: String? = null
    ): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val cmd = git.commit().setMessage(message)
            if (!authorName.isNullOrBlank() && !authorEmail.isNullOrBlank()) {
                val author = org.eclipse.jgit.lib.PersonIdent(authorName, authorEmail)
                cmd.setAuthor(author)
                cmd.setCommitter(author)
            }
            val result = cmd.call()
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
                conflictedFiles = status.conflictingStageState.keys.toList()
            ))
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get status failed")
        }
    }

    suspend fun pull(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.pull().setTransportConfigCallback(sshCallback()).call()
            git.close()
            GitResult(success = true, data = "Pull completed successfully")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Pull failed: ${e.message}")
        }
    }

    suspend fun push(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.push().setTransportConfigCallback(sshCallback()).call()
            git.close()
            GitResult(success = true, data = "Push completed successfully")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Push failed: ${e.message}")
        }
    }

    suspend fun fetch(repoPath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.fetch().setTransportConfigCallback(sshCallback()).call()
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
            git.use { g ->
                val os = ByteArrayOutputStream()
                val diffFmt = org.eclipse.jgit.diff.DiffFormatter(os)
                diffFmt.use { fmt ->
                    val status = g.status().call()
                    if (status.modified.contains(filePath) || status.added.contains(filePath) || status.changed.contains(filePath)) {
                        val diff = g.diff().setPathFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath)).call()
                        diff.forEach { fmt.format(it) }
                        GitResult(success = true, data = os.toString())
                    } else {
                        GitResult(success = false, error = "No changes for $filePath")
                    }
                }
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
                val passFile = java.io.File(sshDir, "${file.name}.pass")
                val hasPassphrase = passFile.exists() && passFile.readText().isNotBlank()
                keys.add(SSHKey(
                    name = file.name,
                    type = keyType,
                    fingerprint = "sha256:xxxx",
                    createdTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(file.lastModified())),
                    content = content,
                    hasPassphrase = hasPassphrase
                ))
            }
            GitResult(success = true, data = keys)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to list SSH keys")
        }
    }

    suspend fun addSSHKey(name: String, content: String, passphrase: String = ""): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val homeDir = System.getProperty("user.home") ?: "/root"
            val sshDir = java.io.File(homeDir, ".ssh")
            if (!sshDir.exists()) sshDir.mkdirs()
            val keyFile = java.io.File(sshDir, name)
            keyFile.writeText(content)
            keyFile.setReadable(true, false)
            keyFile.setExecutable(false, false)
            val passFile = java.io.File(sshDir, "$name.pass")
            if (passphrase.isNotBlank()) {
                passFile.writeText(passphrase)
                passFile.setReadable(true, false)
            } else {
                if (passFile.exists()) passFile.delete()
            }
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
            val passFile = java.io.File(homeDir, ".ssh/$name.pass")
            if (passFile.exists()) passFile.delete()
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

    suspend fun setConfig(repoPath: String, key: String, value: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val parts = key.split(".")
            if (parts.size < 2) {
                git.close()
                return@withContext GitResult(success = false, error = "Invalid config key: $key")
            }
            git.repository.config.setString(parts[0], parts[1], null, value)
            git.repository.config.save()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to set config")
        }
    }

    suspend fun getConfig(repoPath: String, key: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val value = when {
                key == "user.name" -> git.repository.config.getString("user", null, "name")
                key == "user.email" -> git.repository.config.getString("user", null, "email")
                else -> null
            }
            git.close()
            GitResult(success = true, data = value ?: "")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get config")
        }
    }

    suspend fun addOrUpdateRemote(repoPath: String, name: String, url: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val config = git.repository.config
            config.setString("remote", name, "url", url)
            config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/$name/*")
            config.save()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to add/update remote")
        }
    }

    suspend fun removeRemote(repoPath: String, name: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.remoteRemove().setRemoteName(name).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to remove remote")
        }
    }

    suspend fun getStashList(repoPath: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val stashList = git.stashList().call()
            val stashes = stashList.mapIndexed { index, commit ->
                "stash@{$index}: ${commit.shortMessage}"
            }
            git.close()
            GitResult(success = true, data = stashes)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get stash list")
        }
    }

    suspend fun getFiles(repoPath: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val dirCache = git.repository.readDirCache()
            val files = (0 until dirCache.entryCount).map { dirCache.getEntry(it).getPathString() }
            git.close()
            GitResult(success = true, data = files)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to get files")
        }
    }

    /** Recursively list all files in the working tree (excludes .git). */
    suspend fun getWorkingTreeFiles(repoPath: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val root = java.io.File(repoPath)
            val result = mutableListOf<String>()
            if (root.isDirectory) {
                root.walkTopDown()
                    .onEnter { !it.name.equals(".git", ignoreCase = true) }
                    .forEach { file ->
                        if (file.isFile) {
                            result.add(file.relativeTo(root).path)
                        }
                    }
            }
            GitResult(success = true, data = result.sorted())
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to list files")
        }
    }

    suspend fun readFile(repoPath: String, relativePath: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = java.io.File(repoPath, relativePath)
            GitResult(success = true, data = file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to read file")
        }
    }

    suspend fun writeFile(repoPath: String, relativePath: String, content: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = java.io.File(repoPath, relativePath)
            file.writeText(content, Charsets.UTF_8)
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Failed to write file")
        }
    }

    suspend fun merge(repoPath: String, branchName: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val target = git.repository.resolve(branchName) ?: run {
                git.close()
                return@withContext GitResult(success = false, error = "Branch not found: $branchName")
            }
            val result = git.merge().include(target).call()
            git.close()
            val ok = result.mergeStatus == org.eclipse.jgit.api.MergeResult.MergeStatus.MERGED
                || result.mergeStatus == org.eclipse.jgit.api.MergeResult.MergeStatus.ALREADY_UP_TO_DATE
            GitResult(success = ok, data = "Merged $branchName")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Merge failed")
        }
    }

    suspend fun stashCreate(repoPath: String, message: String? = null): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val cmd = git.stashCreate()
            if (message != null) {
                runCatching { cmd.javaClass.getMethod("setMessage", String::class.java).invoke(cmd, message) }
            }
            cmd.call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Stash create failed")
        }
    }

    suspend fun stashPop(repoPath: String, index: Int = 0): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.stashApply().setStashRef("stash@{$index}").call()
            git.stashDrop().setStashRef(index).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Stash pop failed")
        }
    }

    suspend fun stashDrop(repoPath: String, index: Int = 0): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.stashDrop().setStashRef(index).call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Stash drop failed")
        }
    }

    suspend fun createTag(repoPath: String, tagName: String, message: String = ""): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val tagBuilder = git.tag().setName(tagName)
            if (message.isNotEmpty()) tagBuilder.setMessage(message)
            tagBuilder.call()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Tag create failed")
        }
    }

    suspend fun deleteTag(repoPath: String, tagName: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val refUpdate = git.repository.refDatabase.newUpdate("refs/tags/$tagName", false)
            refUpdate.delete()
            git.close()
            GitResult(success = true, data = true)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Tag delete failed")
        }
    }

    suspend fun getTags(repoPath: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val tags = git.tagList().call().map { it.name }
            git.close()
            GitResult(success = true, data = tags)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get tags failed")
        }
    }

    suspend fun reset(repoPath: String, commitId: String, hard: Boolean = false): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val resetBuilder = git.reset().setRef(commitId)
            resetBuilder.setMode(if (hard) org.eclipse.jgit.api.ResetCommand.ResetType.HARD else org.eclipse.jgit.api.ResetCommand.ResetType.MIXED)
            resetBuilder.call()
            git.close()
            GitResult(success = true, data = "Reset to $commitId")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Reset failed")
        }
    }

    suspend fun revert(repoPath: String, commitId: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val target = git.repository.resolve(commitId) ?: run {
                git.close()
                return@withContext GitResult(success = false, error = "Commit not found: $commitId")
            }
            git.revert().include(target).call()
            git.close()
            GitResult(success = true, data = "Reverted $commitId")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Revert failed")
        }
    }

    suspend fun cherryPick(repoPath: String, commitId: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val target = git.repository.resolve(commitId) ?: run {
                git.close()
                return@withContext GitResult(success = false, error = "Commit not found: $commitId")
            }
            git.cherryPick().include(target).call()
            git.close()
            GitResult(success = true, data = "Cherry-picked $commitId")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Cherry-pick failed")
        }
    }

    suspend fun amendCommit(repoPath: String, message: String): GitResult<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            git.commit().setMessage(message).setAmend(true).call()
            git.close()
            GitResult(success = true, data = "Amended commit")
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Amend failed")
        }
    }

    suspend fun getReflog(repoPath: String): GitResult<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val git = Git.open(java.io.File(repoPath))
            val reflog = git.reflog().call()
            val entries = reflog.map { "${it.newId.name.substring(0, minOf(7, it.newId.name.length))} ${it.comment}" }
            git.close()
            GitResult(success = true, data = entries)
        } catch (e: Exception) {
            GitResult(success = false, error = e.message ?: "Get reflog failed")
        }
    }
}

package uk.oshawk.jadx.collaboration

import com.google.gson.GsonBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import jadx.api.data.impl.JadxCodeData
import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo
import jadx.api.plugins.events.JadxEvents
import jadx.api.plugins.events.types.ReloadProject
import java.io.File
import java.io.FileNotFoundException

class Plugin : JadxPlugin {
    companion object {
        const val ID = "jadx-collaboration"
        val LOG = KotlinLogging.logger(ID)
        val GSON = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    }

    private val options = Options()
    private var context: JadxPluginContext? = null

    override fun getPluginInfo() = JadxPluginInfo(ID, "JADX Collaboration", "Collaboration support for JADX")

    override fun init(context: JadxPluginContext?) {
        this.context = context

        this.context?.registerOptions(options)

        this.context?.guiContext?.addMenuAction("Pull") { this.context?.guiContext?.uiRun(this::pull) }
        this.context?.guiContext?.addMenuAction("Push") { this.context?.guiContext?.uiRun(this::push) }

        this.context?.guiContext?.registerGlobalKeyBinding("$ID.pull", "ctrl BACK_SLASH") { this.context?.guiContext?.uiRun(this::pull) }
        this.context?.guiContext?.registerGlobalKeyBinding("$ID.push", "ctrl shift BACK_SLASH") { this.context?.guiContext?.uiRun(this::push) }
    }

    private fun <R> readRepository(suffix: String, default: R): R? {
        if (options.repository.isEmpty()) {
            LOG.error { "No repository file is set. Configure it in settings." }
            return null
        }

        val repositoryFile = File("${options.repository}$suffix")
        return try {
            repositoryFile.reader().use {
                GSON.fromJson(it, default!!::class.java) ?: default
            }
        } catch (_: FileNotFoundException) {
            default
        } catch (_: Exception) {
            LOG.error { "Repository file (${options.repository}$suffix) corrupt. Requires manual fixing." }
            null
        }
    }

    private fun readLocalRepository() = readRepository(".local", LocalRepository())
    private fun readRemoteRepository() = readRepository("", RemoteRepository())

    private fun <R> writeRepository(suffix: String, repository: R): Unit? {
        if (options.repository.isEmpty()) {
            LOG.error { "No repository file is set. Configure it in settings." }
            return null
        }

        val repositoryFile = File("${options.repository}$suffix")
        return try {
            repositoryFile.writer().use {
                GSON.toJson(repository, it)
            }
        } catch (_: FileNotFoundException) {
            LOG.error { "Repository file (${options.repository}$suffix) invalid path." }
            null
        } catch (_: Exception) {
            LOG.error { "Repository file (${options.repository}$suffix) corrupt. Requires manual fixing." }
            null
        }
    }

    private fun writeLocalRepository(repository: LocalRepository) = writeRepository(".local", repository)
    private fun writeRemoteRepository(repository: RemoteRepository) = writeRepository(".", repository)

    fun projectToLocalRepository(localRepository: LocalRepository) {
        val projectRenames = this.context!!.args.codeData.renames.sorted()
        var projectRenamesIndex = 0
        LOG.info { "projectToLocalRepository: ${projectRenames.size} project renames" }

        val oldLocalRepositoryRenames = localRepository.renames
        var oldLocalRepositoryRenamesIndex = 0
        localRepository.renames = mutableListOf()
        LOG.info { "projectToLocalRepository: ${oldLocalRepositoryRenames.size} old local repository renames" }

        while (projectRenamesIndex != projectRenames.size || oldLocalRepositoryRenamesIndex != oldLocalRepositoryRenames.size) {
            val projectRename = projectRenames.getOrNull(projectRenamesIndex)
            val oldLocalRepositoryRename = oldLocalRepositoryRenames.getOrNull(oldLocalRepositoryRenamesIndex)

            localRepository.renames.add(when {
                projectRename == null || (oldLocalRepositoryRename != null && projectRename.nodeRef > oldLocalRepositoryRename.nodeRef) -> {
                    // Local repository rename not present in project. Must have been deleted.
                    oldLocalRepositoryRenamesIndex++
                    LocalRename(oldLocalRepositoryRename!!.nodeRef, null, oldLocalRepositoryRename!!.lastPullNewName)
                }
                oldLocalRepositoryRename == null || (projectRename != null && projectRename.nodeRef < oldLocalRepositoryRename.nodeRef) -> {
                    // Project rename not present in local repository. Add it.
                    projectRenamesIndex++
                    LocalRename(NodeRef(projectRename!!.nodeRef), projectRename!!.newName, null)
                }
                else -> {
                    projectRenamesIndex++
                    oldLocalRepositoryRenamesIndex++

                    if (projectRename.newName == oldLocalRepositoryRename.newName) {
                        // No change. Keep the local repository rename.
                        oldLocalRepositoryRename
                    } else {
                        // Change. Replace with the project rename, preserving the last pull new name.
                        LocalRename(NodeRef(projectRename.nodeRef), projectRename.newName, oldLocalRepositoryRename.lastPullNewName)
                    }
                }
            })
        }

        LOG.info { "projectToLocalRepository: ${localRepository.renames.size} new local repository renames" }
    }

    fun remoteRepositoryToLocalRepository(remoteRepository: RemoteRepository, localRepository: LocalRepository): Boolean {
        var remoteRepositoryRenamesIndex = 0
        LOG.info { "remoteRepositoryToLocalRepository: ${remoteRepository.renames.size} remote repository renames" }

        val oldLocalRepositoryRenames = localRepository.renames
        var oldLocalRepositoryRenamesIndex = 0
        localRepository.renames = mutableListOf()
        LOG.info { "remoteRepositoryToLocalRepository: ${oldLocalRepositoryRenames.size} old local repository renames" }

        var conflict = false

        while (remoteRepositoryRenamesIndex != remoteRepository.renames.size || oldLocalRepositoryRenamesIndex != oldLocalRepositoryRenames.size) {
            val remoteRepositoryRename = remoteRepository.renames.getOrNull(remoteRepositoryRenamesIndex)
            val oldLocalRepositoryRename = oldLocalRepositoryRenames.getOrNull(oldLocalRepositoryRenamesIndex)

            localRepository.renames.add(when {
                remoteRepositoryRename == null || (oldLocalRepositoryRename != null && remoteRepositoryRename.nodeRef > oldLocalRepositoryRename.nodeRef) -> {
                    // Local repository rename not present in remote repository. Keep it as is.
                    oldLocalRepositoryRenamesIndex++
                    assert(oldLocalRepositoryRename!!.lastPullNewName == null) // If a rename was deleted on remote, an entry should still be present, but with a null new name.
                    oldLocalRepositoryRename!!
                }
                oldLocalRepositoryRename == null || (remoteRepositoryRename != null && remoteRepositoryRename.nodeRef < oldLocalRepositoryRename.nodeRef) -> {
                    // Remote repository rename not present in local repository. Add it (again, deletions would be explicit),
                    remoteRepositoryRenamesIndex++
                    LocalRename(remoteRepositoryRename!!.nodeRef, remoteRepositoryRename!!.newName, remoteRepositoryRename!!.newName)
                }
                else -> {
                    remoteRepositoryRenamesIndex++
                    oldLocalRepositoryRenamesIndex++

                    if (remoteRepositoryRename.newName == oldLocalRepositoryRename.newName) {
                        // No change. Update the last push new name just in case.
                        LocalRename(remoteRepositoryRename.nodeRef, remoteRepositoryRename.newName, remoteRepositoryRename.newName)
                    } else if (remoteRepositoryRename.newName == oldLocalRepositoryRename.lastPullNewName) {
                        // Local repository rename supersedes remote repository rename. Use the local repository rename.
                        oldLocalRepositoryRename
                    } else {
                        // Conflict. Currently, use our version. TODO: Actual conflict resolution (would need GUI).
                        conflict = true
                        LocalRename(oldLocalRepositoryRename.nodeRef, oldLocalRepositoryRename.newName, remoteRepositoryRename.newName)
                    }
                }
            })
        }

        LOG.info { "remoteRepositoryToLocalRepository: ${localRepository.renames.size} new local repository renames" }

        return conflict
    }

    private fun localRepositoryToProject(localRepository: LocalRepository) {
        (this.context!!.args.codeData as JadxCodeData).renames = localRepository.renames
                .filter { it.newName != null }
                .map { ProjectRename(it.nodeRef, it.newName!!) }

        context!!.events().send(ReloadProject::class.java.declaredFields.first().get(null) as ReloadProject)  // TODO: Change this when the singleton member name is stable.
    }

    fun pull(): Boolean? {
        // Update local repository with project changes.
        // Pull remote repository into local repository.
        // Update project from local repository.

        val localRepository = readLocalRepository() ?: return null

        projectToLocalRepository(localRepository)

        // TODO: Run pre-pull script.

        val remoteRepository = readRemoteRepository() ?: return null

        val conflict = remoteRepositoryToLocalRepository(remoteRepository, localRepository)

        writeLocalRepository(localRepository) ?: return null

        localRepositoryToProject(localRepository)

        return conflict
    }

    fun push() {

    }
}

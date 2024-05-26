package uk.oshawk.jadx.collaboration

import jadx.api.data.*
import jadx.api.data.impl.JadxCodeComment
import jadx.api.data.impl.JadxCodeRef
import jadx.api.data.impl.JadxCodeRename
import jadx.api.data.impl.JadxNodeRef
import java.util.*

annotation class NoArgs  // Used by the no-args plugin. Makes GSON reliable by adding no-argument constructors.

@NoArgs
class NodeRef(
    val type: IJavaNodeRef.RefType,
    val declaringClass: String,
    val shortId: String?,  // Seems to be null for classes.
) : Comparable<NodeRef> {
    constructor(nodeRef: IJavaNodeRef) : this(nodeRef.type, nodeRef.declaringClass, nodeRef.shortId)

    override fun compareTo(other: NodeRef) =
        compareValuesBy(this, other, { it.type }, { it.declaringClass }, { it.shortId })

    fun convert(): JadxNodeRef {
        return JadxNodeRef(type, declaringClass, shortId)
    }
}

@NoArgs
class CodeRef(
    val attachType: CodeRefType,
    val index: Int,
) : Comparable<CodeRef> {
    constructor(codeRef: IJavaCodeRef) : this(codeRef.attachType, codeRef.index)

    override fun compareTo(other: CodeRef) = compareValuesBy(this, other, { it.attachType }, { it.index })

    fun convert(): JadxCodeRef {
        return JadxCodeRef(attachType, index)
    }
}

@NoArgs
class Identifier(val nodeRef: NodeRef, val codeRef: CodeRef?) : Comparable<Identifier> {
    override fun compareTo(other: Identifier) = compareValuesBy(this, other, { it.nodeRef }, { it.codeRef })
}

interface ProjectItem {
    val identifier: Identifier

    fun repositoryItem(versionVector: MutableMap<UUID, Long>): RepositoryItem

    fun matches(projectItem: ProjectItem): Boolean

    fun matches(repositoryItem: RepositoryItem): Boolean
}

@NoArgs
class ProjectRename(override val identifier: Identifier, val newName: String) : ProjectItem {
    constructor(rename: ICodeRename) : this(
        Identifier(NodeRef(rename.nodeRef), rename.codeRef?.let { CodeRef(it) }),
        rename.newName
    )

    override fun repositoryItem(versionVector: MutableMap<UUID, Long>) =
        RepositoryRename(identifier, versionVector, newName)

    override fun matches(projectItem: ProjectItem): Boolean {
        if (projectItem !is ProjectRename) {
            return false
        }

        return projectItem.newName == newName
    }

    override fun matches(repositoryItem: RepositoryItem): Boolean {
        if (repositoryItem !is RepositoryRename) {
            return false
        }

        return repositoryItem.newName == newName
    }

    fun convert(): JadxCodeRename {
        return JadxCodeRename(identifier.nodeRef.convert(), identifier.codeRef?.convert(), newName)
    }
}

@NoArgs
class ProjectComment(override val identifier: Identifier, val comment: String, val style: CommentStyle) : ProjectItem {
    constructor(comment: ICodeComment) : this(
        Identifier(NodeRef(comment.nodeRef), comment.codeRef?.let { CodeRef(it) }),
        comment.comment,
        comment.style
    )

    override fun repositoryItem(versionVector: MutableMap<UUID, Long>) =
        RepositoryComment(identifier, versionVector, comment, style)

    override fun matches(projectItem: ProjectItem): Boolean {
        if (projectItem !is ProjectComment) {
            return false
        }

        return projectItem.comment == comment && projectItem.style == style
    }

    override fun matches(repositoryItem: RepositoryItem): Boolean {
        if (repositoryItem !is RepositoryComment) {
            return false
        }

        return repositoryItem.comment == comment && repositoryItem.style == style
    }

    fun convert(): JadxCodeComment {
        return JadxCodeComment(identifier.nodeRef.convert(), identifier.codeRef?.convert(), comment, style)
    }
}

interface RepositoryItem {
    val identifier: Identifier
    val versionVector: MutableMap<UUID, Long>

    fun deleted(versionVector: MutableMap<UUID, Long>): RepositoryItem

    fun updated(versionVector: MutableMap<UUID, Long>): RepositoryItem

    fun matches(projectItem: ProjectItem): Boolean

    fun matches(repositoryItem: RepositoryItem): Boolean
}

@NoArgs
class RepositoryRename(
    override val identifier: Identifier,
    override val versionVector: MutableMap<UUID, Long>,
    val newName: String?
) : RepositoryItem {
    override fun deleted(versionVector: MutableMap<UUID, Long>) = RepositoryRename(identifier, versionVector, null)

    override fun updated(versionVector: MutableMap<UUID, Long>) = RepositoryRename(identifier, versionVector, newName)

    override fun matches(projectItem: ProjectItem): Boolean {
        if (projectItem !is ProjectRename) {
            return false
        }

        return projectItem.newName == newName
    }

    override fun matches(repositoryItem: RepositoryItem): Boolean {
        if (repositoryItem !is RepositoryRename) {
            return false
        }

        return repositoryItem.newName == newName
    }

    fun convert(): ProjectRename? {
        if (newName == null) {
            return null
        }

        return ProjectRename(identifier, newName)
    }
}

@NoArgs
class RepositoryComment(
    override val identifier: Identifier,
    override val versionVector: MutableMap<UUID, Long>,
    val comment: String?,
    val style: CommentStyle?
) : RepositoryItem {
    override fun deleted(versionVector: MutableMap<UUID, Long>) =
        RepositoryComment(identifier, versionVector, null, null)

    override fun updated(versionVector: MutableMap<UUID, Long>) =
        RepositoryComment(identifier, versionVector, comment, style)

    override fun matches(projectItem: ProjectItem): Boolean {
        if (projectItem !is ProjectComment) {
            return false
        }

        return projectItem.comment == comment && projectItem.style == style
    }

    override fun matches(repositoryItem: RepositoryItem): Boolean {
        if (repositoryItem !is RepositoryComment) {
            return false
        }

        return repositoryItem.comment == comment && repositoryItem.style == style
    }

    fun convert(): ProjectComment? {
        if (comment == null || style == null) {
            assert(comment == null && style == null)  // Both or neither should be null.
            return null
        }

        return ProjectComment(identifier, comment, style)
    }
}

@NoArgs
class LocalRepository {
    var renames = mutableListOf<RepositoryRename>()
    var comments = mutableListOf<RepositoryComment>()
    val uuid = UUID.randomUUID()
}

@NoArgs
class RemoteRepository {
    var renames = mutableListOf<RepositoryRename>()
    var comments = mutableListOf<RepositoryComment>()
}

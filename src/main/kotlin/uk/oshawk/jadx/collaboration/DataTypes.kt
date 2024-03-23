package uk.oshawk.jadx.collaboration

import jadx.api.data.CodeRefType
import jadx.api.data.ICodeRename
import jadx.api.data.IJavaCodeRef
import jadx.api.data.IJavaNodeRef
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

@NoArgs
class ProjectRename(val identifier: Identifier, val newName: String) : Comparable<ProjectRename> {
    constructor(rename: ICodeRename) : this(
        Identifier(NodeRef(rename.nodeRef), rename.codeRef?.let { CodeRef(it) }),
        rename.newName
    )

    override fun compareTo(other: ProjectRename) = compareValuesBy(this, other, { it.identifier }, { it.newName })

    fun convert(): JadxCodeRename {
        return JadxCodeRename(identifier.nodeRef.convert(), identifier.codeRef?.convert(), newName)
    }
}

@NoArgs
class RepositoryRename(val identifier: Identifier, val newName: String?, val versionVector: MutableMap<UUID, Long>)

@NoArgs
class LocalRepository {
    var renames = mutableListOf<RepositoryRename>()
    val uuid = UUID.randomUUID()
}

@NoArgs
class RemoteRepository {
    var renames = mutableListOf<RepositoryRename>()
}

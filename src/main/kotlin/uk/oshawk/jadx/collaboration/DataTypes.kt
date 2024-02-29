package uk.oshawk.jadx.collaboration

import jadx.api.data.ICodeRename
import jadx.api.data.IJavaNodeRef
import jadx.api.data.impl.JadxCodeRename
import jadx.api.data.impl.JadxNodeRef
import java.util.UUID

class NodeRef(
        private val type: IJavaNodeRef.RefType,
        private val declaringClass: String,
        private val shortId: String,
) : IJavaNodeRef {
    companion object {
        val COMPARATOR: Comparator<IJavaNodeRef> = Comparator
                .comparing { obj: IJavaNodeRef -> obj.type }
                .thenComparing { obj: IJavaNodeRef -> obj.declaringClass }
                .thenComparing { obj: IJavaNodeRef -> obj.shortId }
    }

    constructor(nodeRef: IJavaNodeRef) : this(nodeRef.type, nodeRef.declaringClass, nodeRef.shortId)

    override fun getType() = type
    override fun getDeclaringClass() = declaringClass
    override fun getShortId() = shortId
    override fun compareTo(other: IJavaNodeRef?) = COMPARATOR.compare(this, other)

    fun convert(): JadxNodeRef {
        return JadxNodeRef(type, declaringClass, shortId)
    }
}

class ProjectRename(private val nodeRef: NodeRef, private val newName: String) : ICodeRename {
    companion object {
        val COMPARATOR: Comparator<ICodeRename> = Comparator
                .comparing { obj: ICodeRename -> obj.nodeRef }
                .thenComparing { obj: ICodeRename -> obj.newName }
    }

    override fun getNodeRef() = nodeRef
    override fun getCodeRef() = null
    override fun getNewName() = newName
    override fun compareTo(other: ICodeRename?) = COMPARATOR.compare(this, other)

    fun convert(): JadxCodeRename {
        return JadxCodeRename(nodeRef.convert(), newName)
    }
}

 data class RepositoryRename(val nodeRef: NodeRef, val newName: String?, val versionVector: MutableMap<UUID, Long>)

class LocalRepository {
    var renames = mutableListOf<RepositoryRename>()
    val uuid = UUID.randomUUID()
}

class RemoteRepository {
    var renames = mutableListOf<RepositoryRename>()
}

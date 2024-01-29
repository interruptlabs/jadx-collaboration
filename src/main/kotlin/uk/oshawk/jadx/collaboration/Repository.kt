package uk.oshawk.jadx.collaboration

import jadx.api.data.ICodeRename
import jadx.api.data.IJavaNodeRef

class NodeRef(nodeRef: IJavaNodeRef) : IJavaNodeRef {
    companion object {
        val COMPARATOR: Comparator<IJavaNodeRef> = Comparator
                .comparing { obj: IJavaNodeRef -> obj.type }
                .thenComparing { obj: IJavaNodeRef -> obj.declaringClass }
                .thenComparing { obj: IJavaNodeRef -> obj.shortId }
    }

    private val type: IJavaNodeRef.RefType = nodeRef.type
    private val declaringClass: String = nodeRef.declaringClass
    private val shortId: String = nodeRef.shortId

    override fun getType() = type
    override fun getDeclaringClass() = declaringClass
    override fun getShortId() = shortId
    override fun compareTo(other: IJavaNodeRef?) = COMPARATOR.compare(this, other)
}

class ProjectRename(private val nodeRef: NodeRef, private val newName: String): ICodeRename {
    companion object {
        val COMPARATOR: Comparator<ICodeRename> = Comparator
                .comparing { obj: ICodeRename -> obj.codeRef }
                .thenComparing { obj: ICodeRename -> obj.newName }
    }

    override fun getNodeRef() = nodeRef
    override fun getCodeRef() = null
    override fun getNewName() = newName
    override fun compareTo(other: ICodeRename?) = COMPARATOR.compare(this, other)

}

data class LocalRename(
        val nodeRef: NodeRef,
        val newName: String?,
        val lastPullNewName: String?
)
class LocalRepository {
    var renames = mutableListOf<LocalRename>()
}

data class RemoteRename(val nodeRef: NodeRef, val newName: String?)
class RemoteRepository {
    var renames = mutableListOf<RemoteRename>()
}

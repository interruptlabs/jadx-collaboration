package uk.oshawk.jadx.collaboration

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.data.IJavaNodeRef
import jadx.api.data.impl.JadxCodeData
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.events.IJadxEvents
import jadx.api.plugins.gui.JadxGuiContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.mockito.kotlin.*
import java.nio.file.Path
import kotlin.io.path.*

class PluginMockery(conflictResolver: (context: JadxPluginContext, remote: RepositoryItem, local: RepositoryItem) -> Boolean?) {
    val jadxCodeData = JadxCodeData()
    val jadxArgs = mock<JadxArgs> {
        on { codeData } doReturn jadxCodeData
    }

    val jadxDecompiler = mock<JadxDecompiler> {
        on { classes } doReturn listOf()
    }

    val iJadxEvents = mock<IJadxEvents> {
        doNothing().on { send(any()) }  // Don't like that this has a different syntax.
    }

    var pull: Runnable? = null
    var push: Runnable? = null
    val jadxGuiContext = mock<JadxGuiContext> {
        on { addMenuAction(any(), any()) } doAnswer {
            val name = it.getArgument<String>(0)
            val action = it.getArgument<Runnable>(1)
            when (name) {
                "Pull" -> {
                    pull = action
                }

                "Push" -> {
                    push = action
                }
            }
        }
        on { uiRun(any()) } doAnswer { it.getArgument<Runnable>(0).run() }
    }

    var options: Options? = null
    val jadxPluginContext = mock<JadxPluginContext> {
        on { args } doReturn jadxArgs
        on { decompiler } doReturn jadxDecompiler
        on { events() } doReturn iJadxEvents
        on { guiContext } doReturn jadxGuiContext
        on { registerOptions(any()) } doAnswer { options = it.getArgument<Options>(0) }
    }

    val plugin = Plugin(conflictResolver)

    init {
        plugin.init(jadxPluginContext)
    }

    var renames: List<ProjectRename>
        get() = jadxArgs.codeData.renames
            .map { ProjectRename(it) }
            .sortedBy { it.identifier }  // For comparison.
        set(value) {
            (jadxArgs.codeData as JadxCodeData).renames = value.map { it.convert() }
        }
}

class RepositoryMockery(
    conflictResolver: (context: JadxPluginContext, remote: RepositoryItem, local: RepositoryItem) -> Boolean? = { _, _, _ ->
        fail(
            "Conflict!"
        )
    }
) {
    val leftDirectory = createTempDirectory("left")
    val leftRemote = Path(leftDirectory.toString(), "repository")
    val leftLocal = Path(leftDirectory.toString(), "repository.local")

    val rightDirectory = createTempDirectory("right")
    val rightRemote = Path(rightDirectory.toString(), "repository")
    val rightLocal = Path(rightDirectory.toString(), "repository.local")

    val remoteDirectory = createTempDirectory("remote")
    val remote = Path(remoteDirectory.toString(), "repository")

    val leftPlugin = PluginMockery(conflictResolver)
    val rightPlugin = PluginMockery(conflictResolver)

    init {
        // On some platforms, directories seem to be reused.
        leftRemote.deleteIfExists()
        leftLocal.deleteIfExists()
        rightRemote.deleteIfExists()
        rightLocal.deleteIfExists()
        remote.deleteIfExists()
    }

    init {
        leftPlugin.options!!.repository = leftRemote.toString()
        rightPlugin.options!!.repository = rightRemote.toString()
    }

    private fun copy(from: Path, to: Path) {
        Plugin.LOG.info { "copy: $from to $to" }

        if (from.exists()) {
            from.copyTo(to, true)
        } else {
            to.deleteIfExists()
        }
    }

    fun leftToRemote() = copy(leftRemote, remote)
    fun rightToRemote() = copy(rightRemote, remote)

    fun remoteToLeft() = copy(remote, leftRemote)
    fun remoteToRight() = copy(remote, rightRemote)

    fun leftPull() {
        remoteToLeft()
        leftPlugin.pull!!.run()
    }

    fun rightPull() {
        remoteToRight()
        rightPlugin.pull!!.run()
    }

    fun leftPush() {
        remoteToLeft()
        leftPlugin.push!!.run()
        leftToRemote()
    }

    fun rightPush() {
        remoteToRight()
        rightPlugin.push!!.run()
        rightToRemote()
    }
}

class PluginTest {
    fun genRename(i: Int) = ProjectRename(Identifier(NodeRef(IJavaNodeRef.RefType.CLASS, "a$i", "b$i"), null), "c$i")

    fun modRename(rename: ProjectRename) = ProjectRename(rename.identifier, "${rename.newName}m")

    fun assertProjectRenamesEqual(left: Iterable<ProjectRename>, right: Iterable<ProjectRename>) {
        val leftIterator = left.iterator()
        val rightIterator = right.iterator()
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            val leftNext = leftIterator.next()
            val rightNext = rightIterator.next()
            assertEquals(0, leftNext.identifier.compareTo(rightNext.identifier), "Element mismatch in iterator.")
            assertEquals(leftNext.newName, rightNext.newName, "Element mismatch in iterator.")
        }

        assertFalse(leftIterator.hasNext(), "Size mismatch in iterator (left > right).")
        assertFalse(rightIterator.hasNext(), "Size mismatch in iterator (left < right).")
    }

    @Test
    fun basic0() {
        // l.set([0, 1])
        // l.push()
        // r.pull()
        // assert(l == [0, 1])
        // assert(l == r)

        val mockery = RepositoryMockery()

        mockery.leftPlugin.renames = listOf(genRename(0), genRename(1))
        mockery.leftPush()

        mockery.rightPull()

        assertProjectRenamesEqual(mockery.leftPlugin.renames, listOf(genRename(0), genRename(1)))
        assertProjectRenamesEqual(mockery.leftPlugin.renames, mockery.rightPlugin.renames)
    }

    @Test
    fun basic1() {
        // l.set([0])
        // l.push()
        // r.set([1])
        // r.push()
        // assert(l == [0])
        // assert(r == [0, 1])
        // l.pull()
        // assert(l == [0, 1])
        // assert(l == r)

        val mockery = RepositoryMockery()

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()

        mockery.rightPlugin.renames = listOf(genRename(1))
        mockery.rightPush()

        assertProjectRenamesEqual(mockery.leftPlugin.renames, listOf(genRename(0)))
        assertProjectRenamesEqual(mockery.rightPlugin.renames, listOf(genRename(0), genRename(1)))

        mockery.leftPull()

        assertProjectRenamesEqual(mockery.leftPlugin.renames, listOf(genRename(0), genRename(1)))
        assertProjectRenamesEqual(mockery.leftPlugin.renames, mockery.rightPlugin.renames)
    }

    @Test
    fun basic2() {
        // l.set([0])
        // l.push()
        // l.set([0m])
        // l.push()
        // r.pull()
        // assert(l == [0m])
        // assert(l == r)

        val mockery = RepositoryMockery()

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()

        mockery.leftPlugin.renames = listOf(modRename(genRename(0)))
        mockery.leftPush()

        mockery.rightPull()

        assertProjectRenamesEqual(mockery.leftPlugin.renames, listOf(modRename(genRename(0))))
        assertProjectRenamesEqual(mockery.leftPlugin.renames, mockery.rightPlugin.renames)
    }

    //
    // The following test conflicts. Conflicts should only occur where indicated.
    //

    @Test
    fun conflict0() {
        // l.set([0])
        // l.push()
        // r.push()
        // l.set([0m])
        // l.push()
        // r.pull()

        val mockery = RepositoryMockery()

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()

        mockery.rightPush()

        mockery.leftPlugin.renames = listOf(modRename(genRename(0)))
        mockery.leftPush()

        mockery.rightPull()
    }

    @Test
    fun conflict1() {
        // l.set([0])
        // l.push()
        // assert(conflicts == 0)
        // r.set([0m])
        // r.push()  // CONFLICT
        // assert(conflicts == 1)

        var conflicts = 0

        val mockery = RepositoryMockery { _, _, _ ->
            conflicts++
            true
        }

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()
        assertEquals(0, conflicts)

        mockery.rightPlugin.renames = listOf(modRename(genRename(0)))
        mockery.rightPush()
        assertEquals(1, conflicts)
    }

    @Test
    fun conflict2() {
        // l.set([0])
        // l.push()
        // assert(conflicts == 0)
        // r.set([0m])
        // r.push()  // CONFLICT, CHOOSE R
        // assert(conflicts == 1)
        // l.push()
        // assert(conflicts == 1)

        var conflicts = 0

        val mockery = RepositoryMockery { _, _, _ ->
            conflicts++
            false
        }

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()
        assertEquals(0, conflicts)

        mockery.rightPlugin.renames = listOf(modRename(genRename(0)))
        mockery.rightPush()
        assertEquals(1, conflicts)

        mockery.leftPush()
        assertEquals(1, conflicts)
    }

    @Test
    fun conflict3() {
        // l.set([0])
        // l.push()
        // assert(conflicts == 0)
        // r.set([0m])
        // r.push()  // CONFLICT, CHOOSE R
        // assert(conflicts == 1)
        // l.set([0mm])
        // l.push()  // CONFLICT
        // assert(conflicts == 2)

        var conflicts = 0

        val mockery = RepositoryMockery { _, _, _ ->
            conflicts++
            false
        }

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()
        assertEquals(0, conflicts)

        mockery.rightPlugin.renames = listOf(modRename(genRename(0)))
        mockery.rightPush()
        assertEquals(1, conflicts)

        mockery.leftPlugin.renames = listOf(modRename(modRename(genRename(0))))
        mockery.leftPush()
        assertEquals(2, conflicts)
    }

    @Test
    fun conflict4() {
        // l.set([0])
        // l.push()
        // assert(conflicts == 0)
        // r.set([0m])
        // r.push()  // CONFLICT, CHOOSE L
        // assert(conflicts == 1)
        // l.set([0mm])
        // l.push()
        // assert(conflicts == 1)

        var conflicts = 0

        val mockery = RepositoryMockery { _, _, _ ->
            conflicts++
            true
        }

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()
        assertEquals(0, conflicts)

        mockery.rightPlugin.renames = listOf(modRename(genRename(0)))
        mockery.rightPush()
        assertEquals(1, conflicts)

        mockery.leftPlugin.renames = listOf(modRename(modRename(genRename(0))))
        mockery.leftPush()
        assertEquals(1, conflicts)
    }

    @Test
    fun conflict5() {
        // l.set([0])
        // l.push()
        // assert(conflicts == 0)
        // r.set([0m])
        // r.push()  // CONFLICT, CHOOSE R
        // assert(conflicts == 1)
        // l.set([0m])
        // l.push()
        // assert(conflicts == 1)

        var conflicts = 0

        val mockery = RepositoryMockery { _, _, _ ->
            conflicts++
            false
        }

        mockery.leftPlugin.renames = listOf(genRename(0))
        mockery.leftPush()
        assertEquals(0, conflicts)

        mockery.rightPlugin.renames = listOf(modRename(genRename(0)))
        mockery.rightPush()
        assertEquals(1, conflicts)

        mockery.leftPlugin.renames = listOf(modRename(genRename(0)))
        mockery.leftPush()
        assertEquals(1, conflicts)
    }
}

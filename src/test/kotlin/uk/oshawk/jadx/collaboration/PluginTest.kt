package uk.oshawk.jadx.collaboration

import jadx.api.JadxArgs
import jadx.api.data.ICodeRename
import jadx.api.data.IJavaNodeRef
import jadx.api.data.impl.JadxCodeData
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.events.IJadxEvents
import jadx.api.plugins.gui.JadxGuiContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.nio.file.Path
import kotlin.io.path.*

class PluginMockery {
    val jadxCodeData = JadxCodeData()
    val jadxArgs = mock<JadxArgs> {
        on { codeData } doReturn jadxCodeData
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
                "Pull" -> { pull = action }
                "Push" -> { push = action }
            }
        }
        on { uiRun(any()) } doAnswer { it.getArgument<Runnable>(0).run() }
    }

    var options: Options? = null
    val jadxPluginContext = mock<JadxPluginContext> {
        on { args } doReturn jadxArgs
        on { events() } doReturn iJadxEvents
        on { guiContext } doReturn jadxGuiContext
        on { registerOptions(any()) } doAnswer { options = it.getArgument<Options>(0) }
    }

    val plugin = Plugin()

    init {
        plugin.init(jadxPluginContext)
    }

    var renames: List<ICodeRename>
        get() = jadxArgs.codeData.renames.sorted()  // For comparison.
        set(value) { (jadxArgs.codeData as JadxCodeData).renames = value }
}

class RepositoryMockery {
    val leftDirectory = createTempDirectory()
    val leftRemote = Path(leftDirectory.toString(), "repository")
    val leftLocal = Path(leftDirectory.toString(), "repository.local")

    val rightDirectory = createTempDirectory()
    val rightRemote = Path(rightDirectory.toString(), "repository")
    val rightLocal = Path(rightDirectory.toString(), "repository.local")

    val remoteDirectory = createTempDirectory()
    val remote = Path(remoteDirectory.toString(), "repository")

    val leftPlugin = PluginMockery()
    val rightPlugin = PluginMockery()

    init {
        leftPlugin.options!!.repository = leftRemote.toString()
        rightPlugin.options!!.repository = rightRemote.toString()
    }

    private fun copy(from: Path, to: Path) {
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
    fun genRename(i: Int) = ProjectRename(NodeRef(IJavaNodeRef.RefType.CLASS, "a$i", "b$i"), "c$i")

    fun <T: Comparable<T>> assertIterableCompareTo0(left: Iterable<T>, right: Iterable<T>) {

        val leftIterator = left.iterator()
        val rightIterator = right.iterator()

        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            assertEquals(leftIterator.next().compareTo(rightIterator.next()), 0, "Element mismatch in iterator.")
        }

        assertFalse(leftIterator.hasNext(), "Size mismatch in iterator (left > right).")
        assertFalse(rightIterator.hasNext(), "Size mismatch in iterator (left < right).")
    }

    @Test
    fun test0() {
        // l.set([0, 1])
        // l.push()
        // r.pull()
        // assert(l == [0, 1])
        // assert(l == r)

        val mockery = RepositoryMockery()

        mockery.leftPlugin.renames = listOf(genRename(0), genRename(1))

        mockery.leftPush()
        mockery.rightPull()

        assertIterableCompareTo0(mockery.leftPlugin.renames, listOf(genRename(0), genRename(1)))
        assertIterableCompareTo0(mockery.leftPlugin.renames, mockery.rightPlugin.renames)
    }

    @Test
    fun test1() {
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

        assertIterableCompareTo0(mockery.leftPlugin.renames, listOf(genRename(0)))
        assertIterableCompareTo0(mockery.rightPlugin.renames, listOf(genRename(0), genRename(1)))

        mockery.leftPull()

        assertIterableCompareTo0(mockery.leftPlugin.renames, listOf(genRename(0), genRename(1)))
        assertIterableCompareTo0(mockery.leftPlugin.renames, mockery.rightPlugin.renames)
    }
}
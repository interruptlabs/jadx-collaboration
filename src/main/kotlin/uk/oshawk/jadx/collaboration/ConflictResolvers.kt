package uk.oshawk.jadx.collaboration

import jadx.api.plugins.JadxPluginContext
import jadx.gui.plugins.context.GuiPluginContext
import jadx.gui.ui.MainWindow
import java.awt.GridLayout
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.math.max
import kotlin.math.min

const val WRAP = 20

fun getMainWindow(context: JadxPluginContext): MainWindow {
    return (context.guiContext as GuiPluginContext).commonContext.mainWindow
}

fun useLocalConflictResolver(context: JadxPluginContext, remote: RepositoryItem, local: RepositoryItem): Boolean {
    return false
}

class ConflictModal(parent: JFrame, remote: RepositoryItem, local: RepositoryItem) :
    JDialog(parent, "Conflict!", true) {
    var result: Boolean? = null

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE

        contentPane = Box.createVerticalBox()

        val boldFont = SimpleAttributeSet()
        StyleConstants.setFontFamily(boldFont, "Monospaced")
        StyleConstants.setBold(boldFont, true)

        val normalFont = SimpleAttributeSet()
        StyleConstants.setFontFamily(normalFont, "Monospaced")
        StyleConstants.setBold(normalFont, false)

        val remoteChanges = mutableListOf<Pair<String, String>>()
        val localChanges = mutableListOf<Pair<String, String>>()
        when {
            remote is RepositoryRename && local is RepositoryRename -> {
                remoteChanges.add(Pair("New Name:", "${remote.newName}"))
                localChanges.add(Pair("New Name:", "${local.newName}"))
            }

            remote is RepositoryComment && local is RepositoryComment -> {
                remoteChanges.add(Pair("Comment:", "${remote.comment}"))
                localChanges.add(Pair("Comment:", "${local.comment}"))

                remoteChanges.add(Pair("Style:", "${remote.style}"))
                localChanges.add(Pair("Style:", "${local.style}"))
            }

            else -> {
                check(false) { "UNEXPECTED - Type mismatch between local and remote." }
            }
        }

        var width = 0
        width = max(width, remote.identifier.nodeRef.declaringClass.length)
        width = max(width, remote.identifier.nodeRef.shortId?.length ?: 4)
        for ((key, value) in remoteChanges + localChanges) {
            width = max(width, key.length * 2 + 3)
            width = max(width, min(value.length, WRAP) * 2 + 3) // Wrap after WRAP characters.
        }
        width = width or 1  // Make odd for equal width columns.

        val text = JTextPane()

        text.document.insertString(text.document.length, "Type:\n", boldFont)
        text.document.insertString(text.document.length, "${remote.identifier.nodeRef.type}\n", normalFont)
        text.document.insertString(text.document.length, "Declaring Class:\n", boldFont)
        text.document.insertString(text.document.length, "${remote.identifier.nodeRef.declaringClass}\n", normalFont)
        text.document.insertString(text.document.length, "Short ID:\n", boldFont)
        text.document.insertString(text.document.length, "${remote.identifier.nodeRef.shortId}\n", normalFont)
        text.document.insertString(text.document.length, "Attach Type:\n", boldFont)
        text.document.insertString(text.document.length, "${remote.identifier.codeRef?.attachType}\n", normalFont)
        text.document.insertString(text.document.length, "Index:\n", boldFont)
        text.document.insertString(text.document.length, "${remote.identifier.codeRef?.index}\n", normalFont)

        text.document.insertString(text.document.length, "-".repeat(width) + "\n", normalFont)
        for ((remoteChange, localChange) in remoteChanges zip localChanges) {
            val (remoteKey, remoteValue) = remoteChange
            val (localKey, localValue) = localChange

            text.document.insertString(text.document.length, remoteKey, boldFont)
            text.document.insertString(
                text.document.length,
                " ".repeat(width / 2 - remoteKey.length) + "| ",
                normalFont
            )
            text.document.insertString(text.document.length, "${localKey}\n", boldFont)

            var remainingRemoteValue = remoteValue
            var remainingLocalValue = localValue
            while (remainingRemoteValue.isNotEmpty() || remainingLocalValue.isNotEmpty()) {
                val currentRemoteValue = remainingRemoteValue.take(WRAP)
                val currentLocalValue = remainingLocalValue.take(WRAP)
                remainingRemoteValue = remainingRemoteValue.drop(WRAP)
                remainingLocalValue = remainingLocalValue.drop(WRAP)
                text.document.insertString(
                    text.document.length,
                    currentRemoteValue + " ".repeat(width / 2 - currentRemoteValue.length) + "| $currentLocalValue\n",
                    normalFont
                )
            }

        }

        add(text)

        val remoteButton = JButton("Remote")
        remoteButton.addActionListener {
            result = true
            dispose()
        }

        val localButton = JButton("Local")
        localButton.addActionListener {
            result = false
            dispose()
        }

        val buttons = JPanel(GridLayout(1, 2))
        buttons.add(remoteButton)
        buttons.add(localButton)
        add(buttons)

        pack()

        setLocationRelativeTo(parent)
    }

    fun showModal(): Boolean? {
        isVisible = true
        return result
    }
}

fun dialogConflictResolver(context: JadxPluginContext, remote: RepositoryItem, local: RepositoryItem): Boolean? {
    return ConflictModal(getMainWindow(context), remote, local).showModal()
}

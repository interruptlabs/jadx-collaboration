package uk.oshawk.jadx.collaboration

import jadx.api.plugins.JadxPluginContext
import java.awt.GridLayout
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.math.max

fun getMainWindow(context: JadxPluginContext): JFrame {
    // TODO: This needs to be replaced. It is a travesty.

    val commonContextField =
        Class.forName("jadx.gui.plugins.context.GuiPluginContext").getDeclaredField("commonContext")
    commonContextField.isAccessible = true
    val commonContext = commonContextField.get(context.guiContext)

    val mainWindowField =
        Class.forName("jadx.gui.plugins.context.CommonGuiPluginsContext").getDeclaredField("mainWindow")
    mainWindowField.isAccessible = true
    val mainWindow = mainWindowField.get(commonContext) as JFrame

    return mainWindow
}

fun useLocalConflictResolver(context: JadxPluginContext, remote: RepositoryRename, local: RepositoryRename): Boolean {
    return false
}

class ConflictModal(parent: JFrame, remote: RepositoryRename, local: RepositoryRename) :
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

        val remoteNewName = remote.newName ?: "NULL"
        val localNewName = local.newName ?: "NULL"

        var width = 21
        width = max(width, remote.identifier.nodeRef.declaringClass.length)
        width = max(width, remote.identifier.nodeRef.shortId?.length ?: 4)
        width = max(width, max(remoteNewName.length, localNewName.length) * 2 + 3)
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
        text.document.insertString(text.document.length, "New Name:", boldFont)
        text.document.insertString(text.document.length, " ".repeat(width / 2 - 9) + "| ", normalFont)
        text.document.insertString(text.document.length, "New Name:\n", boldFont)
        text.document.insertString(
            text.document.length,
            remoteNewName + " ".repeat(width / 2 - remoteNewName.length) + "| $localNewName",
            normalFont
        )

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

fun dialogConflictResolver(context: JadxPluginContext, remote: RepositoryRename, local: RepositoryRename): Boolean? {
    return ConflictModal(getMainWindow(context), remote, local).showModal()
}


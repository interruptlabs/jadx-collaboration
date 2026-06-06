package uk.oshawk.jadx.collaboration

import io.github.oshai.kotlinlogging.KLogger
import jadx.api.plugins.JadxPluginContext
import jadx.gui.ui.MainWindow
import java.security.MessageDigest
import kotlin.io.path.pathString

fun projectSaver(context: JadxPluginContext, logger: KLogger): String? {
    val mainWindow = getMainWindow(context)

    val saveProject = MainWindow::class.java.getDeclaredMethod("saveProject")
    saveProject.isAccessible = true

    try {
        saveProject.invoke(mainWindow)
    } catch (e: Exception) {
        logger.info { e }
        logger.error { "Error saving JADX project." }
        return null
    }

    val projectPath = mainWindow.project.projectPath ?: run {
        logger.error { "JADX project needs to be saved." }
        return null
    }

    return MessageDigest.getInstance("SHA-256").digest(projectPath.pathString.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

fun doNothingSaver(context: JadxPluginContext, logger: KLogger): String? {
    return "0000000000000000000000000000000000000000000000000000000000000000"
}

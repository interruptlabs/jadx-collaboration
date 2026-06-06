package uk.oshawk.jadx.collaboration

import io.github.oshai.kotlinlogging.KLogger
import jadx.api.plugins.JadxPluginContext
import jadx.gui.ui.MainWindow

fun projectSaver(context: JadxPluginContext, logger: KLogger): Unit? {
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

    if (!mainWindow.project.isSaveFileSelected) {
        logger.error { "JADX project needs to be saved." }
        return null
    }

    return Unit
}

fun doNothingSaver(context: JadxPluginContext, logger: KLogger): Unit? {
    return Unit
}

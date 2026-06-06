package uk.oshawk.jadx.collaboration

import jadx.api.plugins.JadxPluginContext
import jadx.gui.plugins.context.GuiPluginContext
import jadx.gui.ui.MainWindow

fun getMainWindow(context: JadxPluginContext): MainWindow {
    return (context.guiContext as GuiPluginContext).commonContext.mainWindow
}

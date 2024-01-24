package uk.oshawk.jadx.collaboration

import jadx.api.plugins.JadxPlugin
import jadx.api.plugins.JadxPluginContext
import jadx.api.plugins.JadxPluginInfo

class JadxCollaborationPlugin : JadxPlugin {
	companion object {
		const val ID = "jadx-collaboration"
	}

	private val options = JadxCollaborationOptions()

	override fun getPluginInfo() = JadxPluginInfo(ID, "JADX Collaboration", "Collaboration support for JADX")

	override fun init(context: JadxPluginContext?) {
		context?.registerOptions(options)
	}
}

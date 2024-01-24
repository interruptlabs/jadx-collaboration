package uk.oshawk.jadx.collaboration

import jadx.api.plugins.options.OptionFlag
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder

class JadxCollaborationOptions : BasePluginOptionsBuilder() {
	var repo = ""
	var prePush = ""
	var prePull = ""

	override fun registerOptions() {
		strOption("${JadxCollaborationPlugin.ID}.repo")
			.description("Path to the repository file.")
			.defaultValue("")
			.setter { v -> repo = v }
			.flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE)
		strOption("${JadxCollaborationPlugin.ID}.pre-pull")
			.description("Path to the pre-pull script.")
			.defaultValue("")
			.setter { v -> prePull = v }
			.flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE)
		strOption("${JadxCollaborationPlugin.ID}.pre-push")
			.description("Path to the pre-push script.")
			.defaultValue("")
			.setter { v -> prePush = v }
			.flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE)
	}

}

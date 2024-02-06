package uk.oshawk.jadx.collaboration

import jadx.api.plugins.options.OptionFlag
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder

class Options : BasePluginOptionsBuilder() {
    var repository = ""
    var prePull = ""
    var postPush = ""

    override fun registerOptions() {
        strOption("${Plugin.ID}.repository")
                .description("Path to the repository file.")
                .defaultValue("")
                .setter { v -> repository = v }
                .flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE)
        strOption("${Plugin.ID}.pre-pull")
                .description("Path to the pre-pull script.")
                .defaultValue("")
                .setter { v -> prePull = v }
                .flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE)
        strOption("${Plugin.ID}.post-push")
                .description("Path to the post-push script.")
                .defaultValue("")
                .setter { v -> postPush = v }
                .flags(OptionFlag.PER_PROJECT, OptionFlag.NOT_CHANGING_CODE)
    }

}

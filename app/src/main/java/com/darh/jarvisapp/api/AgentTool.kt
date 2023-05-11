package com.darh.jarvisapp.api

interface AgentTool{
    val name : String
    abstract fun getPrompt(): String

    fun baseStructure(description: String, input: String) =
        "tool:\n" +
                "$name - $description\n" +
                "format:\n" +
                "$name: $input"
}

object GoogleSearch : AgentTool{
    override val name = "google_search"
    override fun getPrompt(): String {
        return baseStructure(
            description = "A tool that search Google and summarize the findings",
            input = "The information you want to get. asked a clear question."
        )
    }
}

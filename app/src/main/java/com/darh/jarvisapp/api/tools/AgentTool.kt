package com.darh.jarvisapp.api.tools

enum class AgentTool(val toolName: String, val description: String, val input: String) {
    GoogleSearch(
        "google_search",
        "A tool that search Google and summarize the findings",
        "The information you want to get. asked a clear question."
    ),
    FinalAnswer(
        "final_answer",
        "A tool to give the final answer when it's ready.",
        "The final answer to the task."
    ),
    AskUser(
        "ask_user",
        "A tool that asks the user for further clarification about the task, it can be used to chose between different suggested options or just to clarify the request",
        "The information you want to get. asked as a clear question."
    );

    fun getPrompt(): String {
        return baseStructure(description, input)
    }

    fun baseStructure(description: String, input: String) =
        "tool:\n" +
                "$toolName - $description\n" +
                "format:\n" +
                "$toolName: $input"
}

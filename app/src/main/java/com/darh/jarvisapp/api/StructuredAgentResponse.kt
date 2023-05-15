package com.darh.jarvisapp.api

import androidx.annotation.Keep
import com.darh.jarvisapp.api.tools.AgentTool
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Keep
data class StructuredAgentResponse(
    @SerializedName(THOUGHT_FIELD)
    val thought: String? = null,
    @SerializedName(NEXT_TOOL_FIELD)
    val nextAction: String? = null,
) {
    companion object {

        internal const val THOUGHT_FIELD = "thought"
        const val NEXT_TOOL_FIELD = "next_action_tool"

        // The order of the metadata fields determines the order of their availability with response streaming.
        private fun jsonSchema(): String {
            return "{\n" +
                    " \"$THOUGHT_FIELD\": \"string (evaluation of the task and a plan for next steps or the solution if the task is complete)\",\n" +
                    " \"$NEXT_TOOL_FIELD\": string? (The next tool to be used in order to complete our plan. The tool must be one of the provided \"action_tools\" and follow the format.)\n"+
                    "}"
        }

        fun systemSetup() =
            "You are Jarvis, an AI personal assistant agent built with LLM that runs on an Android app.\n" +
                    "The time now is: ${getCurrentDateTimeText()}.\n" +
        "You job is to complete tasks or answer questions by chaining actions using the \"tools\" that will be provided next. You cannot use tools that are not provided in the list.\n"

//                    "If you cannot perform the user request specify what additional abilities I can give you.\n" +
//                    "These are the tasks within your abilities:\n" +
//                    "\"reminder\" - format: {\"name\": \"reminder\", \"delay\":int (minutes from now)},\n" +
//                    "\"google_search\" - format: {\"name\": \"google_search\", \"query\": \"string\"(the google search query)}, usage: fetch up to date info and background knowledge to answer questions or perform tasks you can't answer with high certainty\n\n" +
//                    "\""

        fun structureSetup(tools: List<AgentTool>, originalTask: String) =
            "Your next response must be in the 'BASE' JSON schema that'll be provided next.\n" +
                    "The '$THOUGHT_FIELD' field is your place to think about the task - describe the task and make a plan how to accomplish it.\n" +
                    "If a follow up action is needed, the \"$NEXT_TOOL_FIELD\" should be added and must be a string in the following format:\n" +
                    "\"$NEXT_TOOL_FIELD\": \"tool_name: tool input text\"\n" +
                    "The available \"action_tools\" to be used under the \"$NEXT_TOOL_FIELD\" field are:\n" +
                    "${tools.joinToString(separator = ",\n", transform = {it.getPrompt()})}\n\n" +
                    "The message must contain the 'BASE' JSON only without additional text.\n" +
                    "The time now is: ${getCurrentDateTimeText()}.\n" +
                    "The original task is: $originalTask\n\n" +
                    "CRITICAL!: Your next response must be complete in the 'BASE' JSON schema:\n${jsonSchema()}\n\n" +
                    "For example for a user saying he wants to eat good sushi tonight in Tel aviv but wants to save money, the response can be:\n" +
                    "{\n" +
                    "\"$THOUGHT_FIELD\": \"I need to look for recommendations of good Sushi restaurants that are cheap in Tel Aviv for tonight. Let's start by search google for a list of Sushi recommendations in Tel Aviv, then find their prices and compare the results to find the optimal match.\",\n" +
                    "\"$NEXT_TOOL_FIELD\": \"google_search: What are the best sushi restaurants in Tel Aviv?\"\n" +
                    "}"

        private fun getCurrentDateTimeText() =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))


    }

}

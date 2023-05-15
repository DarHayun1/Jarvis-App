package com.darh.jarvisapp.api

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Keep
data class StructuredChatResponse(
    @SerializedName(PROMPT_FIELD)
    val prompt: String? = null,
    @SerializedName(NEXT_ACTION_FIELD)
    val nextActions: List<String>? = null,
    @SerializedName(ASK_GOOGLE_FIELD)
    val askGoogle: String? = null,
    @SerializedName(QUESTION_SUMMARY_FIELD)
    val questionSummary: String? = null,
    @SerializedName(StructuredAgentResponse.THOUGHT_FIELD)
    val thought: String? = null,
    @SerializedName(StructuredAgentResponse.NEXT_TOOL_FIELD)
    val nextTool: String? = null,
) {

    companion object {

        internal const val PROMPT_FIELD = "prompt"
        const val NEXT_ACTION_FIELD = "next_actions"

        //        private const val ASSISTANT_ACTIONS = "assistant_actions"
        internal const val ASK_GOOGLE_FIELD = "ask_google"
        internal const val QUESTION_SUMMARY_FIELD = "question_summary"

        // The order of the metadata fields determines the order of their availability with response streaming.
        private fun jsonSchema(fields: List<String>): String {
            val mappedFields = listOf(PROMPT_FIELD).plus(fields)
                .plus(listOf(ASK_GOOGLE_FIELD, QUESTION_SUMMARY_FIELD)).map {
                when (it) {
                    PROMPT_FIELD -> " \"$PROMPT_FIELD\": \"string (The user facing message).\""
                    NEXT_ACTION_FIELD -> " \"$NEXT_ACTION_FIELD\": [string] (3-5 suggestions (max total 100 chars) for short user follow up messages to your prompt (from the user's perspective).)"
                    ASK_GOOGLE_FIELD -> " \"$ASK_GOOGLE_FIELD\": string? (Only if needed. search query to get more information from google.)"
                    QUESTION_SUMMARY_FIELD -> " \"$QUESTION_SUMMARY_FIELD\":string? (Only if $ASK_GOOGLE_FIELD requested. A short description of the question we are trying to answer.)"
                    else -> ""
                }
            }
            return "{\n" +
                    "${mappedFields.joinToString(separator = ",\n")}\n" +
                    "}"
        }

        fun systemSetup() =
            "You are Jarvis, the user's personal assistant that runs on an Android app. The user is also the System (me), my name is Dar.\n" +
                    "You are welcome to suggest improvement I can add to our app in the end of you prompts." +
                    "The time now is: ${getCurrentDateTimeText()}."

//                    "If you cannot perform the user request specify what additional abilities I can give you.\n" +
//                    "These are the tasks within your abilities:\n" +
//                    "\"reminder\" - format: {\"name\": \"reminder\", \"delay\":int (minutes from now)},\n" +
//                    "\"google_search\" - format: {\"name\": \"google_search\", \"query\": \"string\"(the google search query)}, usage: fetch up to date info and background knowledge to answer questions or perform tasks you can't answer with high certainty\n\n" +
//                    "\""

        fun structureSetup(fields: List<String>) =
            "Your next response must be in the 'BASE' JSON schema that'll be provided next.\n" +
                    "The 'prompt' field is your response to the user and should be a complete text unless specified otherwise by the system.\n" +
//                    "Use HTML bold tags (<b>...</b>) in the prompt message to mark at least 3 topic-related important terms in your message.\n" +
                    "Use escaped line breaks generously for better formatting in your prompts.\n" +
                    "The message must contain the 'BASE' JSON only without additional text.\n" +
                    "If you are not sure on the answer or the answer related to information after the year 2020 (now it's 2023 but you are trained until 2021) do:\n" +
                    " 1. Put in the \"$PROMPT_FIELD\" field: A message notifying the user that you are looking into it.\n" +
                    " 2. Add to \"$ASK_GOOGLE_FIELD\" a good query for google search to get the required information.\n" +
                    " 3. Add to \"$QUESTION_SUMMARY_FIELD\" a short description of the problem we are trying to answer.\n" +
                    "The time now is: ${getCurrentDateTimeText()}.\n" +
                    "CRITICAL!: Don't stop after the prompt! Your next response must be complete in the 'BASE' pseudo JSON SCHEMA:\n${jsonSchema(fields)}"

        private fun getCurrentDateTimeText() =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))


    }
}

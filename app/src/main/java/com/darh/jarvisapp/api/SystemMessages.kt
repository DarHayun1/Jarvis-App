package com.darh.jarvisapp.api

import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole

object SystemMessages {

    fun chatSystemSetup() = listOf(
        ChatMessage(
            role = ChatRole.System,
            content = StructuredChatResponse.systemSetup()
        )
    )

    fun chatStructureMethod(requestedFields: List<String>): ChatMessage {
        val fields = listOf(StructuredChatResponse.PROMPT_FIELD).plus(requestedFields)
        .plus(listOf(StructuredChatResponse.ASK_GOOGLE_FIELD, StructuredChatResponse.QUESTION_SUMMARY_FIELD))
        return ChatMessage(
            role = ChatRole.System,
            content = StructuredChatResponse.structureSetup(fields)
        )
    }

    fun agentStructureMethod(requestedFields: List<String>): ChatMessage {

    }

}
package com.darh.jarvisapp.api

import com.darh.jarvisapp.api.tools.AgentTool
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole

object SystemMessages {

    fun chatSystemSetup() = listOf(
        ChatMessage(
            role = ChatRole.System,
            content = StructuredChatResponse.systemSetup()
        )
    )

    fun agentSystemSetup() = listOf(
        ChatMessage(
            role = ChatRole.System,
            content = StructuredAgentResponse.systemSetup()
        )
    )

    fun chatStructureMethod(requestedFields: List<String>): ChatMessage {
        val fields = requestedFields
        .plus(listOf(StructuredChatResponse.ASK_GOOGLE_FIELD, StructuredChatResponse.QUESTION_SUMMARY_FIELD))
        return ChatMessage(
            role = ChatRole.System,
            content = StructuredChatResponse.structureSetup(fields)
        )
    }

    fun agentStructureMethod(originalTask: String): ChatMessage {
        return ChatMessage(
            role = ChatRole.System,
            content = StructuredAgentResponse.structureSetup(listOf(AgentTool.GoogleSearch,
                AgentTool.AskUser
            ), originalTask)
        )
    }

}
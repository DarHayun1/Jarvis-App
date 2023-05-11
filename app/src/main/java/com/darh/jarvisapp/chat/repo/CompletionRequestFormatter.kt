package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.api.SystemMessages
import com.darh.jarvisapp.ui.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletionRequestFormatter @Inject constructor() {

    fun format(format: RequestFormat): List<ChatMessage>{
        when (format){
            is RequestFormat.EnhancedChat -> formatChat(format)
            is RequestFormat.ThoughtsAgent -> formatThoughtsAgent(format)
        }
    }

    private fun formatThoughtsAgent(format: RequestFormat.ThoughtsAgent): List<ChatMessage> {
        val messages = format.history.toMutableList()
        val last = messages[messages.size - 1]
        messages[messages.size - 1] =
            last.copy(content = last.content.plus("\n\n(You must respond in the Json schema provided by the system"))
        return SystemMessages.chatSystemSetup()
            .plus(messages)
            .plus(SystemMessages.agentStructureMethod(format.requestedFields))
    }

    private fun formatChat(format: RequestFormat.EnhancedChat): List<ChatMessage> {
        val messages = format.messages.toMutableList()
        val last = messages[messages.size - 1]
        messages[messages.size - 1] =
            last.copy(content = last.content.plus("\n\n(You must respond in the Json schema provided by the system"))
        return SystemMessages.chatSystemSetup()
            .plus(messages)
            .plus(SystemMessages.chatStructureMethod(format.requestedFields))
    }

}

sealed class RequestFormat{
    data class EnhancedChat(val messages: List<ChatMessage>, val requestedFields: List<String>) : RequestFormat()
    data class ThoughtsAgent(val history: List<ChatMessage>, val input: String) : RequestFormat()
}

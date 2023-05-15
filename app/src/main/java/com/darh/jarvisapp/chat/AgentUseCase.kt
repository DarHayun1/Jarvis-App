package com.darh.jarvisapp.chat

import com.darh.jarvisapp.api.AssistantResponse
import com.darh.jarvisapp.api.CompletionState
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.api.StructuredAgentResponse
import com.darh.jarvisapp.api.StructuredChatResponse
import com.darh.jarvisapp.chat.repo.CompletionRequestFormatter
import com.darh.jarvisapp.chat.repo.RequestFormat
import com.darh.jarvisapp.ui.ChatMessage
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

internal class AgentUseCase @Inject constructor(
    private val dataSource: ChatCompletionRemoteDataSource,
    private val formatter: CompletionRequestFormatter
) {

    fun get(
        userInput: String,
    ): Flow<CompletionState> {
        Timber.tag(OPEN_AI).d("get AskAnythingUseCase")
//        connectionData.requireNetwork("AskAnythingUseCase")
//        assistantHistory.add(ChatMessage(ChatRole.User, userInput))
        val messages = formatter.format(RequestFormat.ThoughtsAgent(emptyList(), userInput))
        return dataSource.getCompletionsStream(
            messages = messages,
            requestedFields = listOf(
                StructuredAgentResponse.THOUGHT_FIELD,
                StructuredAgentResponse.NEXT_TOOL_FIELD
            )
        ) {}
    }

    suspend fun provideToolResult(
        history: List<ChatMessage>,
        originalTask: String
    ): StructuredChatResponse? {
        Timber.tag(OPEN_AI).d("get AskAnythingUseCase")
//        connectionData.requireNetwork("AskAnythingUseCase")
//        assistantHistory.add(ChatMessage(ChatRole.User, userInput))
        val messages = formatter.format(RequestFormat.ThoughtsAgent(history, originalTask))
        return dataSource.getCompletionStructured(
            messages = messages
        ).structured
    }

//    fun getEnhancedAnswer(
//        input: String,
//        assistantHistory: ChatHistory,
//    ): Flow<CompletionState> {
//        Timber.tag(OPEN_AI).d("getEnhancedAnswer AskAnythingUseCase")
////        connectionData.requireNetwork("AskAnythingUseCase")
//        val message = ChatMessage(ChatRole.User, "$input\n\nDon't answer my question, just add a google query to the \"${StructuredResponse.ASK_GOOGLE_FIELD}\" field to get updated data will help answer the question.")
//        val completionMessages = assistantHistory.messages.plus(message)
//        // Saving to history just the query as the rules are relevant just once.
//        assistantHistory.add(ChatMessage(ChatRole.User, input))
//        return dataSource.getCompletionsStream(
//            messages = completionMessages,
//            requestedFields = listOf()
//        ) {}
//    }
}

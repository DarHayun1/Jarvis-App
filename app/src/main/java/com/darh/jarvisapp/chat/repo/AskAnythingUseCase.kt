package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.api.ChatCompletionAPI
import com.darh.jarvisapp.api.CompletionState
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.api.StructuredResponse
import com.darh.jarvisapp.chat.ChatRemoteDataSource
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

internal class AskAnythingUseCase @Inject constructor(
    private val dataSource: ChatRemoteDataSource,
) {

    fun get(
        userInput: String,
        assistantHistory: ChatHistory,
    ): Flow<CompletionState> {
        Timber.tag(OPEN_AI).d("get AskAnythingUseCase")
//        connectionData.requireNetwork("AskAnythingUseCase")
        assistantHistory.add(ChatMessage(ChatRole.User, userInput))

        return dataSource.getCompletionsStream(
            messages = assistantHistory.messages,
            requestedFields = listOf(
                StructuredResponse.NEXT_ACTION_FIELD
            )
        ) {}
    }

    fun getEnhancedAnswer(
        input: String,
        assistantHistory: ChatHistory,
    ): Flow<CompletionState> {
        Timber.tag(OPEN_AI).d("getEnhancedAnswer AskAnythingUseCase")
//        connectionData.requireNetwork("AskAnythingUseCase")
        val message = ChatMessage(ChatRole.User, "$input\n\nDon't answer my question, just add a google query to the \"${StructuredResponse.ASK_GOOGLE_FIELD}\" field to get updated data will help answer the question.")
        val completionMessages = assistantHistory.messages.plus(message)
        // Saving to history just the query as the rules are relevant just once.
        assistantHistory.add(ChatMessage(ChatRole.User, input))
        return dataSource.getCompletionsStream(
            messages = completionMessages,
            requestedFields = listOf()
        ) {}
    }
}

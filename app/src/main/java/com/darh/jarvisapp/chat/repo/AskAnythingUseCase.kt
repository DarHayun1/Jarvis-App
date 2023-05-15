package com.darh.jarvisapp.chat.repo

import android.content.Context
import com.darh.jarvisapp.accessibility.ScreenInteractionService
import com.darh.jarvisapp.api.CompletionState
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.api.StructuredChatResponse
import com.darh.jarvisapp.chat.ChatCompletionRemoteDataSource
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

internal class AskAnythingUseCase @Inject constructor(
    private val dataSource: ChatCompletionRemoteDataSource,
    private val structureFormatter: CompletionRequestFormatter
) {

    fun get(
        userInput: String,
        assistantHistory: ChatHistory,
    ): Flow<CompletionState> {
        Timber.tag(OPEN_AI).d("get AskAnythingUseCase")
//        connectionData.requireNetwork("AskAnythingUseCase")
        assistantHistory.add(ChatMessage(ChatRole.User, userInput))

        val requestedFields = listOf(StructuredChatResponse.PROMPT_FIELD, StructuredChatResponse.NEXT_ACTION_FIELD)
        val requestFormat = RequestFormat.EnhancedChat(assistantHistory.messages, requestedFields)
        return dataSource.getCompletionsStream(
            messages = structureFormatter.format(requestFormat),
            requestedFields = requestedFields
        ) {}
    }

    suspend fun executeTask(
        input: String,
        context: Context,
    ): Flow<CompletionState> {
        Timber.tag(OPEN_AI).d("executeTask AskAnythingUseCase input: $input")
//        connectionData.requireNetwork("AskAnythingUseCase")
        ScreenInteractionService.getInstance()?.mapElementsToString()
        context.packageManager.getLaunchIntentForPackage( "com.whatsapp")?.let {
            context.startActivity(it)
        }
        return flow { emit(CompletionState.Complete("response", StructuredChatResponse("done"))) }
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

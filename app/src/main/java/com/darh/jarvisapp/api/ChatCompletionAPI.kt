package com.darh.jarvisapp.api

import com.darh.jarvisapp.ui.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatCompletionAPI {
    /**
     * Returns a Flow of chunks of the assistant response
     * @param onComplete - Callback to receive the entire response when the completion is complete.
     */
    suspend fun getCompletion(model: String, messages: List<ChatMessage>): String

    /**
    //     * Returns a Flow of chunks of the assistant response
    //     * @param onComplete - Callback to receive the entire response when the completion is complete.
    //     */


    fun getCompletions(model: String, messages: List<ChatMessage>): Flow<String>
}

data class AssistantResponse(
    val rawResponse: String,
    val structured: StructuredChatResponse?
)
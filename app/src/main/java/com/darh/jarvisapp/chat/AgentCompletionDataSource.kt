package com.darh.jarvisapp.chat

import com.darh.jarvisapp.api.*
import com.darh.jarvisapp.di.AppScope
import com.darh.jarvisapp.ui.ChatMessage
import com.google.gson.Gson
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentCompletionRemoteDataSource @Inject constructor(
    private val chatCompletionAPI: ChatCompletionAPI,
    private val appScope: AppScope
) {

    @Throws(Exception::class)
    suspend fun getCompletionStructured(
        messages: List<ChatMessage>,
    ): StructuredAgentResponse {
//        Timber.tag(OPEN_AI).d("getCompletion $messages")
        val result = runCatching {
            chatCompletionAPI.getCompletion(
                model = GPT_3_5_TURBO_MODEL,
                messages = messages
            )
        }
        Timber.tag(OPEN_AI).d("getCompletion $result")

        val rawResponse = result.getOrNull()
        return rawResponse?.let {
            //log response
            Timber.tag(OPEN_AI).d("getCompletion $messages")
            Gson().fromJson(
                rawResponse.tryTrimJsonSurroundings(),
                StructuredAgentResponse::class.java
            )
        } ?: throw (result.exceptionOrNull() ?: IllegalStateException("No choices received"))
    }

}

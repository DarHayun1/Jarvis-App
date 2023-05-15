package com.darh.jarvisapp.chat

import com.darh.jarvisapp.api.*
import com.darh.jarvisapp.di.AppScope
import com.darh.jarvisapp.ui.ChatMessage
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class ChatCompletionRemoteDataSource @Inject constructor(
    private val chatCompletionAPI: ChatCompletionAPI,
    private val structuredCompletionsMapper: StructuredCompletionsMapper,
    private val appScope: AppScope
) {

    @Throws(Exception::class)
    suspend fun getCompletion(
        messages: List<ChatMessage>,
    ): String {
        Timber.tag(OPEN_AI).d("getCompletion $messages")
        val result = runCatching {
            chatCompletionAPI.getCompletion(
                model = GPT_3_5_TURBO_MODEL,
                messages = messages
            )
        }
        Timber.tag(OPEN_AI).d("getCompletion $result")

        return result.getOrNull()
            ?: throw (result.exceptionOrNull() ?: IllegalStateException("No choices received"))
    }


    @Throws(Exception::class)
    suspend fun getCompletionStructured(
        messages: List<ChatMessage>
    ): AssistantResponse {
        val result = runCatching {
            chatCompletionAPI.getCompletion(
                model = GPT_3_5_TURBO_MODEL,
                messages = messages
            )
        }

        val rawResponse = result.getOrNull()
        return rawResponse?.let {
            //log response
            Timber.tag(OPEN_AI).d("getCompletion result $rawResponse")
            AssistantResponse(
                rawResponse,
                Gson().fromJson(
                    rawResponse.tryTrimJsonSurroundings(),
                    StructuredChatResponse::class.java
                )
            )
        } ?: throw (result.exceptionOrNull() ?: IllegalStateException("No choices received"))
    }

    fun getCompletionsStream(
        messages: List<ChatMessage>,
        requestedFields: List<String>,
        onComplete: (AssistantResponse) -> Unit
    ): Flow<CompletionState> {

        val startTime = System.currentTimeMillis()
        return try {
            val requestFlow = structuredCompletionsMapper.mapToState(
                chunksFlow = chatCompletionAPI.getCompletions(
                    model = GPT_3_5_TURBO_MODEL,
                    messages = messages
                ),
                requestedFields = requestedFields
            )
            val responseFlow = MutableSharedFlow<CompletionState>(1)
            appScope.launch {
                runCatching {
                    requestFlow.collect { state ->
                        responseFlow.emit(state)
                    }
                }.onFailure {
                    reportStructuredCompletionError(startTime, it)
                }
            }
            responseFlow.transformWhile {
                emit(it)
//                val isMetadataReady = (it as? CompletionState.Metadata)?.isUiReady() == true
                it !is CompletionState.Complete
            }
        } catch (e: Exception) {
            reportStructuredCompletionError(startTime, e)
            throw e
        }
    }

    private fun reportStructuredCompletionError(startTime: Long, error: Throwable) {
        val elapsed = (System.currentTimeMillis() - startTime).milliseconds
        Timber.tag(OPEN_AI)
            .e(error, "Completion stream error after ${elapsed.inWholeSeconds} seconds")
    }

}

@file:OptIn(BetaOpenAI::class)

package com.darh.jarvisapp.api

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.darh.jarvisapp.BuildConfig
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

const val OPEN_AI = "open_ai"
const val GPT_4_MODEL = "gpt-4"
const val GPT_3_5_TURBO_MODEL = "gpt-3.5-turbo"

@Singleton
class OpenAILibraryImpl @Inject constructor() : ChatCompletionAPI {
    private val openAI = OpenAI(
        OpenAIConfig(
            token = BuildConfig.OPEN_AI_KEY,
            timeout = Timeout(socket = 60.seconds, request = 60.seconds),
        )
    )

    @Throws(Exception::class)
    override suspend fun getCompletion(model: String, messages: List<ChatMessage>): String {
        Timber.tag(OPEN_AI).d("getCompletion $messages")
        val result = runCatching {
            openAI.chatCompletion(
                ChatCompletionRequest(
                    model = ModelId(model),
                    messages = messages.mapToRequestMessages()
                )
            )
        }
        Timber.tag(OPEN_AI).d("getCompletion $result")

        val rawResponse = result.getOrNull()?.choices?.firstOrNull()?.message?.content
        return rawResponse ?: throw (result.exceptionOrNull()
            ?: IllegalStateException("No choices received"))
    }

    override fun getCompletions(model: String, messages: List<ChatMessage>): Flow<String> {
        var finishReason: String? = null
        return openAI.chatCompletions(
            ChatCompletionRequest(
                model = ModelId(model),
                messages = messages.mapToRequestMessages()
            )
        ).mapNotNull {
            finishReason = it.choices.firstOrNull()?.finishReason
            it.choices.firstOrNull()?.delta?.content
        }.onCompletion {
            Timber.tag(OPEN_AI).w("finishReason: $finishReason")
        }
    }


}

private fun List<ChatMessage>.mapToRequestMessages(): List<com.aallam.openai.api.chat.ChatMessage> {
    return map {
        com.aallam.openai.api.chat.ChatMessage(
            role = when (it.role) {
                ChatRole.Assistant -> com.aallam.openai.api.chat.ChatRole.Assistant
                ChatRole.System -> com.aallam.openai.api.chat.ChatRole.System
                ChatRole.User -> com.aallam.openai.api.chat.ChatRole.User
            },
            content = it.content
        )
    }.also {
        Timber.tag(OPEN_AI).i("sent messages: ${it.joinToString(separator = "\n")}")
    }
}


internal fun String.tryTrimJsonSurroundings(): String {
    val startIndex = this.indexOfFirst { it == '{' }
    val endIndex = this.indexOfLast { it == '}' }
    val trimmedSurroundings = if (startIndex in 0 until endIndex) {
        substring(
            startIndex,
            endIndex + 1
        )
    } else {
        this
    }
    val lastPsik = trimmedSurroundings.indexOfLast { it == ',' }
    return if (lastPsik != -1 && trimmedSurroundings.substring(lastPsik).none { it.isLetter() }) {
        trimmedSurroundings.removeRange(lastPsik, lastPsik + 1)
    } else {
        trimmedSurroundings
    }
}

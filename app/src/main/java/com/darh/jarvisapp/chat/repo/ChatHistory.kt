package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.ui.ChatMessage

private const val MAX_CHAT_CONTEXT = 10

internal class ChatHistory(
//    private val studentContext: StudentContext,
    private val maxContext: Int = MAX_CHAT_CONTEXT
) {

    val messages: List<ChatMessage>
        get() = stickyContext.plus(chatContext)

    private val stickyContext = listOf<ChatMessage>(
//        UserMessages.declareUserContext(studentContext)
    )

    private var chatContext = listOf<ChatMessage>()

    fun add(vararg messages: ChatMessage) {
        chatContext = chatContext.plus(messages).takeLast(maxContext).validateMaxTokens()
    }

    fun isStarted() = chatContext.isNotEmpty()

    fun clearHistory(initialItems: List<ChatMessage> = emptyList()) {
        chatContext = initialItems
    }

    private fun List<ChatMessage>.validateMaxTokens(): List<ChatMessage> {
        val chars = (this + stickyContext).sumOf { it.content.length }
        val tokens = chars.toDouble() / CHARS_PER_TOKEN
        val extraTokens = tokens - MAX_TOKENS_WITH_BUFFER
        return if (extraTokens > 0 && this.size > 1) {
            this.subList(1, this.size).validateMaxTokens()
        } else {
            this
        }
    }
}

private const val CHARS_PER_TOKEN = 5.8
private const val MAX_TOKENS_WITH_BUFFER = 3200

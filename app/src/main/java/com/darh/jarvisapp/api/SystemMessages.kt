package com.darh.jarvisapp.api

import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole

object SystemMessages {

    fun systemSetup() = listOf(
        ChatMessage(
            role = ChatRole.System,
            content = StructuredResponse.systemSetup()
        )
    )

    fun structureMethod(requestedFields: List<String>) = listOf(
        ChatMessage(
            role = ChatRole.System,
            content = StructuredResponse.structureSetup(requestedFields)
        )
    )

}
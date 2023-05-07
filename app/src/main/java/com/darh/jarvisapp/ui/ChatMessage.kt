package com.darh.jarvisapp.ui

import java.util.UUID

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val id: String = UUID.randomUUID().toString(),
    val name: String? = null
)

sealed class ChatRole(val role: String) {
    object System : ChatRole("system")
    object User : ChatRole("user")
    object Assistant : ChatRole("assistant")

    override fun toString(): String {
        return role
    }
}

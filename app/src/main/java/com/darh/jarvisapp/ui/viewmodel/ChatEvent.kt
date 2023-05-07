package com.darh.jarvisapp.ui.viewmodel

sealed class ChatEvent {
    data class SendMessage(val message: String) : ChatEvent()
}

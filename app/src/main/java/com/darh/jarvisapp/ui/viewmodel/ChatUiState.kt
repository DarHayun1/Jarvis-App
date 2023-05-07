package com.darh.jarvisapp.ui.viewmodel

import com.darh.jarvisapp.ui.ChatMessage

sealed class ChatUiState {
    object Initial : ChatUiState()
    data class ChatLoaded(val messages: List<ChatMessage>) : ChatUiState()
    object Error : ChatUiState()
}

sealed class ChatAction {
    data class ShowToast(val message: String) : ChatAction()
}

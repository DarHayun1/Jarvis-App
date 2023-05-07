package com.darh.jarvisapp.ui.viewmodel

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.darh.jarvisapp.chat.repo.AssistantUiItem
import com.darh.jarvisapp.chat.repo.ChatRepositoryProvider
import com.darh.jarvisapp.ui.ChatFragment.Companion.ASSISTANT_PARAMS_KEY
import com.darh.jarvisapp.ui.adapter.AssistantDynamicTextItem
import com.darh.jarvisapp.ui.adapter.AssistantErrorItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
internal class ChatViewModel @Inject constructor(
    chatRepositoryProvider: ChatRepositoryProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val params = getParams(savedStateHandle)

    private val chatRepository = chatRepositoryProvider.provide(
        params.chatId,
    )

    val uiState = chatRepository.uiState

    val actionsFlow = chatRepository.actionsFlow

//    val actionSuggestionsState = chatRepository.actionSuggestionsState

    fun obtainEvent(event: AssistantVM.Event) {
        chatRepository.obtainEvent(event)
    }

    private fun getParams(savedStateHandle: SavedStateHandle) =
        savedStateHandle.get<AssistantParams>(ASSISTANT_PARAMS_KEY)
            ?: throw IllegalStateException("Couldn't extract AssistantParams from VM params")
}

@Parcelize
data class AssistantParams(val chatId: Int) : Parcelable

internal class AssistantVM {

    sealed class Event {
        data class OnActionSelected(val type: AssistanceType) : Event()
        data class OnNewInput(val input: String, val enhancedQuestion: Boolean = false) : Event()
        data class OnTryAgain(val errorItem: AssistantErrorItem) : Event()
        object OnStopGeneration : Event()
        object OnClearChat : Event()
    }

    data class State(
        val items: List<AssistantUiItem>,
        val dynamicTextFlow: Flow<AssistantDynamicTextItem>? = null,
        val generationActive: Boolean = false
    )

    sealed class Action {
        data class NoInternetError(val tryAgainEvent: Event?) : Action()
    }
}

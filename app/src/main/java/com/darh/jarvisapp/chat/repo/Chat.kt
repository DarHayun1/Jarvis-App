package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.ui.adapter.AssistantDynamicTextItem
import com.darh.jarvisapp.ui.adapter.AssistantErrorItem
import kotlinx.coroutines.flow.Flow

internal object Chat {

    sealed class Event {
        data class OnNewInput(val input: String) : Event()
        data class OnTryAgain(val errorItem: AssistantErrorItem) : Event()
        object OnStopGeneration : Event() //TODO: Should really stop generation
        object OnClearChat : Event()
    }

    data class State(
        val items: List<AssistantUiItem>,
        val dynamicTextFlow: Flow<AssistantDynamicTextItem>? = null,
        val generationActive: Boolean = false,
        val blockUsage: Boolean = false
    )

    sealed class Action {
        data class NoInternetError(val tryAgainEvent: Event?) : Action()
    }
}

interface AssistantUiItem {

    val messageId: String

    override fun equals(other: Any?): Boolean

    open fun isSameItem(other: AssistantUiItem): Boolean = equals(other)
    open fun isLoading(): Boolean = false
}

package com.darh.jarvisapp.ui.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.darh.jarvisapp.chat.repo.AssistantUiItem

internal abstract class AssistantVH<T : AssistantUiItem>(itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    abstract val messageSource: MessageSource

    @Suppress("UNCHECKED_CAST")
    fun bind(item: AssistantUiItem?) {
        bindItem(item as? T)
    }


    open fun bindItem(item: T?) {}
    enum class MessageSource {
        USER, ASSISTANT, APP, ERROR
    }
}

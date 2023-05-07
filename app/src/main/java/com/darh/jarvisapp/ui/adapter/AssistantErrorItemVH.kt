package com.darh.jarvisapp.ui.adapter

import android.view.View
import androidx.annotation.StringRes
import com.darh.jarvisapp.R
import com.darh.jarvisapp.databinding.ErrorItemBinding
import com.darh.jarvisapp.chat.repo.AssistantUiItem
import com.darh.jarvisapp.ui.viewmodel.AssistantVM
import com.darh.jarvisapp.ui.viewmodel.ChatEvent

internal data class AssistantErrorItem(
    val message: String? = null,
    val event: AssistantVM.Event?,
    override val messageId: String,
) : AssistantUiItem

internal class AssistantErrorItemVH(
    val itemView: View,
    val tryAgainClicked: (AssistantErrorItem) -> Unit
) :
    AssistantVH<AssistantErrorItem>(itemView) {
    constructor(
        binding: ErrorItemBinding,
        tryAgainClicked: (AssistantErrorItem) -> Unit
    ) : this(binding.root, tryAgainClicked) {
        this.binding = binding
    }

    var binding: ErrorItemBinding? = null

    override val messageSource = MessageSource.ERROR

    override fun bindItem(item: AssistantErrorItem?) {
        item?.event?.let {
            binding?.tryAgainButton?.setOnClickListener { tryAgainClicked(item) }
            binding?.errorMessage?.setText(
                item.message ?: itemView.context.getString(R.string.generic_error_message)
            )
        }
    }
}

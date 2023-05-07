package com.darh.jarvisapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.darh.jarvisapp.R
import com.darh.jarvisapp.databinding.ButtonsItemBinding
import com.darh.jarvisapp.databinding.DynamicTextItemBinding
import com.darh.jarvisapp.databinding.ErrorItemBinding
import com.darh.jarvisapp.chat.repo.AssistantUiItem
import com.darh.jarvisapp.ui.viewmodel.AssistanceType

internal class MessageAdapter(
    private val onActionSuggest: (AssistanceType) -> Unit,
    private val tryAgainClicked: (AssistantErrorItem) -> Unit,
) : ListAdapter<AssistantUiItem, AssistantVH<out AssistantUiItem>>(DIFF_CALLBACK) {

    /**
     * Added for the ability to change the list items without triggering the diff callback.
     */
    private var cachedItems: MutableList<AssistantUiItem> = mutableListOf()
    override fun submitList(list: MutableList<AssistantUiItem>?, commitCallback: Runnable?) {
        if (cachedItems != list) {
            cachedItems = list?.toMutableList() ?: mutableListOf()
            super.submitList(list, commitCallback)
        }
    }

    /**
     * Used to update the newly received flow items when the VH is not created.
     * That way we avoid losing the last flow item.
     */
    fun updateDynamicTextSilently(newItem: AssistantDynamicTextItem) {
        cachedItems.replaceAll { currentItem ->
            if ((currentItem as? AssistantDynamicTextItem)?.flowId == newItem.flowId) {
                currentItem.copy(content = newItem.content)
            } else {
                currentItem
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AssistantVH<out AssistantUiItem> {
        val vh = when (viewType) {
            R.layout.buttons_item -> AssistanceKeyConceptsItemVH(
                ButtonsItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), onActionSuggest
            )
            R.layout.error_item -> AssistantErrorItemVH(
                ErrorItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), tryAgainClicked
            )
            else -> AssistantTextItemVH(
                DynamicTextItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
        return vh
    }

    override fun onBindViewHolder(holder: AssistantVH<out AssistantUiItem>, position: Int) {
        holder.bind(cachedItems.getOrNull(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (cachedItems.getOrNull(position)) {
            is AssistantDynamicTextItem -> R.layout.dynamic_text_item
            is AssistantKeyConceptsItem -> R.layout.buttons_item
            is AssistantErrorItem -> R.layout.error_item
            else -> -1
        }
    }

    override fun getItemCount(): Int {
        return cachedItems.size
    }
}

private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssistantUiItem>() {

    override fun areItemsTheSame(oldItem: AssistantUiItem, newItem: AssistantUiItem): Boolean {
        return oldItem.isSameItem(newItem)
    }

    override fun areContentsTheSame(oldItem: AssistantUiItem, newItem: AssistantUiItem): Boolean {
        return oldItem == newItem
    }
}

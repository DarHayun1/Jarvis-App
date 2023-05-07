package com.darh.jarvisapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darh.jarvisapp.R
import com.darh.jarvisapp.databinding.ButtonsItemBinding
import com.darh.jarvisapp.chat.repo.AssistantUiItem
import com.darh.jarvisapp.ui.viewmodel.AssistanceType
import java.util.*

private const val NUM_OF_SHIMMERS = 5

data class AssistantKeyConceptsItem(
    val keyConcepts: List<String>,
    val verticalOrientation: Boolean = true,
    @StringRes val header: Int? = null,
    override val messageId: String = UUID.randomUUID().toString(),
) : AssistantUiItem {
    override fun isLoading(): Boolean {
        return keyConcepts.isEmpty()
    }

    override fun isSameItem(other: AssistantUiItem): Boolean {
        return messageId == (other as? AssistantKeyConceptsItem)?.messageId
    }
}

internal class AssistanceKeyConceptsItemVH(
    val itemView: View,
    val conceptClicked: (AssistanceType) -> Unit
) :
    AssistantVH<AssistantKeyConceptsItem>(itemView) {
    constructor(
        binding: ButtonsItemBinding,
        conceptClicked: (AssistanceType) -> Unit
    ) : this(binding.root, conceptClicked) {
        this.binding = binding
    }

    var binding: ButtonsItemBinding? = null

    override val messageSource = MessageSource.ASSISTANT

    override fun bindItem(item: AssistantKeyConceptsItem?) {
        val suggestions = item?.keyConcepts
        if (suggestions != null) {
            binding?.prompt?.setText(item.header ?: -1)
            setupAdapter(suggestions, item)
        } else {
            binding?.root?.visibility = View.GONE
        }
    }

    private fun setupAdapter(suggestions: List<String>, item: AssistantKeyConceptsItem) {
        val adapter = KeyConceptsAdapter(suggestions) {
            conceptClicked(AssistanceType.ResponseSuggestion(it))
        }
        binding?.root?.visibility = View.VISIBLE
        binding?.suggestionsRv?.layoutManager = LinearLayoutManager(
            itemView.context,
            if (item.verticalOrientation) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL,
            false
        )
        binding?.suggestionsRv?.adapter = adapter
    }
}

class KeyConceptsAdapter(
    val items: List<String>,
    val onClick: (String) -> Unit
) : RecyclerView.Adapter<KeyConceptsAdapter.KeyConceptsViewHolder>() {

    private val isLoading
        get() = items.isEmpty()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyConceptsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.suggestion_item, parent, false)
        return KeyConceptsViewHolder(view, onClick)
    }

    override fun getItemCount(): Int {
        return if (items.isEmpty()) NUM_OF_SHIMMERS else items.size
    }

    override fun onBindViewHolder(holder: KeyConceptsViewHolder, position: Int) {
        if (isLoading) {
            holder.setLoading()
        } else {
            holder.bind(items[position])
        }
    }

    companion object {
        const val SHIMMER_ITEM = 1
        const val DATA_ITEM = 2
    }

    class KeyConceptsViewHolder(
        itemView: View,
        val onClick: (String) -> Unit
    ) :
        RecyclerView.ViewHolder(itemView) {

        val textView = itemView.findViewById<TextView>(R.id.suggestion)

        init {
        }

        fun bind(keyConcept: String) {
            itemView.setOnClickListener {
                onClick(textView.tag as String)
            }
            textView?.text = keyConcept
            textView?.tag = keyConcept
        }

        fun setLoading() {
            textView?.text = "Loading..."
        }
    }
}

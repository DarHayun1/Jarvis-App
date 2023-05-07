package com.darh.jarvisapp.ui.adapter

import android.text.Html
import android.text.Spanned
import android.view.Gravity
import android.view.View
import com.darh.jarvisapp.R
import com.darh.jarvisapp.databinding.DynamicTextItemBinding
import com.darh.jarvisapp.chat.repo.AssistantUiItem
import java.util.*

internal data class AssistantDynamicTextItem(
    val content: String,
    val flowId: String = UUID.randomUUID().toString(),
    val header: String? = null,
    val source: AssistantVH.MessageSource = AssistantVH.MessageSource.ASSISTANT,
) : AssistantUiItem {

    override val messageId: String = flowId
    override fun isSameItem(other: AssistantUiItem): Boolean {
        return flowId == (other as? AssistantDynamicTextItem)?.flowId
    }
}

internal class AssistantTextItemVH(itemView: View) :
    AssistantVH<AssistantDynamicTextItem>(itemView) {
    constructor(binding: DynamicTextItemBinding) : this(binding.root) {
        this.binding = binding
    }

    var binding: DynamicTextItemBinding? = null

    private var _messageSource = MessageSource.ASSISTANT
    override val messageSource: MessageSource
        get() = _messageSource

    override fun bindItem(item: AssistantDynamicTextItem?) {
        item?.let {
            _messageSource = item.source
            binding?.header?.text = item.header
            binding?.content?.tag = item.flowId
            updateText(item)
            setPadding()
        }
//        binding?.root?.isVisible = item != null
    }

    private fun updateText(
        item: AssistantDynamicTextItem
    ) {
        val oldText = (binding?.content?.text) as? Spanned

        if (item.content.contains(Html.toHtml(oldText, Html.FROM_HTML_MODE_LEGACY))) {
            val oldLength = oldText?.length ?: 0
            if (item.content.length > oldLength) {
                val delta = Html.fromHtml(item.content.substring(oldLength), Html.FROM_HTML_MODE_LEGACY)
                binding?.content?.append(delta)
            }
        } else {
            val text = Html.fromHtml(item.content, Html.FROM_HTML_MODE_LEGACY)
            binding?.content?.text = text
        }
    }

    private fun setPadding() {
        val textView = binding?.content
        val contentRoot = binding?.contentRoot
        val padding =
            itemView.context.resources.getDimensionPixelSize(R.dimen.chat_hor_margin)
        if (messageSource == MessageSource.USER) {
            contentRoot?.setPadding(padding, contentRoot.paddingTop, 0, contentRoot.paddingBottom)
            textView?.gravity = Gravity.END
            textView?.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        } else {
            contentRoot?.setPadding(0, contentRoot.paddingTop, padding, contentRoot.paddingBottom)
            textView?.gravity = Gravity.START
            textView?.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    fun isFlowMatch(flowId: String) = (binding?.content?.tag as? String)?.equals(flowId) == true

    fun updateTextForFlow(item: AssistantDynamicTextItem) {
        if (isFlowMatch(item.flowId)) {
            updateText(item)
        }
    }
}

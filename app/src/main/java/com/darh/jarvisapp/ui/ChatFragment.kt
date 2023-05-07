package com.darh.jarvisapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.databinding.FragmentChatBinding
import com.darh.jarvisapp.launchRepeatOn
import com.darh.jarvisapp.ui.adapter.AssistantDynamicTextItem
import com.darh.jarvisapp.ui.adapter.AssistantTextItemVH
import com.darh.jarvisapp.ui.adapter.MessageAdapter
import com.darh.jarvisapp.ui.viewmodel.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onCompletion
import timber.log.Timber

@AndroidEntryPoint
class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var adapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        observeState()
    }

    private fun initUI() {
        setupContentRv()
        setupInputBar()
    }

    @OptIn(FlowPreview::class)
    private fun observeState() {
        launchRepeatOn(Lifecycle.State.STARTED) {
            // collectLatest is used for cancelling previous dynamic text flow collection.
            // debounce is used to prevent inconsistent adapter states
            viewModel.uiState.debounce(150L).collectLatest {
                Timber.tag(OPEN_AI).d("State collected. $it")
                handleUiState(it)
            }
        }

        launchRepeatOn(Lifecycle.State.STARTED) {
            viewModel.actionsFlow.collect {
                handleAction(it)
            }
        }
    }

    private fun setupInputBar() {
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.query.toString()
            viewModel.obtainEvent(AssistantVM.Event.OnNewInput(message))
            binding.messageInput.setQuery("", false)
            binding.messageInput.clearFocus()
        }

        binding.googleButton.setOnClickListener {
            val message = binding.messageInput.query.toString()
            viewModel.obtainEvent(AssistantVM.Event.OnNewInput(message, enhancedQuestion = true))
            binding.messageInput.setQuery("", false)
            binding.messageInput.clearFocus()
        }
    }

    private fun setupContentRv() {
        adapter = MessageAdapter(
            onActionSuggest = { viewModel.obtainEvent(AssistantVM.Event.OnActionSelected(it)) },
            tryAgainClicked = { viewModel.obtainEvent(AssistantVM.Event.OnTryAgain(it)) },
        )
        val linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMessages.layoutManager = linearLayoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun handleAction(action: AssistantVM.Action) {
        when (action) {
            is AssistantVM.Action.NoInternetError -> showErrorSnackbar(action)
        }
    }

    private suspend fun handleUiState(state: AssistantVM.State) {
        adapter.submitList(state.items.toMutableList()) {
            if (isScrolledToBottom()) {
                binding.rvMessages.scrollToPosition(state.items.size - 1)
            } else if (binding.rvMessages.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                binding.rvMessages.smoothScrollToPosition(state.items.size - 1)
            }
        }
        handleDynamicTextFlow(state)
    }

    private fun showErrorSnackbar(errorItem: AssistantVM.Action.NoInternetError) {
        Snackbar.make(
            requireView(),
            "No internet connection",
            Snackbar.LENGTH_SHORT
        ).setAction("Try again"){
            val event = errorItem.tryAgainEvent
                ?: AssistantVM.Event.OnNewInput("Try again")
            viewModel.obtainEvent(event)
        }.show()
    }

    /**
     * This function suspends until the ui state is updated or the flow is complete.
     * Must be used with collectLatest in order the to cancel the current flow collection.
     */
    private suspend fun handleDynamicTextFlow(state: AssistantVM.State) {
        state.dynamicTextFlow?.onCompletion {
            Timber.tag(OPEN_AI).w("Flow completed. e:$it")
        }
        state.dynamicTextFlow?.collect { dynamicTextItem ->
//            Timber.tag(OPEN_AI).d("collect $dynamicTextItem")

            val vh = findDynamicTextVH(dynamicTextItem)
            if (vh != null) {
                val isScrolledToBottom = isScrolledToBottom()
                val isScrollIdle = binding.rvMessages.scrollState == RecyclerView.SCROLL_STATE_IDLE

                Timber.tag(OPEN_AI).d("updateTextForFlow")

                vh.updateTextForFlow(dynamicTextItem)

                if (isScrollIdle && isScrolledToBottom) {
                    binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
                }
            } else {
                adapter.updateDynamicTextSilently(dynamicTextItem)
            }
        }
    }

    private fun isScrolledToBottom(): Boolean {
        val visibleArea = binding.rvMessages.computeVerticalScrollExtent()
        val scrollPosition = binding.rvMessages.computeVerticalScrollOffset()
        val totalContentRange = binding.rvMessages.computeVerticalScrollRange()
        val isScrolledToBottom = visibleArea + scrollPosition >= totalContentRange
        return isScrolledToBottom
    }

    private suspend fun findDynamicTextVH(dynamicTextItem: AssistantDynamicTextItem): AssistantTextItemVH? {
        val rv = binding.rvMessages
        repeat(2) {
            for (i in rv.childCount - 1 downTo 0) {
                val vh = rv.getChildViewHolder(rv.getChildAt(i))
                if ((vh as? AssistantTextItemVH)?.isFlowMatch(dynamicTextItem.flowId) == true) {
                    return vh
                }
            }
            delay(200L)
        }
        return null
    }



    fun updateInputBar(query: String?) {
        binding.messageInput.setQuery(query, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val ASSISTANT_PARAMS_KEY = "assistant_params"

        fun newInstance(params: AssistantParams): ChatFragment {
            return ChatFragment().apply {
                arguments = bundleOf(
                    ASSISTANT_PARAMS_KEY to params
                )
            }
        }
    }
}


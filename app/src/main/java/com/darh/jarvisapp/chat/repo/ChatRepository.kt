package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.BuildConfig
import com.darh.jarvisapp.R
import com.darh.jarvisapp.api.CompletionState
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.api.StructuredResponse
import com.darh.jarvisapp.api.StructuredResponse.Companion.NEXT_ACTION_FIELD
import com.darh.jarvisapp.di.AppScope
import com.darh.jarvisapp.google.GoogleSearchRepository
import com.darh.jarvisapp.google.SearchResultResponse
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole
import com.darh.jarvisapp.ui.adapter.AssistantDynamicTextItem
import com.darh.jarvisapp.ui.adapter.AssistantErrorItem
import com.darh.jarvisapp.ui.adapter.AssistantKeyConceptsItem
import com.darh.jarvisapp.ui.adapter.AssistantVH
import com.darh.jarvisapp.ui.viewmodel.AssistanceType
import com.darh.jarvisapp.ui.viewmodel.AssistantVM.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

internal class ChatRepository(
    private val askAnythingUseCase: AskAnythingUseCase,
    private val googleResultsUseCase: GoogleResultsUseCase,
    private val requestsManager: ChatRequestsManager,
    private val appScope: AppScope,
    private val googleRepository: GoogleSearchRepository
) {

    private val assistantHistory = ChatHistory()

    private val _state = MutableStateFlow(State(emptyList()))
    val uiState = _state.mapUiState()

    private val _actionsFlow = MutableSharedFlow<Action>()
    val actionsFlow = _actionsFlow.asSharedFlow()

    init {
        initNewChat()
    }

    fun obtainEvent(event: Event) {
        when (event) {
            Event.OnStopGeneration -> requestsManager.stopAndClearRunningRequest()
            is Event.OnActionSelected -> onActionSelected(event)
            is Event.OnNewInput -> onNewUserInput(
                event,
                includeUserMessage = true
            )
            is Event.OnTryAgain -> onTryAgain(event)
            Event.OnClearChat -> onClearChat()
        }
    }

    private fun googleSearch(
        query: String,
        questionSummary: String?,
        triggerEvent: Event,
        userMessage: String
    ) {
        appScope.launch {

            val result = runCatching {
                googleRepository.search(
                    query,
                    BuildConfig.GOOGLE_SEARCH_KEY,
                    BuildConfig.GOOGLE_ENGINE_KEY
                )
            }
            Timber.tag(OPEN_AI).i("google results: $result")
            result.getOrNull()?.items?.let { searchResults ->
                var resultIndex :Int? = null
                requestsManager.launchRequest(
                    requestBlock = {
                        resultIndex = googleResultsUseCase.getIndexFromSearchResults(
                            searchResults, questionSummary ?: query, assistantHistory
                        )
                    },
                    onError = {
                        Timber.tag(OPEN_AI).e(it, "handleGoogleResultRequest failed")
                    },
                    onCompletion = {
                        Timber.tag(OPEN_AI).d("googleSearch onCompletion. e:[$it], resultIndex: [$resultIndex]")
                        handleRequestCompletion(it, listOf())
                        resultIndex?.let { index ->
                            handleGoogleResultRequest(index, searchResults, userMessage, triggerEvent)
                        }
                    }
                )
            } ?: reportGoogleError(result.exceptionOrNull())
        }
    }

    private fun reportGoogleError(exceptionOrNull: Throwable?) {
        Timber.tag(OPEN_AI).e(exceptionOrNull, "Error searching google")
        updateState(newItems = listOf(AssistantDynamicTextItem("Error searching google: $exceptionOrNull")))
    }

    private fun handleGoogleResultRequest(
        index: Int,
        searchResults: List<SearchResultResponse.SearchResult>,
        userMessage: String,
        triggerEvent: Event
    ) {
        searchResults.getOrNull(index)?.let { result ->
            var summaryResult: String? = null
            requestsManager.launchRequest(
                requestBlock = {
                    summaryResult = googleResultsUseCase.summarizeGoogleResult(result, userMessage)

                },
                onError = {
                    Timber.tag(OPEN_AI).e(it, "handleGoogleResultRequest failed")
                },
                onCompletion = {
                    Timber.tag(OPEN_AI).d("handleGoogleResultRequest onCompletion. e:[$it], summaryResult: [$summaryResult]")
                    handleRequestCompletion(it, listOf())
                    summaryResult?.let { summary ->
                        launchStreamRequest(
                            responseFlow = {
                                googleResultsUseCase.getAnswerFromSummary(
                                    summary,
                                    assistantHistory,
                                    userMessage
                                )
                            },
                            triggerEvent = triggerEvent
                        )
                    }
                }
            )
        }
    }

    fun init() {
//        when {
//            showIntroSequence -> showIntroSequence(initialAssistance)
//            initialAssistance != null -> {
//                onActionSelected(Event.OnActionSelected(initialAssistance))
//            }
//
//            !assistantHistory.isStarted() && provideSuggestionIfEmpty -> {
        initNewChat()
//            }
//        }
    }

    private fun onClearChat() {
        initNewChat(includeSuggestions = true)
    }

    private fun onActionSelected(event: Event.OnActionSelected) {
        when (event.type) {
            is AssistanceType.ResponseSuggestion ->
                onNewUserInput(Event.OnNewInput(event.type.text))
        }
    }

    private fun updateState(
        baseState: State = _state.value,
        dynamicTextFlow: Flow<AssistantDynamicTextItem>? = baseState.dynamicTextFlow,
        newItems: List<AssistantUiItem> = emptyList(),
        isGenerating: Boolean = baseState.generationActive,
    ) {
        val newList = newItems.toMutableList()
        var messages = baseState.items.map { old ->
            val replaceItem = newList.find { new -> new.isSameItem(old) }
            replaceItem?.also { newList.remove(it) } ?: old
        } + newList

        if (!isGenerating) {
            messages = messages.filterNot { it.isLoading() }
        }
        _state.value = State(
            items = messages,
            dynamicTextFlow = dynamicTextFlow,
            generationActive = isGenerating,
        )
    }

    private fun initNewChat(includeSuggestions: Boolean = false) {
        appScope.launch {
            requestsManager.stopCurrentSessionSync()
            assistantHistory.clearHistory()
            val messages = listOf<AssistantUiItem>()
            _state.emit(State(messages))

            // TODO: Added to ensure Chat started. consider change logic.
            if (includeSuggestions) {
                assistantHistory.add(ChatMessage(ChatRole.User, "How can I help?"))
            }
        }
    }

    private fun onTryAgain(event: Event.OnTryAgain) {
        event.errorItem.event?.let {
            val prevState = _state.value
            val list = prevState.items.toMutableList()
            list.remove(event.errorItem)
            if ((list.lastOrNull() as? AssistantDynamicTextItem)?.content?.isBlank() == true) {
                list.removeLast()
                val lastUserContent = (list.lastOrNull() as? AssistantDynamicTextItem)?.content
                val retryContent = (event.errorItem.event as? Event.OnNewInput)?.input
                if (lastUserContent == retryContent) {
                    list.removeLast()
                }
            }
            updateState(prevState.copy(items = list))
            obtainEvent(event.errorItem.event)
        }
        // TODO: Analytics, Any other logic?
    }

    private fun launchStreamRequest(
        responseFlow: suspend () -> Flow<CompletionState>,
        header: String? = null,
        triggerEvent: Event,
        persistedItem: AssistantUiItem? = null,
        onCompletion: (StructuredResponse?) -> Unit = {}
    ) {
        var messageBuffer = ""
        val flowId = UUID.randomUUID().toString()

        val textFlow = MutableSharedFlow<AssistantDynamicTextItem>(replay = 1)
        var response: StructuredResponse? = null

        requestsManager.launchRequest(
            requestBlock = {
                updateState(
                    dynamicTextFlow = textFlow,
                    newItems = listOfNotNull(
                        persistedItem,
                        AssistantDynamicTextItem(messageBuffer, flowId, header)
                    ),
                    isGenerating = true
                )
                responseFlow().collect { state ->
                    val responseSnapshot = handleResponseState(
                        state = state,
                        flowId = flowId,
                        messageBuffer = messageBuffer,
                        header = header,
                        textFlow = textFlow,
                        triggerEvent = triggerEvent
                    )
                    messageBuffer = responseSnapshot.tempBuffer
                    responseSnapshot.response?.let { response = it }
                }
            },
            onError = { e ->
                Timber.tag(OPEN_AI).e(e, "launchRequestStream failed, e: [$e]")
                val latestTextItem = AssistantDynamicTextItem(messageBuffer, flowId, header)
                handleRequestException(
                    e,
                    triggerEvent,
                    listOfNotNull(latestTextItem.takeUnless { messageBuffer.isBlank() }),
                    flowId
                )
            },
            onCompletion = { e ->
                Timber.tag(OPEN_AI).d("onCompletion. e:[$e], response: [$response]")
                handleRequestCompletion(
                    e, listOf(AssistantDynamicTextItem(messageBuffer, flowId, header)),
                    messageBuffer
                )
                onCompletion(response)
            }
        )
    }

    private fun onNewUserInput(
        event: Event.OnNewInput,
        includeUserMessage: Boolean = true,
        header: String? = null,
    ) {
        val input = event.input
        if (input.isBlank()) {
            Timber.tag(OPEN_AI).d("input is blank, ignoring request..")
            return
        }
        val userMessage =
            AssistantDynamicTextItem(
                input,
                source = AssistantVH.MessageSource.USER
            )
        launchStreamRequest(
            responseFlow = {
                if (event.enhancedQuestion) {
                    askAnythingUseCase.getEnhancedAnswer(input, assistantHistory)
                } else {
                    askAnythingUseCase.get(input, assistantHistory)
                }
            },
            header = header,
            triggerEvent = event,
            persistedItem = userMessage.takeIf { includeUserMessage },
            onCompletion = { response ->
                response?.askGoogle?.let {
                    googleSearch(it, response.questionSummary, event, event.input)
                }
            }
        )
    }

    /**
     * Handles stream request state.
     * @return The latest buffered message
     */
    private suspend fun handleResponseState(
        state: CompletionState,
        flowId: String,
        messageBuffer: String,
        header: String?,
        textFlow: MutableSharedFlow<AssistantDynamicTextItem>,
        triggerEvent: Event
    ): ResponseStateSnapshot {
        var tempBuffer = messageBuffer
        val textItem = AssistantDynamicTextItem(tempBuffer, flowId, header)
        Timber.tag(OPEN_AI).i("state $state")

        var response: StructuredResponse? = null

        when (state) {
            is CompletionState.Typing -> {
                tempBuffer += state.content
//                Timber.tag(OPEN_AI).d("sending $textItem")
                textFlow.emit(textItem)
            }

            is CompletionState.Metadata -> {
                val messages = provideMetadataItems(state, flowId, textItem, triggerEvent)

                updateState(
                    dynamicTextFlow = null,
                    newItems = messages,
                    isGenerating = !state.isUiReady()
                )
            }

            is CompletionState.Complete -> {
                val messages = listOfNotNull(
                    textItem,
                    getNextActionsItem(state.structuredResponse.nextActions, flowId)
                )
                updateState(dynamicTextFlow = null, newItems = messages, isGenerating = false)
                response = state.structuredResponse
            }
        }
        return ResponseStateSnapshot(tempBuffer = tempBuffer, response)
    }

    private fun provideMetadataItems(
        state: CompletionState.Metadata,
        flowId: String,
        typedTextItem: AssistantDynamicTextItem? = null,
        triggerEvent: Event
    ): List<AssistantUiItem> {
        val actions = state.getList(NEXT_ACTION_FIELD)

        return if (actions != null) {
            listOfNotNull(
                typedTextItem,
                getNextActionsItem(actions, flowId)
            )
        } else {
            emptyList()
        }
    }

    private suspend fun handleRequestException(
        e: Throwable,
        retryEvent: Event?,
        newItems: List<AssistantUiItem> = emptyList(),
        messageId: String
    ) {

//        if (e is NoNetworkException) {
//            updateState(
//                dynamicTextFlow = null,
//                newItems = newItems,
//                isGenerating = false
//            )
//            _actionsFlow.emit(Action.NoInternetError(retryEvent))
//        } else {
        updateState(
            dynamicTextFlow = null,
            newItems = newItems.plus(
                AssistantErrorItem(
                    "Something went wrong. e.message:\n${e.message}",
                    retryEvent,
                    messageId
                )
            ),
            isGenerating = false
        )
//        }
    }

    private fun handleRequestCompletion(
        e: Throwable?,
        persistedItems: List<AssistantUiItem>,
        collectedMessage: String? = null
    ) {
        collectedMessage?.let {
            assistantHistory.add(ChatMessage(ChatRole.Assistant, collectedMessage))
        }

        if (e is StopGenerationException) {
            updateState(
                dynamicTextFlow = null,
                newItems = persistedItems,
                isGenerating = false
            )
        }
    }

    private fun getNextActionsItem(actionList: List<String>?, flowId: String) =
        actionList?.let {
            AssistantKeyConceptsItem(
                actionList,
                verticalOrientation = false,
                header = R.string.suggestions_prompt,
                messageId = flowId
            )
        }

    private fun MutableStateFlow<State>.mapUiState(): StateFlow<State> {
        return map { state ->
            val items = state.items.toMutableList()
            //TODO: Remove this function if generationItem is not used.
//            if (state.generationActive) {
//                items.add(GenerationItem(items.lastOrNull()?.messageId ?: ""))
//            }
            state.copy(items = items)
        }.stateIn(appScope, SharingStarted.Eagerly, State(emptyList()))
    }
}

class StopGenerationException : CancellationException()
object StopSessionException : CancellationException()

private data class ResponseStateSnapshot(val tempBuffer: String, val response: StructuredResponse?)

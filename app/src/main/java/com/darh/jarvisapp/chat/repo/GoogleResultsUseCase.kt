package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.BuildConfig
import com.darh.jarvisapp.api.CompletionState
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.api.StructuredChatResponse
import com.darh.jarvisapp.chat.ChatCompletionRemoteDataSource
import com.darh.jarvisapp.google.GoogleSearchRepository
import com.darh.jarvisapp.google.SearchResultResponse
import com.darh.jarvisapp.page_reader.WebPageReader
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

internal class GoogleResultsUseCase @Inject constructor(
    private val dataSource: ChatCompletionRemoteDataSource,
    private val webPageReader: WebPageReader,
    private val structureFormatter: CompletionRequestFormatter,
    private val googleRepository: GoogleSearchRepository
) {
    /**
    fun getAnswerFromSearchResults(
    results: List<SearchResultResponse.SearchResult>,
    googleQuery: String,
    assistantHistory: ChatHistory,
    ): Flow<CompletionState> {
    Timber.tag(OPEN_AI).d("GoogleResultsUseCase")
    //        connectionData.requireNetwork("AskAnythingUseCase")
    val resultsText = StringBuffer()
    results.forEachIndexed { index, result ->
    resultsText.append(result.toStringI(index))
    if (index != results.size - 1) {
    resultsText.append(",\n")
    }
    }

    val messages = assistantHistory.messages.plus(
    ChatMessage(
    ChatRole.User,
    "Respond to my previous question based on the Google search results\n" +
    "If you need more information you can ask to read the search result by adding the result index to the \"${StructuredResponse.GOOGLE_RESULT_FIELD}\"\n" +
    "Google search results:$resultsText\n\n" +
    "The google query was: $googleQuery."
    )
    )

    assistantHistory.add(
    ChatMessage(
    ChatRole.User,
    "Respond to my previous question based on the Google search results"
    )
    )

    return dataSource.getCompletionsStream(
    messages = messages,
    requestedFields = listOf(
    StructuredResponse.NEXT_ACTION_FIELD
    )
    ) {}
    }
     **/

    suspend fun getInfo(question: String): String? {
        Timber.tag(OPEN_AI).d("GoogleResultsUseCase getInfo: $question")

        val queries = generateGoogleQueries(question)
        val googleResults = queries.firstOrNull()?.let { searchGoogle(it) }
        val resultsText = StringBuffer()
        googleResults?.forEachIndexed { index, result ->
            resultsText.append(result.toStringI(index))
            if (index != googleResults.size - 1) {
                resultsText.append(",\n")
            }
        }

        val messages = listOf(
            ChatMessage(
                ChatRole.User,
                "Choose one search result that you think is most likely to help you answer the question and respond only with the index of the result. without additional text around it.\n" +
                        "Google search results: $resultsText\n\n" +
                        "The question was: $question.\n\n" +
                        "Your response will be: \"result index: {index}.\" without additional text."
            )
        )

        val resultIndex = dataSource.getCompletion(
            messages = messages
        ).filter { it.isDigit() }.toInt()

        return googleResults?.getOrNull(resultIndex)?.let {
            summarizeGoogleResult(it, question)
        }
    }

    private suspend fun generateGoogleQueries(question: String): List<String> {
        val messages = listOf(
            ChatMessage(
                ChatRole.User,
                "We need to answer this question with up to date information:\n" +
                        "Question: $question\n\n" +
                        "Response with 3 good google queries I can use to find good information to answer the question.\n" +
                        "IMPORTANT!:Your response will be only the queries in this format: \"[\"query option 1\", \"query option 2\", \"query option 3\"]\" without additional text around it!\n" +
                        "For example for the question \"What rock concerts are in Tel Aviv in 2023\" the response can be:\n" +
                        "[\"Tel aviv rock concerts 2023\", \"Rock show Barbie Reading Tel Aviv 2023\", \"Tel aviv music shows this year\"]"
            )
        )

        return dataSource.getCompletion(
            messages = messages
        ).removeSurrounding("[", "]")
            .split(",")
            .map { it.removeSurrounding(" ").removeSurrounding("\"") }
    }

    private suspend fun searchGoogle(query: String): List<SearchResultResponse.SearchResult>? {
        val items = runCatching {
            Timber.tag(OPEN_AI).d("Google search query: $query")

            val result = googleRepository.search(
                query,
                BuildConfig.GOOGLE_SEARCH_KEY,
                BuildConfig.GOOGLE_ENGINE_KEY
            )
            Timber.tag(OPEN_AI).i("Google search result: $result")
            result
        }.getOrNull()?.items
        if (!items.isNullOrEmpty()) {
            return items
        }
        return null
    }

    suspend fun getIndexFromSearchResults(
        results: List<SearchResultResponse.SearchResult>,
        questionSummary: String,
    ): Int {
        Timber.tag(OPEN_AI).d("GoogleResultsUseCase")
//        connectionData.requireNetwork("AskAnythingUseCase")
        val resultsText = StringBuffer()
        results.forEachIndexed { index, result ->
            resultsText.append(result.toStringI(index))
            if (index != results.size - 1) {
                resultsText.append(",\n")
            }
        }

        val messages = listOf(
            ChatMessage(
                ChatRole.User,
                "Choose one search result that you think is most likely to help you answer the question and respond only with the index of the result. without additional text around it.\n" +
                        "Google search results:$resultsText\n\n" +
                        "The question was: $questionSummary.\n\n" +
                        "Your response will be: \"result index: {index}.\" without additional text."
            )
        )

        return dataSource.getCompletion(
            messages = messages
        ).filter { it.isDigit() }.toInt()
    }

    suspend fun summarizeGoogleResult(
        result: SearchResultResponse.SearchResult,
        userMessage: String
    ): String {

        val pageText = result.link?.let { webPageReader.readFromUrl(it) }
        var chunkSummary: String? = null
        Timber.tag(OPEN_AI).w("summarizeGoogleResult: (${pageText}")
        if (pageText != null && pageText.length > MAX_CONTENT_LENGTH) {
            Timber.tag(OPEN_AI).w("Long page content (${pageText.length}, splitting requests")
            val chunks = pageText.chunked(MAX_CONTENT_LENGTH.toInt())
            chunks.forEachIndexed { index, chunk ->
                chunkSummary = summarizeChunk(chunk, index, chunks.size, chunkSummary, userMessage)
            }
        }
        return chunkSummary ?: result.snippet ?: ""
    }

    private suspend fun summarizeChunk(
        chunk: String,
        i: Int,
        size: Int,
        partialSummary: String?,
        userMessage: String
    ): String {
        Timber.tag(OPEN_AI).d("Summarizing chunk ${i + 1}/$size")
        return dataSource.getCompletion(
            messages = listOf(
                ChatMessage(
                    ChatRole.User,
                    ("You are writing a shorter version of a web page content. There are $size parts to the content, you are summarizing it in pieces and you are getting part #${i + 1}/$size.\n" +
                            "You must include all relevant data that is related to the original message.\n" +
                            "Include just the new version in your message without additional text.\n" +
                            "The original message: $userMessage,\n" +
                            (partialSummary?.let { "The partial summary so far: $it,\n" } ?: "") +
                            "Part #${i + 1}:\n$chunk\n\n" +
                            when (i) {
                                size - 1 -> "This is the last part. Write the final version of the summary."
                                else -> "Write a short version of the content so far. if the text is cut off mark it for the next part summary."
                            }
                            )
                )
            )
        )
    }

    fun getAnswerFromSummary(
        summaryResult: String,
        assistantHistory: ChatHistory,
        previousUserMessage: String
    ): Flow<CompletionState> {
        Timber.tag(OPEN_AI).d("GoogleResultsUseCase getAnswerFromSummary")
//        connectionData.requireNetwork("AskAnythingUseCase")

        assistantHistory.add(
            ChatMessage(
                ChatRole.User,
                "Respond to my previous question with the help of what we found on the web.\n" +
                        "My message was: $previousUserMessage\n\n" +
                        "What we found on the web:\n$summaryResult."
            )
        )
        val requestedFields = listOf(StructuredChatResponse.NEXT_ACTION_FIELD)
        val requestFormat = RequestFormat.EnhancedChat(assistantHistory.messages, requestedFields)

        return dataSource.getCompletionsStream(
            messages = structureFormatter.format(requestFormat),
            requestedFields = requestedFields
        ) {}
    }
}

private const val CHARS_PER_TOKEN_EST = 5.8
private const val MAX_CONTENT_LENGTH = 2000 * CHARS_PER_TOKEN_EST

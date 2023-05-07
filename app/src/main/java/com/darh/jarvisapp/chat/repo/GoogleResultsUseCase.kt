package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.api.CompletionState
import com.darh.jarvisapp.api.OPEN_AI
import com.darh.jarvisapp.api.StructuredResponse
import com.darh.jarvisapp.chat.ChatRemoteDataSource
import com.darh.jarvisapp.google.SearchResultResponse
import com.darh.jarvisapp.page_reader.WebPageReader
import com.darh.jarvisapp.ui.ChatMessage
import com.darh.jarvisapp.ui.ChatRole
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

internal class GoogleResultsUseCase @Inject constructor(
    private val dataSource: ChatRemoteDataSource,
    private val webPageReader: WebPageReader
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

    suspend fun getIndexFromSearchResults(
        results: List<SearchResultResponse.SearchResult>,
        questionSummary: String,
        assistantHistory: ChatHistory,
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
        var chunkSummary :String? = null
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
        Timber.tag(OPEN_AI).d("Summarizing chunk ${i+1}/$size")
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

    return dataSource.getCompletionsStream(
        messages = assistantHistory.messages,
        requestedFields = listOf(
            StructuredResponse.NEXT_ACTION_FIELD
        )
    ) {}
}
}

private const val CHARS_PER_TOKEN_EST = 5.8
private const val MAX_CONTENT_LENGTH = 2000 * CHARS_PER_TOKEN_EST

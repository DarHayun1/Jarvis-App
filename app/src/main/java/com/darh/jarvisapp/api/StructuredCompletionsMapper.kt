package com.darh.jarvisapp.api

import com.darh.jarvisapp.filterNotNullValues
import com.darh.jarvisapp.unescapeSpecialChars
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

private const val SEPARATOR = "##&\$PROMPT_SEPARATOR##&\$"

class StructuredCompletionsMapper @Inject constructor() {

    fun mapToState(
        chunksFlow: Flow<String>,
        requestedFields: List<String>
    ): Flow<CompletionState> {

        var promptStarted = false
        var promptEnded = false
        var stringBuffer = ""
        val jsonElementsReader = JsonElementsReader()
        return chunksFlow
//            .onEach { Timber.tag(OPEN_AI).v("chunk: $it") }
            .detectAndCombine(SEPARATOR, requestedFields.firstOrNull())
//            .onEach { Timber.tag(OPEN_AI).v("chunk (post): $it") }
            .detectAndCombineSpecialChars()
            .detectAndCombineBoldTags()
            .mapNotNull {
                return@mapNotNull if (it == SEPARATOR) {
                    promptEnded = promptStarted
                    promptStarted = true
                    null
                } else {
                    stringBuffer += it
                    it
                }
            }
            .mapNotNull { rawChunk ->
                jsonElementsReader.feedChunk(rawChunk)
                when {
                    promptStarted && !promptEnded -> {
                        CompletionState.Typing(rawChunk.unescapeSpecialChars())
                    }
                    promptEnded -> {
                        val fieldsMap = requestedFields
                            .associateWith { jsonElementsReader.getField(it) }
                            .filterNotNullValues()
                        CompletionState.Metadata(fieldsMap, requestedFields)
                    }
                    else -> null
                }
            }
            .distinctUntilChanged()
            .onCompletion { throwable ->
                if (throwable == null) {
                    Timber.tag(OPEN_AI)
                        .d("getCompletionFlow onCompletion. response:\n$stringBuffer")
                    val structuredResponse = Gson().fromJson(
                        stringBuffer.tryTrimJsonSurroundings(),
                        StructuredChatResponse::class.java
                    )
                    emit(CompletionState.Complete(stringBuffer, structuredResponse))
                }
            }
    }
}

/**
 * Detecting and sending the [separatorSequence] in the beginning and ending of the prompt.
 */
private fun Flow<String>.detectAndCombine(
    separatorSequence: String,
    nextField: String?
): Flow<String> =
    channelFlow {

        var buffer = ""
        var promptStarted = false
        var promptEnded = false
        val promptSeq = "\"prompt\": \""

        this@detectAndCombine.mapNotNull { it }.collect { chunk ->

            when {
                !promptStarted -> {
                    buffer += chunk
                    val seqIndex = buffer.indexOf(promptSeq)
                    promptStarted = seqIndex >= 0
                    val promptIndex = seqIndex + promptSeq.length
                    if (promptStarted) {
                        send(buffer.substring(0, promptIndex))
                        send(separatorSequence)
                        if (promptIndex < buffer.length) {
                            send(buffer.substring(promptIndex))
                        }
                        buffer = ""
                    }
                }

                !promptEnded -> {
                    if (chunk.contains("\"") || buffer.contains("\"")) {
                        buffer += chunk
                        val index = buffer.indexOf('"')
//                        Timber.i("\" index: $index")
                        if (index > 0) {
                            // Clear text before '"'. makes it the first char in the buffer
                            send(buffer.substring(0, index))
                            buffer = buffer.substring(index)
                        }
                        val endOfPromptSequence = nextField?.let { "\"$it\":" } ?: "\"}"
                        if (buffer.replace("\n", "").contains(endOfPromptSequence)) {
                            // We found the start of the next json field
                            send(separatorSequence)
                            send(buffer.substring(index))
                            promptEnded = true
                            buffer = ""
                        } else {
//                            Timber.tag(OPEN_AI).d("nextField: [$nextField], buffer: [$buffer], filtered: [${buffer.filter { it.isLetter() }}]")
                            if (nextField?.letters()?.contains(buffer.letters()) == false) {
                                // the buffer contains letter that are not the next json field. clear buffer.
                                send(buffer)
                                buffer = ""
                            }
                        }
                    } else {
                        send(chunk)
                    }
                }

                else -> {
                    send(chunk)
                }
            }
        }
    }

/**
 * Combining chunks that may split special characters
 */
private fun Flow<String>.detectAndCombineSpecialChars(): Flow<String> = channelFlow {
    var buffer = ""

    this@detectAndCombineSpecialChars.collect { chunk ->

        val escapeCharIndex = chunk.indexOf('\\')
        val andCharIndex = chunk.indexOf('&')
        if (escapeCharIndex >= 0 || andCharIndex >= 0 || buffer.isNotEmpty()) {
            buffer += chunk
            if (maxOf(escapeCharIndex, andCharIndex) < chunk.length - 3) {
                send(buffer)
                buffer = ""
            }
        } else {
            send(chunk)
        }
    }
}


/**
 * Combining chunks that may contain tags
 */
private fun Flow<String>.detectAndCombineBoldTags(): Flow<String> = channelFlow {
    var buffer = ""

    this@detectAndCombineBoldTags.collect { chunk ->


        val tagStartIndex = chunk.indexOf("<")

        when {
            tagStartIndex >= 0 || buffer.isNotEmpty() -> {
                buffer += chunk
                if ((buffer.length - tagStartIndex) >= "</b>".length) {
                    send(buffer)
                    buffer = ""
                }
            }

            else -> {
                send(chunk)
            }
        }
    }
}

private fun String.letters() = filter { it.isLetter() }
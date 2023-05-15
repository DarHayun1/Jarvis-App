package com.darh.jarvisapp.api

import com.darh.jarvisapp.unescapeSpecialChars

sealed class  CompletionState {
    data class Typing(val content: String) : CompletionState()
    data class Metadata(val fields: Map<String, Any>, val requested: List<String>) :
        CompletionState() {

        fun getList(key: String): List<String>? =
            (fields[key] as? List<*>)?.mapNotNull { it?.toString()?.unescapeSpecialChars() }

        fun getString(key: String): String? = (fields[key] as? String)?.unescapeSpecialChars()

        fun isUiReady() = requested.all { key ->
            !getList(key).isNullOrEmpty() || !getString(key).isNullOrBlank()
        }
    }

    data class Complete(val rawResponse: String, val structuredResponse: StructuredChatResponse) :
        CompletionState()
}

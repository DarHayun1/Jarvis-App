package com.darh.jarvisapp.google

import com.google.gson.annotations.SerializedName

data class SearchResultResponse(@SerializedName("items") val items: List<SearchResult>? = null) {

    data class SearchResult(
        @SerializedName("title") val title: String? = null,
        @SerializedName("link") val link: String? = null,
        @SerializedName("snippet") val snippet: String? = null
    ){
        fun toStringI(index:Int): String {
            return "SearchResult(index=$index, title=$title, snippet=$snippet)"
        }
    }
}
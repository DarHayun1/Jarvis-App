package com.darh.jarvisapp.google

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSearchRepository @Inject constructor() {

    private val googleSearchApi: GoogleSearchApi

    companion object {
        private const val GOOGLE_SEARCH_API_BASE_URL = "https://www.googleapis.com/"
    }

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(GOOGLE_SEARCH_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        googleSearchApi = retrofit.create(GoogleSearchApi::class.java)
    }

    suspend fun search(query: String, apiKey: String, searchEngineId: String): SearchResultResponse {
        return withContext(Dispatchers.IO) {
             googleSearchApi.getSearchResults(apiKey, searchEngineId, query)
        }
    }

    interface GoogleSearchApi {
        @GET("customsearch/v1")
        suspend fun getSearchResults(
            @Query("key") apiKey: String,
            @Query("cx") searchEngineId: String,
            @Query("q") query: String
        ): SearchResultResponse
    }
}

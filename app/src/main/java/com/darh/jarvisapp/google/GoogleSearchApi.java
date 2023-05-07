package com.darh.jarvisapp.google;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleSearchApi {
    @GET("customsearch/v1")
    Call<SearchResultResponse> getSearchResults(
            @Query("key") String apiKey,
            @Query("cx") String searchEngineId,
            @Query("q") String query
    );
}

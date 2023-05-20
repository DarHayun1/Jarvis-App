package com.darh.jarvisapp.api.agent

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface JarvisAgentAPI {
    @FormUrlEncoded
    @POST("main_agent/send_message/")
    suspend fun sendMessage(@Field("message") message: String): AgentCompletionResponse

    @GET("main_agent/get_request_state/")
    suspend fun getRequestState(@Query("request_id") requestId: String): AgentCompletionResponse

    companion object {
        fun create(): JarvisAgentAPI = Retrofit.Builder()
            .baseUrl("http://checke-agent-api-env.eba-2rxci4wa.eu-north-1.elasticbeanstalk.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JarvisAgentAPI::class.java)
    }
}


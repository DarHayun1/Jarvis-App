package com.darh.jarvisapp.api.agent

data class AgentCompletionResponse(
    val id: Int,
    val state: String,
    val message: String,
    val steps: List<ResponseStep>?
)

data class ResponseStep(
    val id: Int,
    val request: Int,
    val type: String,
    val content: String
)

package com.darh.jarvisapp.chat

import android.content.Context
import android.speech.tts.TextToSpeech
import com.darh.jarvisapp.api.agent.JarvisAgentAPI
import com.darh.jarvisapp.api.agent.ResponseStep
import com.darh.jarvisapp.api.tools.AgentTool
import com.darh.jarvisapp.chat.repo.ChatRequestsManager
import com.darh.jarvisapp.chat.repo.GoogleResultsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject


internal class Agent @Inject constructor(
    private val googleResultsUseCase: GoogleResultsUseCase,
    private val agentUseCase: AgentUseCase,
    private val agentAPI: JarvisAgentAPI,
    private val requestsManager: ChatRequestsManager
) : TextToSpeech.OnInitListener {

    var ttsService: TextToSpeech? = null

    suspend fun executeTask(message: String, context: Context?): Flow<String> {
        return flow {
            withContext(Dispatchers.IO) {
                val steps = mutableListOf<ResponseStep>()
                val sendResponse = agentAPI.sendMessage(message)

                if (sendResponse.state == "P") {
                    do {
                        delay(1000) // wait for 1 second
                        val pollResponse =
                            runCatching { agentAPI.getRequestState(sendResponse.id.toString()) }.getOrNull()
                        pollResponse?.steps
                            ?.filter { newStep -> steps.any { newStep == it } }
                            ?.forEach { emit("${it.type}: ${it.content}") }
                        when (pollResponse?.state) {
                            "C" -> {
                                println("Completed: $pollResponse")
                                break
                            }

                            "F" -> {
                                println("Failed: $pollResponse")
                                break
                            }
                        }
                    } while (true)
                }
            }
        }
    }
//        fun executeTask(input: String, context: Context?): Flow<String> {
//
//        return flow {
//            context?.let {  ttsService = TextToSpeech(context, this@Agent) }
//
//            val history = ChatHistory()
//            history.add(ChatMessage(ChatRole.User, input))
//            var toolResponse: String?
//            do {
//                val initialResult = agentUseCase.provideToolResult(history.messages, input)
//                initialResult?.thought?.let {
//                    emit("Thought: $it")
//                    speakText("Thought: $it")
//                    history.add(ChatMessage(ChatRole.Assistant, it))
//                }
//                toolResponse = initialResult?.nextTool?.let {
//                    emit("[$it]")
//                    speakText("Tool used: $it")
//                    handleToolRequest(it)
//                }
//
//                toolResponse?.let {
//                    history.add(ChatMessage(ChatRole.Assistant, "Tool usage result: $toolResponse"))
//                }
//            } while (toolResponse != null)
//
//        }.onCompletion {
//            ttsService?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//                override fun onStart(utteranceId: String?) { // NoOp
//                }
//
//                override fun onDone(utteranceId: String?) {
//                    ttsService?.stop()
//                    ttsService?.shutdown()
//                    ttsService = null
//                }
//
//                override fun onError(utteranceId: String?) {
//                    Timber.tag(OPEN_AI).e("TTS Error: $utteranceId")
//                }
//
//            })
//        }
//    }

    fun speakText(text: String) {
        ttsService?.speak(text, TextToSpeech.QUEUE_ADD, null, "ID")
    }

    //TTS Init
    override fun onInit(status: Int) {
        if (status != TextToSpeech.ERROR) {
            ttsService?.language = Locale.UK

        }
    }

    private suspend fun handleToolRequest(request: String): String? {
        val separatorIndex = request.indexOf(":")
        val name = request.substring(0, separatorIndex)
        val tool = AgentTool.values().find { it.toolName == name }
        val input = request.substring(separatorIndex + 1, request.length)
        return when (tool) {
            AgentTool.GoogleSearch -> googleResultsUseCase.getInfo(input)
            AgentTool.FinalAnswer, AgentTool.AskUser -> null
            else -> "Yes"
        }
    }
}

sealed class AgentTaskState {

}

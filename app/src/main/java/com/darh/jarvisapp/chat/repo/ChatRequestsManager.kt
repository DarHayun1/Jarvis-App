package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.di.AppScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ChatRequestsManager @Inject constructor(
    // Must be a SupervisorScope
    private val appScope: AppScope
) {

    private var latestRequest: Job? = null

    fun launchRequest(
        requestBlock: suspend () -> Unit,
        onError: suspend (Throwable) -> Unit,
        onCompletion: (Throwable?) -> Unit
    ) {
        appScope.launch {
            stopRunningRequestSync()

            val newRequest = launch() {
                runCatching {
                    requestBlock()
                }.onFailure {
                    if (it is CancellationException) {
                        throw it
                    } else {
                        onError(it)
                    }
                }
            }
            latestRequest = newRequest
            newRequest.invokeOnCompletion { e ->
                latestRequest = null
                onCompletion(e)
            }
        }
    }

    suspend fun stopRunningRequestSync(oldRequest: Job? = latestRequest) {
        oldRequest?.cancel(StopGenerationException())
        runCatching { oldRequest?.join() }
        latestRequest = null
    }

    fun stopAndClearRunningRequest() {
        latestRequest?.cancel(
            StopGenerationException()
        )
        latestRequest = null
    }

    suspend fun stopCurrentSessionSync() {
        latestRequest?.cancel(StopSessionException)
        runCatching { latestRequest?.join() }
        latestRequest = null
    }
}

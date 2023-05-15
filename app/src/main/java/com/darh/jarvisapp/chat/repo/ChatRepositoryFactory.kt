package com.darh.jarvisapp.chat.repo

import android.content.Context
import com.darh.jarvisapp.chat.Agent
import com.darh.jarvisapp.chat.AgentUseCase
import com.darh.jarvisapp.google.GoogleSearchRepository
import com.darh.jarvisapp.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChatRepositoryFactory @Inject constructor(
    private val askAnythingUseCase: AskAnythingUseCase,
    private val googleResultsUseCase: GoogleResultsUseCase,
    private val agent: Agent,
    private val requestsManager: ChatRequestsManager,
    private val appScope: AppScope,
    private val googleRepository: GoogleSearchRepository,
    @ApplicationContext private val context: Context
) {
    fun create(chatId: Int) = ChatRepository(
        askAnythingUseCase,
        googleResultsUseCase,
        agent,
        requestsManager,
        appScope,
        googleRepository,
        context
    )
}

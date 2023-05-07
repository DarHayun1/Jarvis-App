package com.darh.jarvisapp.chat.repo

import android.content.Context
import com.darh.jarvisapp.google.GoogleSearchRepository
import com.darh.jarvisapp.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChatRepositoryFactory @Inject constructor(
    private val askAnythingUseCase: AskAnythingUseCase,
    private val googleResultsUseCase: GoogleResultsUseCase,
    private val requestsManager: ChatRequestsManager,
    private val appScope: AppScope,
    private val googleRepository: GoogleSearchRepository
) {
    fun create(chatId: Int) = ChatRepository(
        askAnythingUseCase,
        googleResultsUseCase,
        requestsManager,
        appScope,
        googleRepository,
    )
}

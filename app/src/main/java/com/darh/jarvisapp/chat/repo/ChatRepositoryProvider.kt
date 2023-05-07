package com.darh.jarvisapp.chat.repo

import com.darh.jarvisapp.ui.viewmodel.AssistanceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChatRepositoryProvider @Inject constructor(private val uiStateUseCaseFactory: ChatRepositoryFactory) {

    private val useCases = mutableMapOf<Int, ChatRepository>()

    fun provide(
        id: Int
    ): ChatRepository {
        val useCase = useCases.getOrPut(id) {
            uiStateUseCaseFactory.create(id)
        }
        useCase.init()
        return useCase
    }

    fun provideOrNull(id: Int) = useCases[id]
}

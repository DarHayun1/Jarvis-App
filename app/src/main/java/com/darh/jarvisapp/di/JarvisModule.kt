package com.darh.jarvisapp.di

import com.darh.jarvisapp.api.ChatCompletionAPI
import com.darh.jarvisapp.api.OpenAILibraryImpl
import com.darh.jarvisapp.api.agent.JarvisAgentAPI
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class JarvisModule {

    @Provides
    @Singleton
    fun provideAgentAPI() = JarvisAgentAPI.create()
}

@InstallIn(SingletonComponent::class)
@Module
abstract class JarvisModuleBind {

    @Binds
    @Singleton
    abstract fun completionApi(impk: OpenAILibraryImpl): ChatCompletionAPI
}
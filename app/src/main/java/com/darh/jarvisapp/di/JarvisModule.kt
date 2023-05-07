package com.darh.jarvisapp.di

import com.darh.jarvisapp.api.ChatCompletionAPI
import com.darh.jarvisapp.api.OpenAILibraryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class JarvisModule {
}

@InstallIn(SingletonComponent::class)
@Module
abstract class JarvisModuleBind {

    @Binds
    @Singleton
    abstract fun completionApi(impk: OpenAILibraryImpl): ChatCompletionAPI
}
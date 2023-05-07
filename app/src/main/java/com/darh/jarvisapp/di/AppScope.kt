package com.darh.jarvisapp.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class AppScope @Inject constructor() : CoroutineScope by MainScope()

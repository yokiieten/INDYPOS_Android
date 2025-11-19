package com.indybrain.indypos_Android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * UseCase Module
 * Note: UseCases are automatically provided by Hilt via constructor injection
 * This module is kept for future use cases that might need special setup
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule


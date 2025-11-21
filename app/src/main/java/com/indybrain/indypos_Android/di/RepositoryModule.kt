package com.indybrain.indypos_Android.di

import android.content.Context
import android.content.SharedPreferences
import com.indybrain.indypos_Android.data.repository.AuthRepositoryImpl
import com.indybrain.indypos_Android.data.repository.OrderRepositoryImpl
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("indypos_prefs", Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository {
        return authRepositoryImpl
    }
    
    @Provides
    @Singleton
    fun provideOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository {
        return orderRepositoryImpl
    }
}


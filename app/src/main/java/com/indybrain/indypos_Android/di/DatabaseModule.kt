package com.indybrain.indypos_Android.di

import android.content.Context
import androidx.room.Room
import com.indybrain.indypos_Android.data.local.dao.OrderAddonDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.database.IndyPosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): IndyPosDatabase {
        return Room.databaseBuilder(
            context,
            IndyPosDatabase::class.java,
            "indypos_database"
        ).build()
    }
    
    @Provides
    fun provideOrderDao(database: IndyPosDatabase): OrderDao {
        return database.orderDao()
    }
    
    @Provides
    fun provideOrderItemDao(database: IndyPosDatabase): OrderItemDao {
        return database.orderItemDao()
    }
    
    @Provides
    fun provideOrderAddonDao(database: IndyPosDatabase): OrderAddonDao {
        return database.orderAddonDao()
    }
}


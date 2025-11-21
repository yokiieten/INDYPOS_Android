package com.indybrain.indypos_Android.di

import android.content.Context
import androidx.room.Room
import com.indybrain.indypos_Android.data.local.dao.*
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
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
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
    
    @Provides
    fun provideCategoryDao(database: IndyPosDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    fun provideProductDao(database: IndyPosDatabase): ProductDao {
        return database.productDao()
    }
    
    @Provides
    fun provideAddonGroupDao(database: IndyPosDatabase): AddonGroupDao {
        return database.addonGroupDao()
    }
    
    @Provides
    fun provideAddonDao(database: IndyPosDatabase): AddonDao {
        return database.addonDao()
    }
}


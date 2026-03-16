package com.indybrain.indypos_Android.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.indybrain.indypos_Android.BuildConfig
import com.indybrain.indypos_Android.data.remote.api.AuthApi
import com.indybrain.indypos_Android.data.remote.api.GraphApi
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import com.indybrain.indypos_Android.data.remote.interceptor.AuthInterceptor
import com.indybrain.indypos_Android.data.remote.interceptor.CurlLoggingInterceptor
import com.indybrain.indypos_Android.data.remote.interceptor.UnauthorizedInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)

        if (BuildConfig.ENABLE_HTTP_LOGGING) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(CurlLoggingInterceptor())
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideOrdersApi(retrofit: Retrofit): OrdersApi {
        return retrofit.create(OrdersApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideProductsApi(retrofit: Retrofit): ProductsApi {
        return retrofit.create(ProductsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGraphApi(retrofit: Retrofit): GraphApi {
        return retrofit.create(GraphApi::class.java)
    }
}


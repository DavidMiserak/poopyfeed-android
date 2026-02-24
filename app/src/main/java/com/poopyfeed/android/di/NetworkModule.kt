package com.poopyfeed.android.di

import com.poopyfeed.android.BuildConfig
import com.poopyfeed.android.data.local.TokenManager
import com.poopyfeed.android.data.remote.AuthApi
import com.poopyfeed.android.data.remote.ChildrenApi
import com.poopyfeed.android.data.remote.CookieStore
import com.poopyfeed.android.data.remote.ProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            if (url.contains("/api/v1/") && !url.contains("/auth/")) {
                val token = runBlocking { tokenManager.getToken() }
                if (token != null) {
                    val newRequest =
                        request.newBuilder()
                            .addHeader("Authorization", "Token $token")
                            .build()
                    return@Interceptor chain.proceed(newRequest)
                }
            }

            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieStore: CookieStore,
        authInterceptor: Interceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieStore)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level =
                        if (BuildConfig.DEBUG) {
                            HttpLoggingInterceptor.Level.BODY
                        } else {
                            HttpLoggingInterceptor.Level.NONE
                        }
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi {
        return retrofit.create(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChildrenApi(retrofit: Retrofit): ChildrenApi {
        return retrofit.create(ChildrenApi::class.java)
    }
}

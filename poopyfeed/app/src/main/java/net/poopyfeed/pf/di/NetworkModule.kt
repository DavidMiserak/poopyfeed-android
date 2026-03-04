package net.poopyfeed.pf.di

import android.content.Context
import android.content.SharedPreferences
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import net.poopyfeed.pf.BuildConfig
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Manual DI provider for network dependencies.
 * Provides Retrofit, OkHttpClient, and the API service.
 *
 * Token authentication:
 * - Token is stored in SharedPreferences under key "auth_token"
 * - AuthInterceptor reads it on every request and adds "Authorization: Token <value>"
 * - BaseUrl: http://10.0.2.2:8000/api/v1/ (Android emulator -> host localhost)
 */
object NetworkModule {

    private var sharedPreferences: SharedPreferences? = null
    private var json: Json? = null
    private var okHttpClient: OkHttpClient? = null
    private var retrofit: Retrofit? = null
    private var apiService: PoopyFeedApiService? = null

    fun provideSharedPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: context.applicationContext.getSharedPreferences(
            "poopyfeed_prefs",
            Context.MODE_PRIVATE
        ).also { sharedPreferences = it }
    }

    fun provideJson(): Json {
        return json ?: Json {
            ignoreUnknownKeys = true
        }.also { json = it }
    }

    fun provideOkHttpClient(context: Context): OkHttpClient {
        return okHttpClient ?: run {
            val prefs = provideSharedPreferences(context)

            val authInterceptor = Interceptor { chain ->
                val token = prefs.getString("auth_token", null)
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Token $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()
                .also { okHttpClient = it }
        }
    }

    /**
     * Retrofit instance for PoopyFeed API.
     * Uses 10.0.2.2 which is the Android emulator alias for host localhost.
     */
    fun provideRetrofit(context: Context): Retrofit {
        return retrofit ?: run {
            val contentType = "application/json".toMediaType()
            Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(provideOkHttpClient(context))
                .addConverterFactory(provideJson().asConverterFactory(contentType))
                .build()
                .also { retrofit = it }
        }
    }

    fun providePoopyFeedApiService(context: Context): PoopyFeedApiService {
        return apiService ?: provideRetrofit(context)
            .create(PoopyFeedApiService::class.java)
            .also { apiService = it }
    }
}

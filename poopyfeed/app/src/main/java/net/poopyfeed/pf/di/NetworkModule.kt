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

    private fun getAuthTokenInternal(prefs: SharedPreferences): String? =
        prefs.getString("auth_token", null)

    fun saveAuthToken(context: Context, token: String) {
        val prefs = provideSharedPreferences(context)
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(context: Context): String? =
        getAuthTokenInternal(provideSharedPreferences(context))

    fun clearAuthToken(context: Context) {
        val prefs = provideSharedPreferences(context)
        prefs.edit().remove("auth_token").apply()
    }

    fun provideOkHttpClient(context: Context): OkHttpClient {
        return okHttpClient ?: run {
            val prefs = provideSharedPreferences(context)

            val authInterceptor = Interceptor { chain ->
                val token = getAuthTokenInternal(prefs)
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

            val cookieJar = object : okhttp3.CookieJar {
                private val cookies: MutableList<okhttp3.Cookie> = mutableListOf()

                override fun saveFromResponse(
                    url: okhttp3.HttpUrl,
                    cookies: List<okhttp3.Cookie>
                ) {
                    this.cookies.removeAll { it.matches(url) }
                    this.cookies.addAll(cookies)
                }

                override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                    return cookies.filter { it.matches(url) }
                }
            }

            OkHttpClient.Builder()
                .cookieJar(cookieJar)
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

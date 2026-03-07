package net.poopyfeed.pf.di

import android.content.Context
import android.content.SharedPreferences
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import net.poopyfeed.pf.BuildConfig
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

private const val PREFS_NAME = "poopyfeed_prefs"

/**
 * Hilt module for network dependencies. Provides Retrofit, OkHttpClient, and the API service.
 *
 * Token authentication: Auth interceptor reads token from SharedPreferences (same prefs as
 * [TokenManager]) and adds "Authorization: Token <value>".
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
      context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  @Provides @Singleton fun provideJson(): Json = Json { ignoreUnknownKeys = true }

  @Provides
  @Singleton
  internal fun providePersistentCookieJar(
      prefs: SharedPreferences,
      json: Json,
  ): PersistentCookieJar = PersistentCookieJar(prefs, json)

  @Provides
  @Singleton
  internal fun provideOkHttpClient(
      prefs: SharedPreferences,
      json: Json,
      cookieJar: PersistentCookieJar,
      @ApplicationContext context: Context,
  ): OkHttpClient {
    val connectivityInterceptor = ConnectivityInterceptor(context, BuildConfig.API_BASE_URL)
    val authInterceptor = Interceptor { chain ->
      val token = prefs.getString("auth_token", null)
      val request =
          if (token != null) {
            chain.request().newBuilder().header("Authorization", "Token $token").build()
          } else {
            chain.request()
          }
      chain.proceed(request)
    }
    val logging =
        HttpLoggingInterceptor().apply {
          level =
              if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
              else HttpLoggingInterceptor.Level.NONE
        }
    return OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(connectivityInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
  }

  @Provides
  @Singleton
  fun providePoopyFeedApiService(retrofit: Retrofit): PoopyFeedApiService =
      retrofit.create(PoopyFeedApiService::class.java)

  @Provides @Singleton @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

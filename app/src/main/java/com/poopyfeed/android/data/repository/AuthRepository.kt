package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.local.TokenManager
import com.poopyfeed.android.data.remote.AuthApi
import com.poopyfeed.android.data.remote.CookieStore
import com.poopyfeed.android.data.remote.dto.LoginRequest
import com.poopyfeed.android.data.remote.dto.SignupRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository
    @Inject
    constructor(
        private val authApi: AuthApi,
        private val tokenManager: TokenManager,
        private val cookieStore: CookieStore,
    ) {
        val tokenFlow: Flow<String?> = tokenManager.tokenFlow

        suspend fun login(
            email: String,
            password: String,
        ): Result<Unit> {
            return try {
                val loginResponse = authApi.login(LoginRequest(email, password))
                if (!loginResponse.isSuccessful) {
                    return Result.failure(Exception(parseErrorBody(loginResponse.errorBody()?.string())))
                }

                val tokenResponse = authApi.getToken()
                if (!tokenResponse.isSuccessful) {
                    return Result.failure(Exception("Failed to retrieve auth token"))
                }

                val token =
                    tokenResponse.body()?.authToken
                        ?: return Result.failure(Exception("Empty token response"))

                tokenManager.saveToken(token)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(getNetworkErrorMessage(e)))
            }
        }

        suspend fun signup(
            email: String,
            password: String,
        ): Result<Unit> {
            return try {
                val signupResponse = authApi.signup(SignupRequest(email, password))
                if (!signupResponse.isSuccessful) {
                    return Result.failure(Exception(parseErrorBody(signupResponse.errorBody()?.string())))
                }

                val tokenResponse = authApi.getToken()
                if (!tokenResponse.isSuccessful) {
                    return Result.failure(Exception("Failed to retrieve auth token"))
                }

                val token =
                    tokenResponse.body()?.authToken
                        ?: return Result.failure(Exception("Empty token response"))

                tokenManager.saveToken(token)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(getNetworkErrorMessage(e)))
            }
        }

        suspend fun logout(): Result<Unit> {
            return try {
                authApi.logout()
                tokenManager.clearToken()
                cookieStore.clear()
                Result.success(Unit)
            } catch (_: Exception) {
                // Clear local state even if API call fails
                tokenManager.clearToken()
                cookieStore.clear()
                Result.success(Unit)
            }
        }

        suspend fun hasToken(): Boolean {
            return tokenManager.getToken() != null
        }

        companion object {
            private val json = Json { ignoreUnknownKeys = true }

            fun parseErrorBody(errorBody: String?): String {
                if (errorBody.isNullOrBlank()) return RepositoryConstants.UNKNOWN_ERROR_MESSAGE

                return try {
                    val jsonObject = json.decodeFromString<JsonObject>(errorBody)

                    // Check non_field_errors first
                    jsonObject["non_field_errors"]?.jsonArray?.let { errors ->
                        return errors.joinToString(". ") { it.jsonPrimitive.content }
                    }

                    // Check detail field
                    jsonObject["detail"]?.jsonPrimitive?.content?.let { return it }

                    // Check field-specific errors
                    val fieldErrors = mutableListOf<String>()
                    for ((key, value) in jsonObject) {
                        try {
                            val messages = value.jsonArray.map { it.jsonPrimitive.content }
                            fieldErrors.addAll(messages)
                        } catch (_: Exception) {
                            // Skip non-array fields
                        }
                    }

                    if (fieldErrors.isNotEmpty()) {
                        fieldErrors.joinToString(". ")
                    } else {
                        RepositoryConstants.UNKNOWN_ERROR_MESSAGE
                    }
                } catch (_: Exception) {
                    RepositoryConstants.UNKNOWN_ERROR_MESSAGE
                }
            }

            fun getNetworkErrorMessage(e: Exception): String {
                return when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "No internet connection. Please check your network."
                    e.message?.contains("timeout") == true ->
                        "Request timed out. Please try again."
                    e.message?.contains("Connection refused") == true ->
                        "Cannot reach the server. Please try again later."
                    else -> e.message ?: "An unexpected error occurred"
                }
            }
        }
    }

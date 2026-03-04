package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.local.TokenManager
import com.poopyfeed.android.data.remote.ProfileApi
import com.poopyfeed.android.data.remote.dto.ChangePasswordRequest
import com.poopyfeed.android.data.remote.dto.DeleteAccountRequest
import com.poopyfeed.android.data.remote.dto.UpdateProfileRequest
import com.poopyfeed.android.data.remote.dto.UserProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository
    @Inject
    constructor(
        private val profileApi: ProfileApi,
        private val tokenManager: TokenManager,
    ) {
        suspend fun getProfile(): Result<UserProfile> {
            return try {
                val response = profileApi.getProfile()
                if (!response.isSuccessful) {
                    return Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
                }

                val profile =
                    response.body()
                        ?: return Result.failure(Exception("Empty profile response"))

                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(Exception(getNetworkErrorMessage(e)))
            }
        }

        suspend fun updateProfile(
            firstName: String,
            lastName: String,
            timezone: String,
        ): Result<UserProfile> {
            return try {
                val request =
                    UpdateProfileRequest(
                        firstName = firstName,
                        lastName = lastName,
                        timezone = timezone,
                    )
                val response = profileApi.updateProfile(request)
                if (!response.isSuccessful) {
                    return Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
                }

                val profile =
                    response.body()
                        ?: return Result.failure(Exception("Empty profile response"))

                Result.success(profile)
            } catch (e: Exception) {
                Result.failure(Exception(getNetworkErrorMessage(e)))
            }
        }

        suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
            newPasswordConfirm: String,
        ): Result<Unit> {
            return try {
                val request =
                    ChangePasswordRequest(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        newPasswordConfirm = newPasswordConfirm,
                    )
                val response = profileApi.changePassword(request)
                if (!response.isSuccessful) {
                    return Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
                }

                val passwordResponse =
                    response.body()
                        ?: return Result.failure(Exception("Empty password response"))

                // Rotate token on successful password change
                tokenManager.saveToken(passwordResponse.authToken)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(getNetworkErrorMessage(e)))
            }
        }

        suspend fun deleteAccount(currentPassword: String): Result<Unit> {
            return try {
                val request = DeleteAccountRequest(currentPassword = currentPassword)
                val response = profileApi.deleteAccount(request)
                if (!response.isSuccessful) {
                    return Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
                }

                // Clear token on successful account deletion
                tokenManager.clearToken()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(getNetworkErrorMessage(e)))
            }
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
                val msg = e.message?.lowercase() ?: ""
                return when {
                    msg.contains("unable to resolve host") ->
                        "No internet connection. Please check your network."
                    msg.contains("timeout") ->
                        "Request timed out. Please try again."
                    msg.contains("connection refused") || msg.contains("failed to connect") ->
                        "Cannot reach the server. Ensure backend is running (make run). On a physical device, set api.base.url in android/local.properties to your computer's IP."
                    else -> e.message ?: "An unexpected error occurred"
                }
            }
        }
    }

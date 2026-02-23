package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.ChildrenApi
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.CreateChildRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildrenRepository @Inject constructor(
    private val childrenApi: ChildrenApi,
) {

    suspend fun getChildren(): Result<List<Child>> {
        return try {
            val response = childrenApi.getChildren()
            if (!response.isSuccessful) {
                return Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
            }

            val childrenResponse = response.body()
                ?: return Result.failure(Exception("Empty children response"))

            Result.success(childrenResponse.results)
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    suspend fun createChild(
        name: String,
        dateOfBirth: String,
        gender: String? = null,
    ): Result<Child> {
        return try {
            val request = CreateChildRequest(
                name = name,
                dateOfBirth = dateOfBirth,
                gender = gender,
            )
            val response = childrenApi.createChild(request)
            if (!response.isSuccessful) {
                return Result.failure(Exception(parseErrorBody(response.errorBody()?.string())))
            }

            val child = response.body()
                ?: return Result.failure(Exception("Empty child response"))

            Result.success(child)
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parseErrorBody(errorBody: String?): String {
            if (errorBody.isNullOrBlank()) return "An unknown error occurred"

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
                    "An unknown error occurred"
                }
            } catch (_: Exception) {
                "An unknown error occurred"
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

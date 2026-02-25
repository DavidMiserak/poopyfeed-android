package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.FeedingsApi
import com.poopyfeed.android.data.remote.dto.CreateFeedingRequest
import com.poopyfeed.android.data.remote.dto.Feeding
import com.poopyfeed.android.data.remote.dto.UpdateFeedingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedingsRepository
    @Inject
    constructor(
        private val feedingsApi: FeedingsApi,
    ) {
        suspend fun getFeedings(childId: Int): Result<List<Feeding>> {
            return try {
                val response = feedingsApi.getFeedings(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body =
                    response.body()
                        ?: return Result.failure(Exception("Empty feedings response"))
                val list = body.results
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun getFeeding(
            childId: Int,
            id: Int,
        ): Result<Feeding> {
            return try {
                val response = feedingsApi.getFeeding(childId, id)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val feeding =
                    response.body()
                        ?: return Result.failure(Exception("Empty feeding response"))
                Result.success(feeding)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun createFeeding(
            childId: Int,
            feedingType: String,
            fedAt: String,
            amountOz: String? = null,
            durationMinutes: Int? = null,
            side: String? = null,
        ): Result<Feeding> {
            return try {
                val request =
                    CreateFeedingRequest(
                        feedingType = feedingType,
                        fedAt = fedAt,
                        amountOz = amountOz,
                        durationMinutes = durationMinutes,
                        side = side,
                    )
                val response = feedingsApi.createFeeding(childId, request)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val feeding =
                    response.body()
                        ?: return Result.failure(Exception("Empty feeding response"))
                Result.success(feeding)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun updateFeeding(
            childId: Int,
            id: Int,
            feedingType: String? = null,
            fedAt: String? = null,
            amountOz: String? = null,
            durationMinutes: Int? = null,
            side: String? = null,
        ): Result<Feeding> {
            return try {
                val request =
                    UpdateFeedingRequest(
                        feedingType = feedingType,
                        fedAt = fedAt,
                        amountOz = amountOz,
                        durationMinutes = durationMinutes,
                        side = side,
                    )
                val response = feedingsApi.updateFeeding(childId, id, request)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val feeding =
                    response.body()
                        ?: return Result.failure(Exception("Empty feeding response"))
                Result.success(feeding)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun deleteFeeding(
            childId: Int,
            id: Int,
        ): Result<Unit> {
            return try {
                val response = feedingsApi.deleteFeeding(childId, id)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }
    }

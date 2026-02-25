package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.DiapersApi
import com.poopyfeed.android.data.remote.dto.CreateDiaperRequest
import com.poopyfeed.android.data.remote.dto.Diaper
import com.poopyfeed.android.data.remote.dto.UpdateDiaperRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiapersRepository
    @Inject
    constructor(
        private val diapersApi: DiapersApi,
    ) {
        suspend fun getDiapers(childId: Int): Result<List<Diaper>> {
            return try {
                val response = diapersApi.getDiapers(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body =
                    response.body()
                        ?: return Result.failure(Exception("Empty diapers response"))
                Result.success(body.results)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun getDiaper(
            childId: Int,
            id: Int,
        ): Result<Diaper> {
            return try {
                val response = diapersApi.getDiaper(childId, id)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val diaper =
                    response.body()
                        ?: return Result.failure(Exception("Empty diaper response"))
                Result.success(diaper)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun createDiaper(
            childId: Int,
            changeType: String,
            changedAt: String,
        ): Result<Diaper> {
            return try {
                val request = CreateDiaperRequest(changeType = changeType, changedAt = changedAt)
                val response = diapersApi.createDiaper(childId, request)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val diaper =
                    response.body()
                        ?: return Result.failure(Exception("Empty diaper response"))
                Result.success(diaper)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun updateDiaper(
            childId: Int,
            id: Int,
            changeType: String? = null,
            changedAt: String? = null,
        ): Result<Diaper> {
            return try {
                val request = UpdateDiaperRequest(changeType = changeType, changedAt = changedAt)
                val response = diapersApi.updateDiaper(childId, id, request)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val diaper =
                    response.body()
                        ?: return Result.failure(Exception("Empty diaper response"))
                Result.success(diaper)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun deleteDiaper(
            childId: Int,
            id: Int,
        ): Result<Unit> {
            return try {
                val response = diapersApi.deleteDiaper(childId, id)
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

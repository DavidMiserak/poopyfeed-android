package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.NapsApi
import com.poopyfeed.android.data.remote.dto.CreateNapRequest
import com.poopyfeed.android.data.remote.dto.Nap
import com.poopyfeed.android.data.remote.dto.UpdateNapRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NapsRepository
    @Inject
    constructor(
        private val napsApi: NapsApi,
    ) {
        suspend fun getNaps(childId: Int): Result<List<Nap>> {
            return try {
                val response = napsApi.getNaps(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body =
                    response.body()
                        ?: return Result.failure(Exception("Empty naps response"))
                Result.success(body.results)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun getNap(
            childId: Int,
            id: Int,
        ): Result<Nap> {
            return try {
                val response = napsApi.getNap(childId, id)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val nap =
                    response.body()
                        ?: return Result.failure(Exception("Empty nap response"))
                Result.success(nap)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun createNap(
            childId: Int,
            nappedAt: String,
        ): Result<Nap> {
            return try {
                val request = CreateNapRequest(nappedAt = nappedAt)
                val response = napsApi.createNap(childId, request)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val nap =
                    response.body()
                        ?: return Result.failure(Exception("Empty nap response"))
                Result.success(nap)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun updateNap(
            childId: Int,
            id: Int,
            nappedAt: String? = null,
        ): Result<Nap> {
            return try {
                val request = UpdateNapRequest(nappedAt = nappedAt)
                val response = napsApi.updateNap(childId, id, request)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val nap =
                    response.body()
                        ?: return Result.failure(Exception("Empty nap response"))
                Result.success(nap)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun deleteNap(
            childId: Int,
            id: Int,
        ): Result<Unit> {
            return try {
                val response = napsApi.deleteNap(childId, id)
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

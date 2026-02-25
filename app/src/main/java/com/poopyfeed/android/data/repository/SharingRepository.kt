package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.AcceptInviteRequest
import com.poopyfeed.android.data.remote.InvitesApi
import com.poopyfeed.android.data.remote.SharingApi
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.CreateInviteRequest
import com.poopyfeed.android.data.remote.dto.Invite
import com.poopyfeed.android.data.remote.dto.Share
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharingRepository
    @Inject
    constructor(
        private val sharingApi: SharingApi,
        private val invitesApi: InvitesApi,
    ) {
        suspend fun getShares(childId: Int): Result<List<Share>> {
            return try {
                val response = sharingApi.getShares(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body() ?: emptyList())
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun revokeShare(
            childId: Int,
            shareId: Int,
        ): Result<Unit> {
            return try {
                val response = sharingApi.revokeShare(childId, shareId)
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

        suspend fun getInvites(childId: Int): Result<List<Invite>> {
            return try {
                val response = sharingApi.getInvites(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body() ?: emptyList())
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun createInvite(
            childId: Int,
            role: String,
        ): Result<Invite> {
            return try {
                val response = sharingApi.createInvite(childId, CreateInviteRequest(role = role))
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body()!!)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun deleteInvite(
            childId: Int,
            inviteId: Int,
        ): Result<Unit> {
            return try {
                val response = sharingApi.deleteInvite(childId, inviteId)
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

        suspend fun acceptInvite(token: String): Result<Child> {
            return try {
                val response = invitesApi.acceptInvite(AcceptInviteRequest(token = token))
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body()!!)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }
    }

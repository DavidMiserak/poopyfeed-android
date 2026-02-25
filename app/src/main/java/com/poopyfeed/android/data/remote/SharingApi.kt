package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.CreateInviteRequest
import com.poopyfeed.android.data.remote.dto.Invite
import com.poopyfeed.android.data.remote.dto.Share
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface SharingApi {
    @GET("api/v1/children/{childId}/shares/")
    suspend fun getShares(
        @Path("childId") childId: Int,
    ): Response<List<Share>>

    @DELETE("api/v1/children/{childId}/shares/{shareId}/")
    suspend fun revokeShare(
        @Path("childId") childId: Int,
        @Path("shareId") shareId: Int,
    ): Response<Unit>

    @GET("api/v1/children/{childId}/invites/")
    suspend fun getInvites(
        @Path("childId") childId: Int,
    ): Response<List<Invite>>

    @POST("api/v1/children/{childId}/invites/")
    suspend fun createInvite(
        @Path("childId") childId: Int,
        @Body request: CreateInviteRequest,
    ): Response<Invite>

    @PATCH("api/v1/children/{childId}/invites/{inviteId}/")
    suspend fun toggleInvite(
        @Path("childId") childId: Int,
        @Path("inviteId") inviteId: Int,
    ): Response<Invite>

    @DELETE("api/v1/children/{childId}/invites/{inviteId}/delete/")
    suspend fun deleteInvite(
        @Path("childId") childId: Int,
        @Path("inviteId") inviteId: Int,
    ): Response<Unit>
}

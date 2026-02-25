package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.Child
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface InvitesApi {
    @POST("api/v1/invites/accept/")
    suspend fun acceptInvite(
        @Body body: AcceptInviteRequest,
    ): Response<Child>
}

@kotlinx.serialization.Serializable
data class AcceptInviteRequest(
    val token: String,
)

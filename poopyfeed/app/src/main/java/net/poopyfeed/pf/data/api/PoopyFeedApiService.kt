package net.poopyfeed.pf.data.api

import net.poopyfeed.pf.data.models.*
import retrofit2.http.*

/**
 * PoopyFeed API service interface for Django REST API at localhost:8000/api/v1/
 *
 * All endpoints use suspend functions for coroutine-based async operations. Token authentication is
 * added via OkHttp interceptor.
 */
interface PoopyFeedApiService {

  // ========================
  // Authentication Endpoints
  // ========================

  /**
   * Step 1: Session login with email and password. Uses django-allauth headless browser login
   * endpoint.
   */
  @Headers("Accept: application/json", "X-Requested-With: XMLHttpRequest")
  @POST("browser/v1/auth/login")
  suspend fun sessionLogin(@Body request: LoginRequest): SessionLoginResponse

  /** Sign up a new user with email and password. Uses django-allauth headless signup endpoint. */
  @Headers("Accept: application/json", "X-Requested-With: XMLHttpRequest")
  @POST("browser/v1/auth/signup")
  suspend fun signup(@Body request: SignupRequest): SessionLoginResponse

  /**
   * Step 2: Exchange authenticated session for an auth token. Requires session cookie from step 1
   * to be present.
   */
  @Headers("Accept: application/json", "X-Requested-With: XMLHttpRequest")
  @POST("browser/v1/auth/token/")
  suspend fun fetchAuthToken(): AuthTokenResponse

  /** Get the authenticated user's profile (name, email, timezone). */
  @Headers("Accept: application/json")
  @GET("account/profile/")
  suspend fun getProfile(): UserProfile

  /**
   * Partially update the authenticated user's profile.
   *
   * Supports updating first name, last name, email, and timezone.
   */
  @Headers("Accept: application/json")
  @PATCH("account/profile/")
  suspend fun updateProfile(@Body request: UserProfileUpdate): UserProfile

  /** Logout the current session (invalidates session cookie and token). */
  @Headers("Accept: application/json", "X-Requested-With: XMLHttpRequest")
  @DELETE("browser/v1/auth/session")
  suspend fun logoutSession()

  // ========================
  // Child Endpoints
  // ========================

  /** Get paginated list of all children for current user. GET /api/v1/children/ */
  @GET("children/") suspend fun listChildren(@Query("page") page: Int = 1): PaginatedResponse<Child>

  /** Get a specific child by ID. GET /api/v1/children/{childId}/ */
  @GET("children/{childId}/") suspend fun getChild(@Path("childId") childId: Int): Child

  /** Create a new child profile. POST /api/v1/children/ */
  @POST("children/") suspend fun createChild(@Body request: CreateChildRequest): Child

  /** Update a child's profile. PATCH /api/v1/children/{childId}/ */
  @PATCH("children/{childId}/")
  suspend fun updateChild(@Path("childId") childId: Int, @Body request: CreateChildRequest): Child

  /** Delete a child profile. DELETE /api/v1/children/{childId}/ */
  @DELETE("children/{childId}/") suspend fun deleteChild(@Path("childId") childId: Int)

  // ========================
  // Feeding Endpoints
  // ========================

  /** Get paginated list of feedings for a child. GET /api/v1/children/{childId}/feedings/ */
  @GET("children/{childId}/feedings/")
  suspend fun listFeedings(
      @Path("childId") childId: Int,
      @Query("page") page: Int = 1
  ): PaginatedResponse<Feeding>

  /** Get a specific feeding by ID. GET /api/v1/children/{childId}/feedings/{feedingId}/ */
  @GET("children/{childId}/feedings/{feedingId}/")
  suspend fun getFeeding(@Path("childId") childId: Int, @Path("feedingId") feedingId: Int): Feeding

  /** Create a new feeding record. POST /api/v1/children/{childId}/feedings/ */
  @POST("children/{childId}/feedings/")
  suspend fun createFeeding(
      @Path("childId") childId: Int,
      @Body request: CreateFeedingRequest
  ): Feeding

  /** Update a feeding record. PATCH /api/v1/children/{childId}/feedings/{feedingId}/ */
  @PATCH("children/{childId}/feedings/{feedingId}/")
  suspend fun updateFeeding(
      @Path("childId") childId: Int,
      @Path("feedingId") feedingId: Int,
      @Body request: CreateFeedingRequest
  ): Feeding

  /** Delete a feeding record. DELETE /api/v1/children/{childId}/feedings/{feedingId}/ */
  @DELETE("children/{childId}/feedings/{feedingId}/")
  suspend fun deleteFeeding(@Path("childId") childId: Int, @Path("feedingId") feedingId: Int)

  // ========================
  // Diaper Endpoints
  // ========================

  /** Get paginated list of diaper changes for a child. GET /api/v1/children/{childId}/diapers/ */
  @GET("children/{childId}/diapers/")
  suspend fun listDiapers(
      @Path("childId") childId: Int,
      @Query("page") page: Int = 1
  ): PaginatedResponse<Diaper>

  /** Create a new diaper change record. POST /api/v1/children/{childId}/diapers/ */
  @POST("children/{childId}/diapers/")
  suspend fun createDiaper(
      @Path("childId") childId: Int,
      @Body request: CreateDiaperRequest
  ): Diaper

  /** Delete a diaper change record. DELETE /api/v1/children/{childId}/diapers/{diaperId}/ */
  @DELETE("children/{childId}/diapers/{diaperId}/")
  suspend fun deleteDiaper(@Path("childId") childId: Int, @Path("diaperId") diaperId: Int)

  // ========================
  // Nap Endpoints
  // ========================

  /** Get paginated list of naps for a child. GET /api/v1/children/{childId}/naps/ */
  @GET("children/{childId}/naps/")
  suspend fun listNaps(
      @Path("childId") childId: Int,
      @Query("page") page: Int = 1
  ): PaginatedResponse<Nap>

  /** Create a new nap record. POST /api/v1/children/{childId}/naps/ */
  @POST("children/{childId}/naps/")
  suspend fun createNap(@Path("childId") childId: Int, @Body request: CreateNapRequest): Nap

  /** Update a nap record (e.g., set end_time). PATCH /api/v1/children/{childId}/naps/{napId}/ */
  @PATCH("children/{childId}/naps/{napId}/")
  suspend fun updateNap(
      @Path("childId") childId: Int,
      @Path("napId") napId: Int,
      @Body request: UpdateNapRequest
  ): Nap

  /** Delete a nap record. DELETE /api/v1/children/{childId}/naps/{napId}/ */
  @DELETE("children/{childId}/naps/{napId}/")
  suspend fun deleteNap(@Path("childId") childId: Int, @Path("napId") napId: Int)

  // ========================
  // Sharing Endpoints
  // ========================

  /** Get list of children shared with other users. GET /api/v1/children/{childId}/sharing/ */
  @GET("children/{childId}/sharing/")
  suspend fun listShares(@Path("childId") childId: Int): List<ChildShare>

  /** Create a share invite for a child. POST /api/v1/children/{childId}/sharing/ */
  @POST("children/{childId}/sharing/")
  suspend fun createShare(
      @Path("childId") childId: Int,
      @Body request: CreateShareRequest
  ): ShareInvite

  /** Get pending invites for current user. GET /api/v1/invites/pending/ */
  @GET("invites/pending/") suspend fun getPendingInvites(): List<ShareInvite>

  /** Accept a share invite. POST /api/v1/invites/{inviteId}/accept/ */
  @POST("invites/{inviteId}/accept/")
  suspend fun acceptInvite(@Path("inviteId") inviteId: Int): ShareInvite
}

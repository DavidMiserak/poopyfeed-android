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

  /**
   * Change the authenticated user's password.
   *
   * Validates current password, confirms new passwords match, and enforces password strength rules.
   * Returns new auth token that must replace the old one (token rotation).
   */
  @Headers("Accept: application/json")
  @POST("account/password/")
  suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordResponse

  /**
   * Delete the authenticated user's account.
   *
   * Requires password confirmation for security. This is a permanent operation; all user data and
   * associated child profiles will be deleted. Returns HTTP 204 No Content on success.
   */
  @Headers("Accept: application/json")
  @POST("account/delete/")
  suspend fun deleteAccount(@Body request: DeleteAccountRequest)

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

  /** Update a child's profile (partial). PATCH /api/v1/children/{childId}/ */
  @PATCH("children/{childId}/")
  suspend fun updateChild(@Path("childId") childId: Int, @Body request: UpdateChildRequest): Child

  /** Delete a child profile. DELETE /api/v1/children/{childId}/ */
  @DELETE("children/{childId}/") suspend fun deleteChild(@Path("childId") childId: Int)

  /**
   * Batch dashboard summary: today + weekly + unread_count. GET
   * /api/v1/children/{childId}/dashboard-summary/
   */
  @GET("children/{childId}/dashboard-summary/")
  suspend fun getDashboardSummary(@Path("childId") childId: Int): DashboardSummaryResponse

  // ========================
  // Feeding Endpoints
  // ========================

  /** Get paginated list of feedings for a child. GET /api/v1/children/{childId}/feedings/ */
  @GET("children/{childId}/feedings/")
  suspend fun listFeedings(
      @Path("childId") childId: Int,
      @Query("page") page: Int = 1,
      @Query("page_size") pageSize: Int = 20
  ): PaginatedResponse<FeedingListResponse>

  /** Create a new feeding record. POST /api/v1/children/{childId}/feedings/ */
  @POST("children/{childId}/feedings/")
  suspend fun createFeeding(
      @Path("childId") childId: Int,
      @Body request: CreateFeedingRequest
  ): FeedingListResponse

  /** Update a feeding record. PATCH /api/v1/children/{childId}/feedings/{feedingId}/ */
  @PATCH("children/{childId}/feedings/{feedingId}/")
  suspend fun updateFeeding(
      @Path("childId") childId: Int,
      @Path("feedingId") feedingId: Int,
      @Body request: CreateFeedingRequest
  ): FeedingListResponse

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
      @Query("page") page: Int = 1,
      @Query("page_size") pageSize: Int = 20
  ): PaginatedResponse<DiaperListResponse>

  /** Create a new diaper change record. POST /api/v1/children/{childId}/diapers/ */
  @POST("children/{childId}/diapers/")
  suspend fun createDiaper(
      @Path("childId") childId: Int,
      @Body request: CreateDiaperRequest
  ): DiaperListResponse

  /** Update a diaper change record. PATCH /api/v1/children/{childId}/diapers/{diaperId}/ */
  @PATCH("children/{childId}/diapers/{diaperId}/")
  suspend fun updateDiaper(
      @Path("childId") childId: Int,
      @Path("diaperId") diaperId: Int,
      @Body request: CreateDiaperRequest
  ): DiaperListResponse

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
  ): PaginatedResponse<NapListResponse>

  /** Create a new nap record. POST /api/v1/children/{childId}/naps/ */
  @POST("children/{childId}/naps/")
  suspend fun createNap(
      @Path("childId") childId: Int,
      @Body request: CreateNapRequest
  ): NapListResponse

  /** Update a nap record (e.g., set end_time). PATCH /api/v1/children/{childId}/naps/{napId}/ */
  @PATCH("children/{childId}/naps/{napId}/")
  suspend fun updateNap(
      @Path("childId") childId: Int,
      @Path("napId") napId: Int,
      @Body request: UpdateNapRequest
  ): NapListResponse

  /** Delete a nap record. DELETE /api/v1/children/{childId}/naps/{napId}/ */
  @DELETE("children/{childId}/naps/{napId}/")
  suspend fun deleteNap(@Path("childId") childId: Int, @Path("napId") napId: Int)

  // ========================
  // Sharing Endpoints (match backend: children/{id}/shares/, children/{id}/invites/,
  // invites/accept/)
  // ========================

  /** Get list of shares for a child (owner only). GET /api/v1/children/{childId}/shares/ */
  @GET("children/{childId}/shares/")
  suspend fun listShares(@Path("childId") childId: Int): List<ChildShare>

  /** List invite links for a child (owner only). GET /api/v1/children/{childId}/invites/ */
  @GET("children/{childId}/invites/")
  suspend fun listInvites(@Path("childId") childId: Int): List<ChildInvite>

  /**
   * Create an invite for a child (owner only). Body: { "role": "co-parent"|"caregiver" }. POST
   * /api/v1/children/{childId}/invites/
   */
  @POST("children/{childId}/invites/")
  suspend fun createShare(
      @Path("childId") childId: Int,
      @Body request: CreateShareRequest
  ): ShareInviteResponse

  /**
   * Toggle invite active status (owner only). PATCH /api/v1/children/{childId}/invites/{invitePk}/
   */
  @PATCH("children/{childId}/invites/{invitePk}/")
  suspend fun toggleInvite(
      @Path("childId") childId: Int,
      @Path("invitePk") invitePk: Int,
  ): ChildInvite

  /** Delete an invite (owner only). DELETE /api/v1/children/{childId}/invites/{invitePk}/delete/ */
  @DELETE("children/{childId}/invites/{invitePk}/delete/")
  suspend fun deleteInvite(
      @Path("childId") childId: Int,
      @Path("invitePk") invitePk: Int,
  )

  /**
   * Accept an invite by token. Body: { "token": "..." }. POST /api/v1/invites/accept/ Returns the
   * child the user now has access to. There is no GET pending-invites endpoint.
   */
  @POST("invites/accept/") suspend fun acceptInvite(@Body request: AcceptInviteRequest): Child

  // ========================
  // Notifications Endpoints
  // ========================

  /** Get paginated list of notifications. GET /api/v1/notifications/ */
  @GET("notifications/")
  suspend fun listNotifications(@Query("page") page: Int = 1): PaginatedResponse<Notification>

  /** Get all per-child notification preferences. GET /api/v1/notifications/preferences/ */
  @GET("notifications/preferences/")
  suspend fun getNotificationPreferences(): List<NotificationPreference>

  /** Update a notification preference. PATCH /api/v1/notifications/preferences/{id}/ */
  @PATCH("notifications/preferences/{id}/")
  suspend fun updateNotificationPreference(
      @Path("id") id: Int,
      @Body request: UpdateNotificationPreferenceRequest
  ): NotificationPreference

  /** Get unread notification count. GET /api/v1/notifications/unread-count/ */
  @GET("notifications/unread-count/") suspend fun getUnreadCount(): UnreadCountResponse

  /** Mark all notifications as read. POST /api/v1/notifications/mark-all-read/ */
  @POST("notifications/mark-all-read/") suspend fun markAllNotificationsRead(): MarkAllReadResponse

  /** Mark a single notification as read. PATCH /api/v1/notifications/{id}/ */
  @PATCH("notifications/{id}/")
  suspend fun markNotificationRead(
      @Path("id") id: Int,
      @Body request: MarkReadRequest
  ): Notification

  /** Get quiet hours. GET /api/v1/notifications/quiet-hours/ */
  @GET("notifications/quiet-hours/") suspend fun getQuietHours(): QuietHours

  /** Update quiet hours. PATCH /api/v1/notifications/quiet-hours/ */
  @PATCH("notifications/quiet-hours/")
  suspend fun updateQuietHours(@Body request: QuietHoursUpdate): QuietHours

  /** Register FCM device token. POST /api/v1/notifications/devices/ */
  @POST("notifications/devices/")
  suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): DeviceTokenResponse

  /** Unregister FCM device token. Uses HTTP method override for DELETE with body. */
  @HTTP(method = "DELETE", path = "notifications/devices/", hasBody = true)
  suspend fun unregisterDeviceToken(@Body request: DeviceTokenDeleteRequest): DeviceTokenResponse

  // ========================
  // Analytics Endpoints
  // ========================

  /** Get pattern alerts for a child. GET /api/v1/analytics/children/{childId}/pattern-alerts/ */
  @GET("analytics/children/{childId}/pattern-alerts/")
  suspend fun getPatternAlerts(@Path("childId") childId: Int): PatternAlertsResponse
}

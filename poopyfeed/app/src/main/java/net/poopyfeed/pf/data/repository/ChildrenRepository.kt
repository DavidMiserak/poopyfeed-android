package net.poopyfeed.pf.data.repository

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.*
import net.poopyfeed.pf.data.models.toApiError
import net.poopyfeed.pf.di.IoDispatcher

/**
 * Repository for child profile operations.
 *
 * Provides a clean abstraction over the API service with:
 * - Centralized error handling
 * - Flow-based async operations
 * - Type-safe error results
 * - Single source of truth pattern
 */
class ChildrenRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /**
   * Get all children as a Flow. Emits Loading, then Success or Error.
   *
   * Usage:
   * ```
   * childrenRepository.listChildren(page = 1)
   *     .collect { result ->
   *         when (result) {
   *             is ApiResult.Loading -> showLoader()
   *             is ApiResult.Success -> showChildren(result.data.results)
   *             is ApiResult.Error -> showError(result.error.getUserMessage(context))
   *         }
   *     }
   * ```
   */
  fun listChildren(page: Int = 1): Flow<ApiResult<PaginatedResponse<Child>>> =
      flow {
            emit(ApiResult.Loading())
            try {
              val response = apiService.listChildren(page)
              emit(ApiResult.Success(response))
            } catch (e: Exception) {
              emit(ApiResult.Error(e.toApiError()))
            }
          }
          .flowOn(ioDispatcher)

  /** Get a single child by ID. */
  fun getChild(childId: Int): Flow<ApiResult<Child>> =
      flow {
            emit(ApiResult.Loading())
            try {
              val child = apiService.getChild(childId)
              emit(ApiResult.Success(child))
            } catch (e: Exception) {
              emit(ApiResult.Error(e.toApiError()))
            }
          }
          .flowOn(ioDispatcher)

  /** Create a new child profile. */
  suspend fun createChild(request: CreateChildRequest): ApiResult<Child> =
      withContext(ioDispatcher) {
        try {
          val child = apiService.createChild(request)
          ApiResult.Success(child)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Update an existing child profile. */
  suspend fun updateChild(childId: Int, request: CreateChildRequest): ApiResult<Child> =
      withContext(ioDispatcher) {
        try {
          val child = apiService.updateChild(childId, request)
          ApiResult.Success(child)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Delete a child profile. */
  suspend fun deleteChild(childId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteChild(childId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}

/** Repository for feeding operations. */
class FeedingsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  fun listFeedings(childId: Int, page: Int = 1): Flow<ApiResult<PaginatedResponse<Feeding>>> =
      flow {
            emit(ApiResult.Loading())
            try {
              val response = apiService.listFeedings(childId, page)
              emit(ApiResult.Success(response))
            } catch (e: Exception) {
              emit(ApiResult.Error(e.toApiError()))
            }
          }
          .flowOn(ioDispatcher)

  suspend fun createFeeding(childId: Int, request: CreateFeedingRequest): ApiResult<Feeding> =
      withContext(ioDispatcher) {
        try {
          val feeding = apiService.createFeeding(childId, request)
          ApiResult.Success(feeding)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun updateFeeding(
      childId: Int,
      feedingId: Int,
      request: CreateFeedingRequest
  ): ApiResult<Feeding> =
      withContext(ioDispatcher) {
        try {
          val feeding = apiService.updateFeeding(childId, feedingId, request)
          ApiResult.Success(feeding)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun deleteFeeding(childId: Int, feedingId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteFeeding(childId, feedingId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}

/** Repository for diaper change operations. */
class DiapersRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  fun listDiapers(childId: Int, page: Int = 1): Flow<ApiResult<PaginatedResponse<Diaper>>> =
      flow {
            emit(ApiResult.Loading())
            try {
              val response = apiService.listDiapers(childId, page)
              emit(ApiResult.Success(response))
            } catch (e: Exception) {
              emit(ApiResult.Error(e.toApiError()))
            }
          }
          .flowOn(ioDispatcher)

  suspend fun createDiaper(childId: Int, request: CreateDiaperRequest): ApiResult<Diaper> =
      withContext(ioDispatcher) {
        try {
          val diaper = apiService.createDiaper(childId, request)
          ApiResult.Success(diaper)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun deleteDiaper(childId: Int, diaperId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteDiaper(childId, diaperId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}

/** Repository for nap operations. */
class NapsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  fun listNaps(childId: Int, page: Int = 1): Flow<ApiResult<PaginatedResponse<Nap>>> =
      flow {
            emit(ApiResult.Loading())
            try {
              val response = apiService.listNaps(childId, page)
              emit(ApiResult.Success(response))
            } catch (e: Exception) {
              emit(ApiResult.Error(e.toApiError()))
            }
          }
          .flowOn(ioDispatcher)

  suspend fun createNap(childId: Int, request: CreateNapRequest): ApiResult<Nap> =
      withContext(ioDispatcher) {
        try {
          val nap = apiService.createNap(childId, request)
          ApiResult.Success(nap)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun updateNap(childId: Int, napId: Int, request: UpdateNapRequest): ApiResult<Nap> =
      withContext(ioDispatcher) {
        try {
          val nap = apiService.updateNap(childId, napId, request)
          ApiResult.Success(nap)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun deleteNap(childId: Int, napId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteNap(childId, napId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}

/** Repository for sharing operations. */
class SharingRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  suspend fun listShares(childId: Int): ApiResult<List<ChildShare>> =
      withContext(ioDispatcher) {
        try {
          val shares = apiService.listShares(childId)
          ApiResult.Success(shares)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun createShare(childId: Int, request: CreateShareRequest): ApiResult<ShareInvite> =
      withContext(ioDispatcher) {
        try {
          val invite = apiService.createShare(childId, request)
          ApiResult.Success(invite)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun getPendingInvites(): ApiResult<List<ShareInvite>> =
      withContext(ioDispatcher) {
        try {
          val invites = apiService.getPendingInvites()
          ApiResult.Success(invites)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun acceptInvite(inviteId: Int): ApiResult<ShareInvite> =
      withContext(ioDispatcher) {
        try {
          val invite = apiService.acceptInvite(inviteId)
          ApiResult.Success(invite)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}

/** Repository for authentication operations. */
class AuthRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /** Perform two-step browser login and return the auth token string. */
  suspend fun login(email: String, password: String): ApiResult<String> =
      withContext(ioDispatcher) {
        try {
          apiService.sessionLogin(LoginRequest(email, password))
          val tokenResponse = apiService.fetchAuthToken()
          ApiResult.Success(tokenResponse.auth_token)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /**
   * Sign up a new user and return the auth token string.
   *
   * Mirrors the web frontend flow:
   * 1) POST browser/v1/auth/signup
   * 2) POST browser/v1/auth/token/ to obtain auth_token
   */
  suspend fun signup(email: String, password: String): ApiResult<String> =
      withContext(ioDispatcher) {
        try {
          apiService.signup(SignupRequest(email = email, password = password))
          val tokenResponse = apiService.fetchAuthToken()
          ApiResult.Success(tokenResponse.auth_token)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Fetch the authenticated user's profile. */
  suspend fun getProfile(): ApiResult<UserProfile> =
      withContext(ioDispatcher) {
        try {
          val profile = apiService.getProfile()
          ApiResult.Success(profile)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Update the authenticated user's profile. */
  suspend fun updateProfile(update: UserProfileUpdate): ApiResult<UserProfile> =
      withContext(ioDispatcher) {
        try {
          val profile = apiService.updateProfile(update)
          ApiResult.Success(profile)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Logout current browser session. */
  suspend fun logout(): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.logoutSession()
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /**
   * Change the authenticated user's password.
   *
   * Validates current password and enforces new password strength. Returns new auth token that must
   * be stored (token rotation). User remains authenticated with new token.
   */
  suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<String> =
      withContext(ioDispatcher) {
        try {
          val request =
              ChangePasswordRequest(
                  current_password = currentPassword,
                  new_password = newPassword,
                  new_password_confirm = newPassword)
          val response = apiService.changePassword(request)
          ApiResult.Success(response.auth_token)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /**
   * Delete the authenticated user's account permanently.
   *
   * This is an irreversible operation that deletes the user and all associated data (children,
   * feedings, diapers, naps, shares, etc.). Requires password confirmation.
   *
   * On success, caller should clear token, clear cache, and navigate to login.
   */
  suspend fun deleteAccount(password: String): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          val request = DeleteAccountRequest(current_password = password)
          apiService.deleteAccount(request)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}

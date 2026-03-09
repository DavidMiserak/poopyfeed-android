package net.poopyfeed.pf.notifications

import android.content.Context
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PoopyFeedMessagingServiceTest {

  private lateinit var service: PoopyFeedMessagingService
  private lateinit var context: Context
  private val mockNotificationsRepository: NotificationsRepository = mockk(relaxed = true)
  private val mockTokenManager: TokenManager = mockk(relaxed = true)

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }
}

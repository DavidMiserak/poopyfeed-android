package net.poopyfeed.pf.ui.toast

import android.content.Context
import android.widget.Toast
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ToastManagerImplTest {

    private lateinit var context: Context
    private lateinit var toastManager: ToastManagerImpl
    private lateinit var mockToast: Toast

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        mockToast = mockk(relaxed = true)

        // Mock the static Toast.makeText() method
        mockkStatic(Toast::class)
        every { Toast.makeText(any(), any<String>(), any()) } returns mockToast

        toastManager = ToastManagerImpl(context)
    }

    @Test
    fun testShowSuccessCreatesToastWithShortDuration() {
        toastManager.showSuccess("✓ Test message")

        verify {
            Toast.makeText(context, "✓ Test message", Toast.LENGTH_SHORT)
            mockToast.show()
        }
    }

    @Test
    fun testShowErrorCreatesToastWithLongDuration() {
        toastManager.showError("✗ Error message")

        verify {
            Toast.makeText(context, "✗ Error message", Toast.LENGTH_LONG)
            mockToast.show()
        }
    }

    @Test
    fun testShowInfoCreatesToastWithShortDuration() {
        toastManager.showInfo("📱 Info message")

        verify {
            Toast.makeText(context, "📱 Info message", Toast.LENGTH_SHORT)
            mockToast.show()
        }
    }
}

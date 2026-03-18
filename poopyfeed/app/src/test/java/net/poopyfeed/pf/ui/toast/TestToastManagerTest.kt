package net.poopyfeed.pf.ui.toast

import org.junit.Before
import org.junit.Test

class TestToastManagerTest {

  private lateinit var testManager: TestToastManager

  @Before
  fun setup() {
    testManager = TestToastManager()
  }

  @Test
  fun testRecordsSuccessMessages() {
    testManager.showSuccess("✓ Feeding logged")

    assert(testManager.messages.size == 1)
    assert(testManager.messages[0].first == "✓ Feeding logged")
    assert(testManager.messages[0].second == TestToastManager.MessageType.SUCCESS)
  }

  @Test
  fun testRecordsErrorMessages() {
    testManager.showError("✗ Failed to save")

    assert(testManager.messages.size == 1)
    assert(testManager.messages[0].first == "✗ Failed to save")
    assert(testManager.messages[0].second == TestToastManager.MessageType.ERROR)
  }

  @Test
  fun testRecordsInfoMessages() {
    testManager.showInfo("🔄 Syncing...")

    assert(testManager.messages.size == 1)
    assert(testManager.messages[0].first == "🔄 Syncing...")
    assert(testManager.messages[0].second == TestToastManager.MessageType.INFO)
  }

  @Test
  fun testAssertSuccessFindsMessage() {
    testManager.showSuccess("✓ Test message")

    testManager.assertSuccess("✓ Test message") // Should not throw
  }

  @Test(expected = IllegalStateException::class)
  fun testAssertSuccessThrowsWhenNotFound() {
    testManager.showSuccess("✓ Message A")

    testManager.assertSuccess("✓ Different message") // Should throw
  }

  @Test
  fun testClearRemovesAllMessages() {
    testManager.showSuccess("✓ Message")
    testManager.showError("✗ Error")

    testManager.clear()

    assert(testManager.messages.isEmpty())
  }
}

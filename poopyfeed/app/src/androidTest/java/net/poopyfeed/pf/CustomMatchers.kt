package net.poopyfeed.pf

import android.view.View
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/** Returns a matcher that checks if a [TextInputLayout] has an error set. */
fun hasTextInputLayoutError(): Matcher<View> =
    object : TypeSafeMatcher<View>() {
      override fun describeTo(description: Description) {
        description.appendText("has TextInputLayout error")
      }

      override fun matchesSafely(item: View): Boolean {
        if (item !is TextInputLayout) return false
        return item.error != null
      }
    }

/** Returns a matcher that checks if a [TextInputLayout] has a specific error message. */
fun hasTextInputLayoutErrorText(expected: String): Matcher<View> =
    object : TypeSafeMatcher<View>() {
      override fun describeTo(description: Description) {
        description.appendText("has TextInputLayout error text: $expected")
      }

      override fun matchesSafely(item: View): Boolean {
        if (item !is TextInputLayout) return false
        return item.error?.toString() == expected
      }
    }

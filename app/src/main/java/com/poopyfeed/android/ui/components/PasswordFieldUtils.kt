package com.poopyfeed.android.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.poopyfeed.android.ui.theme.Slate600

/**
 * Reusable composable for password visibility toggle icon button.
 * Used across SignupScreen, LoginScreen, and ProfileScreen.
 */
@Composable
fun PasswordVisibilityIcon(
    isVisible: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter =
                painterResource(
                    id = getPasswordIconResource(isVisible),
                ),
            contentDescription = getPasswordContentDescription(isVisible),
            tint = Slate600,
        )
    }
}

/**
 * Returns the appropriate VisualTransformation based on password visibility state.
 */
fun getPasswordVisualTransformation(isVisible: Boolean): VisualTransformation {
    return if (isVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }
}

/**
 * Returns the icon resource ID (show/hide) based on password visibility state.
 */
internal fun getPasswordIconResource(isVisible: Boolean): Int {
    return if (isVisible) {
        android.R.drawable.ic_menu_view
    } else {
        android.R.drawable.ic_secure
    }
}

/**
 * Returns the accessibility description for password visibility toggle.
 */
internal fun getPasswordContentDescription(isVisible: Boolean): String {
    return if (isVisible) {
        "Hide password"
    } else {
        "Show password"
    }
}

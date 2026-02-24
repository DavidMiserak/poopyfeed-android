package com.poopyfeed.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose200
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.Slate600

/**
 * Configuration for AuthTextField optional parameters.
 * Groups keyboard, visual transformation, and UI customization options.
 */
data class AuthTextFieldConfig(
    val modifier: Modifier = Modifier,
    val error: String? = null,
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val keyboardActions: KeyboardActions = KeyboardActions.Default,
    val visualTransformation: VisualTransformation = VisualTransformation.None,
    val trailingIcon: @Composable (() -> Unit)? = null,
)

/**
 * Public API function with 4 parameters (within SonarQube 7-parameter limit).
 * Config object groups all optional parameters for cleaner API.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    config: AuthTextFieldConfig = AuthTextFieldConfig(),
) {
    AuthTextFieldContent(
        value = value,
        onValueChange = onValueChange,
        label = label,
        config = config,
    )
}

@Composable
private fun AuthTextFieldContent(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    config: AuthTextFieldConfig,
) {
    Column(modifier = config.modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = config.error != null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Rose400,
                    unfocusedBorderColor = Rose200,
                    focusedLabelColor = Rose400,
                    unfocusedLabelColor = Slate600,
                    cursorColor = Rose400,
                ),
            keyboardOptions = config.keyboardOptions,
            keyboardActions = config.keyboardActions,
            visualTransformation = config.visualTransformation,
            trailingIcon = config.trailingIcon,
        )
        if (config.error != null) {
            Text(
                text = config.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthTextFieldPreview() {
    PoopyFeedTheme {
        AuthTextField(
            value = "",
            onValueChange = {},
            label = "Email",
        )
    }
}

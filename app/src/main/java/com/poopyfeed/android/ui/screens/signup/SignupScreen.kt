package com.poopyfeed.android.ui.screens.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.AuthTextField
import com.poopyfeed.android.ui.components.BrandLogo
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.components.PasswordVisibilityIcon
import com.poopyfeed.android.ui.components.getPasswordContentDescription
import com.poopyfeed.android.ui.components.getPasswordIconResource
import com.poopyfeed.android.ui.components.getPasswordVisualTransformation
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose200
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.Rose50
import com.poopyfeed.android.ui.theme.Slate600
import com.poopyfeed.android.ui.theme.White

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSignupSuccess()
        }
    }

    SignupContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSignup = viewModel::signup,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
private fun SignupContent(
    uiState: SignupUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignup: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Rose50, Amber50),
                ),
            )
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogo()

            Spacer(modifier = Modifier.height(32.dp))

            SignupFormCard(
                uiState = uiState,
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                confirmPasswordVisible = confirmPasswordVisible,
                onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                onNameChange = onNameChange,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onSignup = onSignup,
                focusManager = focusManager,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SignupLoginLink(onNavigateToLogin = onNavigateToLogin)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SignupFormCard(
    uiState: SignupUiState,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignup: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(24.dp))
            .border(2.dp, Rose200, RoundedCornerShape(24.dp))
            .padding(24.dp),
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Start tracking your baby's care",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate600,
        )

        Spacer(modifier = Modifier.height(24.dp))

        SignupErrorBanner(apiError = uiState.apiError)

        AuthTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = "Name",
            error = uiState.nameError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "Email",
            error = uiState.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Password",
            error = uiState.passwordError,
            isVisible = passwordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            focusManager = focusManager,
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm Password",
            error = uiState.confirmPasswordError,
            isVisible = confirmPasswordVisible,
            onVisibilityToggle = onConfirmPasswordVisibilityToggle,
            isLastField = true,
            onDone = onSignup,
            focusManager = focusManager,
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Create Account",
            onClick = onSignup,
            isLoading = uiState.isLoading,
        )
    }
}

@Composable
private fun SignupErrorBanner(apiError: String?) {
    if (apiError != null) {
        ErrorBanner(message = apiError)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isLastField: Boolean = false,
    onDone: (() -> Unit)? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (isLastField) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = {
                focusManager.clearFocus()
                onDone?.invoke()
            },
        ),
        visualTransformation = getPasswordVisualTransformation(isVisible),
        trailingIcon = {
            PasswordVisibilityIcon(
                isVisible = isVisible,
                onClick = onVisibilityToggle,
            )
        },
    )
}

@Composable
private fun SignupLoginLink(onNavigateToLogin: () -> Unit) {
    Row {
        Text(
            text = "Already have an account? ",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate600,
        )
        Text(
            text = "Log In",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Rose400,
            modifier = Modifier.clickable { onNavigateToLogin() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignupScreenPreview() {
    PoopyFeedTheme {
        SignupContent(
            uiState = SignupUiState(),
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSignup = {},
            onNavigateToLogin = {},
        )
    }
}

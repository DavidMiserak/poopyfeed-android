package com.poopyfeed.android.ui.screens.login

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
import com.poopyfeed.android.ui.components.getPasswordVisualTransformation
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose200
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.Rose50
import com.poopyfeed.android.ui.theme.Slate600
import com.poopyfeed.android.ui.theme.White

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = viewModel::login,
        onNavigateToSignup = onNavigateToSignup,
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

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

            LoginFormCard(
                uiState = uiState,
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onLogin = onLogin,
                focusManager = focusManager,
            )

            Spacer(modifier = Modifier.height(24.dp))

            LoginSignupLink(onNavigateToSignup = onNavigateToSignup)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoginFormCard(
    uiState: LoginUiState,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
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
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Log in to your account",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate600,
        )

        Spacer(modifier = Modifier.height(24.dp))

        LoginErrorBanner(error = uiState.apiError)

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

        LoginPasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Password",
            error = uiState.passwordError,
            isVisible = passwordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            onDone = onLogin,
            focusManager = focusManager,
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Log In",
            onClick = onLogin,
            isLoading = uiState.isLoading,
        )
    }
}

@Composable
private fun LoginErrorBanner(error: String?) {
    if (error != null) {
        ErrorBanner(message = error)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    onDone: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onDone()
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
private fun LoginSignupLink(onNavigateToSignup: () -> Unit) {
    Row {
        Text(
            text = "Don't have an account? ",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate600,
        )
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Rose400,
            modifier = Modifier.clickable { onNavigateToSignup() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    PoopyFeedTheme {
        LoginContent(
            uiState = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSignup = {},
        )
    }
}

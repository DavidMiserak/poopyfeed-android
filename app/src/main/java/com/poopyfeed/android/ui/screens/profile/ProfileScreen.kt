package com.poopyfeed.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.AuthTextField
import com.poopyfeed.android.ui.components.AuthTextFieldConfig
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

/**
 * State and callbacks for a password field with visibility toggle.
 */
private data class PasswordFieldState(
    val isVisible: Boolean,
    val onVisibilityToggle: () -> Unit,
)

/**
 * Configuration for password field display and behavior.
 */
private data class PasswordFieldConfig(
    val label: String,
    val error: String?,
    val isLastField: Boolean = false,
    val onDone: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteSuccess: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            onDeleteSuccess()
        }
    }

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onLogout()
        }
    }

    ProfileContent(
        uiState = uiState,
        callbacks =
            ProfileContentCallbacks(
                navigation =
                    NavigationCallbacks(
                        onNavigateBack = onNavigateBack,
                        onLogoutNavigate = onLogout,
                    ),
                onTabSelected = viewModel::onTabSelected,
                profileTab =
                    ProfileTabCallbacks(
                        onFirstNameChange = viewModel::onFirstNameChange,
                        onLastNameChange = viewModel::onLastNameChange,
                        onTimezoneChange = viewModel::onTimezoneChange,
                        onSaveProfile = viewModel::saveProfile,
                    ),
                securityTab =
                    SecurityTabCallbacks(
                        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                        onNewPasswordChange = viewModel::onNewPasswordChange,
                        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                        onChangePassword = viewModel::changePassword,
                    ),
                accountTab =
                    AccountTabCallbacks(
                        onDeletePasswordChange = viewModel::onDeletePasswordChange,
                        onDeleteAccount = viewModel::deleteAccount,
                        onLogout = viewModel::logout,
                    ),
            ),
    )
}

private data class NavigationCallbacks(
    val onNavigateBack: () -> Unit,
    val onLogoutNavigate: () -> Unit,
)

private data class ProfileTabCallbacks(
    val onFirstNameChange: (String) -> Unit,
    val onLastNameChange: (String) -> Unit,
    val onTimezoneChange: (String) -> Unit,
    val onSaveProfile: () -> Unit,
)

private data class SecurityTabCallbacks(
    val onCurrentPasswordChange: (String) -> Unit,
    val onNewPasswordChange: (String) -> Unit,
    val onConfirmPasswordChange: (String) -> Unit,
    val onChangePassword: () -> Unit,
)

private data class AccountTabCallbacks(
    val onDeletePasswordChange: (String) -> Unit,
    val onDeleteAccount: () -> Unit,
    val onLogout: () -> Unit,
)

private data class ProfileContentCallbacks(
    val navigation: NavigationCallbacks,
    val onTabSelected: (Int) -> Unit,
    val profileTab: ProfileTabCallbacks,
    val securityTab: SecurityTabCallbacks,
    val accountTab: AccountTabCallbacks,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    callbacks: ProfileContentCallbacks,
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = callbacks.navigation.onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Rose50, Amber50),
                            ),
                        )
                        .imePadding(),
            ) {
                TabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Tab(
                        selected = uiState.selectedTabIndex == 0,
                        onClick = { callbacks.onTabSelected(0) },
                        text = { Text("Profile") },
                    )
                    Tab(
                        selected = uiState.selectedTabIndex == 1,
                        onClick = { callbacks.onTabSelected(1) },
                        text = { Text("Security") },
                    )
                    Tab(
                        selected = uiState.selectedTabIndex == 2,
                        onClick = { callbacks.onTabSelected(2) },
                        text = { Text("Account") },
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                ) {
                    when (uiState.selectedTabIndex) {
                        0 ->
                            ProfileTab(
                                uiState = uiState,
                                onFirstNameChange = callbacks.profileTab.onFirstNameChange,
                                onLastNameChange = callbacks.profileTab.onLastNameChange,
                                onTimezoneChange = callbacks.profileTab.onTimezoneChange,
                                onSaveProfile = callbacks.profileTab.onSaveProfile,
                                focusManager = focusManager,
                            )
                        1 ->
                            SecurityTab(
                                uiState = uiState,
                                onCurrentPasswordChange = callbacks.securityTab.onCurrentPasswordChange,
                                onNewPasswordChange = callbacks.securityTab.onNewPasswordChange,
                                onConfirmPasswordChange = callbacks.securityTab.onConfirmPasswordChange,
                                onChangePassword = callbacks.securityTab.onChangePassword,
                                focusManager = focusManager,
                            )
                        2 ->
                            AccountTab(
                                uiState = uiState,
                                onDeletePasswordChange = callbacks.accountTab.onDeletePasswordChange,
                                onDeleteAccount = callbacks.accountTab.onDeleteAccount,
                                onLogout = callbacks.accountTab.onLogout,
                                isLoggingOut = uiState.isLoggingOut,
                            )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTab(
    uiState: ProfileUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        if (uiState.isLoadingProfile) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (uiState.profileApiError != null) {
                ErrorBanner(message = uiState.profileApiError)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.profileSuccessMessage != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .padding(12.dp),
                ) {
                    Text(
                        text = uiState.profileSuccessMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Email display (read-only)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600,
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = Rose200,
                            disabledLabelColor = Slate600,
                        ),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            AuthTextField(
                value = uiState.firstName,
                onValueChange = onFirstNameChange,
                label = "First Name",
                config =
                    AuthTextFieldConfig(
                        error = uiState.firstNameError,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = "Last Name",
                config =
                    AuthTextFieldConfig(
                        error = uiState.lastNameError,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = uiState.timezone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Timezone") },
                    modifier =
                        Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Rose400,
                            unfocusedBorderColor = Rose200,
                            focusedLabelColor = Rose400,
                            unfocusedLabelColor = Slate600,
                        ),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    ProfileViewModel.TIMEZONES.forEach { timezone ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(timezone) },
                            onClick = {
                                onTimezoneChange(timezone)
                                expanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GradientButton(
                text = "Save Profile",
                onClick = onSaveProfile,
                isLoading = uiState.isSavingProfile,
            )
        }
    }
}

@Composable
private fun SecurityTab(
    uiState: ProfileUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onChangePassword: () -> Unit,
    focusManager: FocusManager,
) {
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val currentPasswordFieldState =
        PasswordFieldState(
            isVisible = currentPasswordVisible,
            onVisibilityToggle = { currentPasswordVisible = !currentPasswordVisible },
        )

    val newPasswordFieldState =
        PasswordFieldState(
            isVisible = newPasswordVisible,
            onVisibilityToggle = { newPasswordVisible = !newPasswordVisible },
        )

    val confirmPasswordFieldState =
        PasswordFieldState(
            isVisible = confirmPasswordVisible,
            onVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        PasswordChangeErrorBanner(error = uiState.passwordApiError)
        PasswordChangeSuccessBanner(message = uiState.passwordSuccessMessage)

        PasswordChangeField(
            value = uiState.currentPassword,
            onValueChange = onCurrentPasswordChange,
            passwordFieldState = currentPasswordFieldState,
            focusManager = focusManager,
            config =
                PasswordFieldConfig(
                    label = "Current Password",
                    error = uiState.currentPasswordError,
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordChangeField(
            value = uiState.newPassword,
            onValueChange = onNewPasswordChange,
            passwordFieldState = newPasswordFieldState,
            focusManager = focusManager,
            config =
                PasswordFieldConfig(
                    label = "New Password",
                    error = uiState.newPasswordError,
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordChangeField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            passwordFieldState = confirmPasswordFieldState,
            focusManager = focusManager,
            config =
                PasswordFieldConfig(
                    label = "Confirm New Password",
                    error = uiState.confirmPasswordError,
                    isLastField = true,
                    onDone = onChangePassword,
                ),
        )

        Spacer(modifier = Modifier.height(24.dp))

        GradientButton(
            text = "Change Password",
            onClick = onChangePassword,
            isLoading = uiState.isSavingPassword,
        )
    }
}

@Composable
private fun AccountTab(
    uiState: ProfileUiState,
    onDeletePasswordChange: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean = false,
) {
    var deletePasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        LogoutSection(onLogout = onLogout, isLoggingOut = isLoggingOut)

        Spacer(modifier = Modifier.height(32.dp))

        DeleteAccountWarningBox()

        Spacer(modifier = Modifier.height(24.dp))

        AccountDeleteErrorBanner(error = uiState.deleteApiError)

        AccountDeletePasswordField(
            value = uiState.deletePassword,
            onValueChange = onDeletePasswordChange,
            error = uiState.deletePasswordError,
            isVisible = deletePasswordVisible,
            onVisibilityToggle = { deletePasswordVisible = !deletePasswordVisible },
            onDone = onDeleteAccount,
        )

        Spacer(modifier = Modifier.height(24.dp))

        DeleteAccountButton(
            onClick = onDeleteAccount,
            isDeletingAccount = uiState.isDeletingAccount,
        )
    }
}

@Composable
private fun LogoutSection(
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
) {
    OutlinedButton(
        onClick = onLogout,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        enabled = !isLoggingOut,
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp),
                color = MaterialTheme.colorScheme.outline,
                strokeWidth = 2.dp,
            )
        }
        Text("Log Out")
    }
}

@Composable
private fun DeleteAccountWarningBox() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(16.dp),
    ) {
        Text(
            text = "Deleting your account is permanent and cannot be undone. All your data will be deleted.",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AccountDeleteErrorBanner(error: String?) {
    if (error != null) {
        ErrorBanner(message = error)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountDeletePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    onDone: () -> Unit,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Password",
        config =
            AuthTextFieldConfig(
                error = error,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { onDone() },
                    ),
                visualTransformation = getPasswordVisualTransformation(isVisible),
                trailingIcon = {
                    PasswordVisibilityIcon(
                        isVisible = isVisible,
                        onClick = onVisibilityToggle,
                    )
                },
            ),
    )
}

@Composable
private fun DeleteAccountButton(
    onClick: () -> Unit,
    isDeletingAccount: Boolean,
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        enabled = !isDeletingAccount,
    ) {
        if (isDeletingAccount) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp),
                color = MaterialTheme.colorScheme.error,
                strokeWidth = 2.dp,
            )
        }
        Text("Delete Account")
    }
}

@Composable
private fun PasswordChangeErrorBanner(error: String?) {
    if (error != null) {
        ErrorBanner(message = error)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PasswordChangeSuccessBanner(message: String?) {
    if (message != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(12.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PasswordChangeField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordFieldState: PasswordFieldState,
    focusManager: FocusManager,
    config: PasswordFieldConfig,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = config.label,
        config =
            AuthTextFieldConfig(
                error = config.error,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (config.isLastField) ImeAction.Done else ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = {
                            focusManager.clearFocus()
                            config.onDone?.invoke()
                        },
                    ),
                visualTransformation = getPasswordVisualTransformation(passwordFieldState.isVisible),
                trailingIcon = {
                    PasswordVisibilityIcon(
                        isVisible = passwordFieldState.isVisible,
                        onClick = passwordFieldState.onVisibilityToggle,
                    )
                },
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    PoopyFeedTheme {
        // Preview without ViewModel
    }
}

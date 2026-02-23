package com.poopyfeed.android.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.poopyfeed.android.ui.components.AuthTextField
import com.poopyfeed.android.ui.components.ErrorBanner
import com.poopyfeed.android.ui.components.GradientButton
import com.poopyfeed.android.ui.theme.Amber50
import com.poopyfeed.android.ui.theme.PoopyFeedTheme
import com.poopyfeed.android.ui.theme.Rose200
import com.poopyfeed.android.ui.theme.Rose400
import com.poopyfeed.android.ui.theme.Rose50
import com.poopyfeed.android.ui.theme.Slate600
import com.poopyfeed.android.ui.theme.White

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
        onNavigateBack = onNavigateBack,
        onLogoutNavigate = onLogout,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onTimezoneChange = viewModel::onTimezoneChange,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onDeletePasswordChange = viewModel::onDeletePasswordChange,
        onTabSelected = viewModel::onTabSelected,
        onSaveProfile = viewModel::saveProfile,
        onChangePassword = viewModel::changePassword,
        onDeleteAccount = viewModel::deleteAccount,
        onLogout = viewModel::logout,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onNavigateBack: () -> Unit,
    onLogoutNavigate: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDeletePasswordChange: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onSaveProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Tab(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text("Profile") },
                )
                Tab(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text("Security") },
                )
                Tab(
                    selected = uiState.selectedTabIndex == 2,
                    onClick = { onTabSelected(2) },
                    text = { Text("Account") },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                when (uiState.selectedTabIndex) {
                    0 -> ProfileTab(
                        uiState = uiState,
                        onFirstNameChange = onFirstNameChange,
                        onLastNameChange = onLastNameChange,
                        onTimezoneChange = onTimezoneChange,
                        onSaveProfile = onSaveProfile,
                        focusManager = focusManager,
                    )
                    1 -> SecurityTab(
                        uiState = uiState,
                        onCurrentPasswordChange = onCurrentPasswordChange,
                        onNewPasswordChange = onNewPasswordChange,
                        onConfirmPasswordChange = onConfirmPasswordChange,
                        onChangePassword = onChangePassword,
                        focusManager = focusManager,
                    )
                    2 -> AccountTab(
                        uiState = uiState,
                        onDeletePasswordChange = onDeletePasswordChange,
                        onDeleteAccount = onDeleteAccount,
                        onLogout = onLogout,
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
        modifier = Modifier
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
                    modifier = Modifier
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
                    colors = OutlinedTextFieldDefaults.colors(
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
                error = uiState.firstNameError,
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
                value = uiState.lastName,
                onValueChange = onLastNameChange,
                label = "Last Name",
                error = uiState.lastNameError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
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
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
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
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        if (uiState.passwordApiError != null) {
            ErrorBanner(message = uiState.passwordApiError)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.passwordSuccessMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = uiState.passwordSuccessMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        AuthTextField(
            value = uiState.currentPassword,
            onValueChange = onCurrentPasswordChange,
            label = "Current Password",
            error = uiState.currentPasswordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            visualTransformation = if (currentPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (currentPasswordVisible) {
                                android.R.drawable.ic_menu_view
                            } else {
                                android.R.drawable.ic_secure
                            },
                        ),
                        contentDescription = if (currentPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = Slate600,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = uiState.newPassword,
            onValueChange = onNewPasswordChange,
            label = "New Password",
            error = uiState.newPasswordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
            visualTransformation = if (newPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (newPasswordVisible) {
                                android.R.drawable.ic_menu_view
                            } else {
                                android.R.drawable.ic_secure
                            },
                        ),
                        contentDescription = if (newPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = Slate600,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm New Password",
            error = uiState.confirmPasswordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onChangePassword()
                },
            ),
            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (confirmPasswordVisible) {
                                android.R.drawable.ic_menu_view
                            } else {
                                android.R.drawable.ic_secure
                            },
                        ),
                        contentDescription = if (confirmPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = Slate600,
                    )
                }
            },
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        // Logout section
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isLoggingOut,
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.outline,
                    strokeWidth = 2.dp,
                )
            }
            Text("Log Out")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Delete account section
        Box(
            modifier = Modifier
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

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.deleteApiError != null) {
            ErrorBanner(message = uiState.deleteApiError)
            Spacer(modifier = Modifier.height(16.dp))
        }

        AuthTextField(
            value = uiState.deletePassword,
            onValueChange = onDeletePasswordChange,
            label = "Password",
            error = uiState.deletePasswordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDeleteAccount() },
            ),
            visualTransformation = if (deletePasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { deletePasswordVisible = !deletePasswordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (deletePasswordVisible) {
                                android.R.drawable.ic_menu_view
                            } else {
                                android.R.drawable.ic_secure
                            },
                        ),
                        contentDescription = if (deletePasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = Slate600,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onDeleteAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isDeletingAccount,
        ) {
            if (uiState.isDeletingAccount) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = 2.dp,
                )
            }
            Text("Delete Account")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    PoopyFeedTheme {
        // Preview without ViewModel
    }
}

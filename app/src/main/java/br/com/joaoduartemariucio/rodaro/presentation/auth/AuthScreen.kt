package br.com.joaoduartemariucio.rodaro.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.presentation.common.FailureMessage
import br.com.joaoduartemariucio.rodaro.presentation.common.Localizer
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionCard
import br.com.joaoduartemariucio.rodaro.presentation.common.dismissesKeyboard
import br.com.joaoduartemariucio.rodaro.presentation.common.rememberLocalizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
) {
    val dependencies = LocalDependencies.current
    val viewModel = viewModel { AuthViewModel(dependencies.accountService, dependencies.analytics) }
    val coroutines = rememberCoroutineScope()
    val localizer = rememberLocalizer()
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    val emailIssue = viewModel.inputs.firstOrNull { it is AuthViewModel.Input.EmailRequired || it is AuthViewModel.Input.EmailInvalid }
    val passwordIssue =
        viewModel.inputs.firstOrNull { it is AuthViewModel.Input.PasswordRequired || it is AuthViewModel.Input.PasswordTooShort }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.mode ==
                                AuthViewModel.Mode.SignIn
                            ) {
                                R.string.auth_sign_in
                            } else {
                                R.string.auth_create_account
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .dismissesKeyboard(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SectionCard(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = {
                        viewModel.email = it
                        emailTouched = true
                    },
                    label = { Text(stringResource(R.string.auth_email)) },
                    singleLine = true,
                    isError = emailTouched && emailIssue != null,
                    supportingText = {
                        if (emailTouched && emailIssue != null) Text(emailIssue.message(localizer))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = {
                        viewModel.password = it
                        passwordTouched = true
                    },
                    label = { Text(stringResource(R.string.auth_password)) },
                    singleLine = true,
                    isError = passwordTouched && passwordIssue != null,
                    supportingText = {
                        if (passwordTouched && passwordIssue != null) Text(passwordIssue.message(localizer))
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            viewModel.error?.let { error -> FailureMessage(error) }

            Button(
                onClick = { coroutines.launch { if (viewModel.submit()) onSignedIn() } },
                enabled = viewModel.canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (viewModel.working) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        stringResource(
                            if (viewModel.mode ==
                                AuthViewModel.Mode.SignIn
                            ) {
                                R.string.auth_sign_in
                            } else {
                                R.string.auth_create_account
                            },
                        ),
                    )
                }
            }

            OutlinedButton(
                onClick = { coroutines.launch { if (viewModel.submitWithGoogle()) onSignedIn() } },
                enabled = !viewModel.working,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.auth_sign_in_google))
            }

            TextButton(onClick = viewModel::switchMode) {
                Text(
                    stringResource(
                        if (viewModel.mode == AuthViewModel.Mode.SignIn) {
                            R.string.auth_switch_to_create
                        } else {
                            R.string.auth_switch_to_sign_in
                        },
                    ),
                )
            }
        }
    }
}

private fun AuthViewModel.Input.message(localizer: Localizer): String =
    when (this) {
        AuthViewModel.Input.EmailRequired -> localizer.string(R.string.auth_email_required)
        AuthViewModel.Input.EmailInvalid -> localizer.string(R.string.auth_email_invalid)
        AuthViewModel.Input.PasswordRequired -> localizer.string(R.string.auth_password_required)
        AuthViewModel.Input.PasswordTooShort -> localizer.string(R.string.auth_password_too_short)
    }

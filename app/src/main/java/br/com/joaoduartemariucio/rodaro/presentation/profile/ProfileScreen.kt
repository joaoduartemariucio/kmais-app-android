package br.com.joaoduartemariucio.rodaro.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.domain.common.services.Account
import br.com.joaoduartemariucio.rodaro.presentation.common.FailureMessage
import br.com.joaoduartemariucio.rodaro.presentation.common.Palette
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionCard
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionDivider
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionHeader
import kotlinx.coroutines.launch

private const val PRIVACY_URL = "https://rodaro.web.app/privacidade"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onSignIn: () -> Unit) {
    val context = LocalContext.current
    val dependencies = LocalDependencies.current
    val viewModel = viewModel { ProfileViewModel(dependencies.accountService, dependencies.clearLocalData) }
    val coroutines = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val version =
        remember(context) {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var confirmingDelete by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) { viewModel.refresh() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(colorResource(R.color.ic_launcher_background)),
                    )
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.profile_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.format_version, version),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AccountSection(viewModel, onSignIn)

            if (viewModel.deleteState == ProfileViewModel.DeleteState.Working) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            PrivacySection(viewModel) { confirmingDelete = true }

            viewModel.error?.let { error ->
                SectionFooterMessage(error)
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        coroutines.launch { viewModel.delete() }
                    },
                ) {
                    Text(stringResource(R.string.delete_confirm_action), color = Palette.red)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    when (viewModel.deleteState) {
        ProfileViewModel.DeleteState.NeedsPassword -> {
            AlertDialog(
                onDismissRequest = {
                    password = ""
                    viewModel.cancelDelete()
                },
                title = { Text(stringResource(R.string.delete_needs_password_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.delete_needs_password_message))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.auth_password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = password.isNotEmpty(),
                        onClick = {
                            val submitted = password
                            password = ""
                            coroutines.launch { viewModel.confirmWithPassword(submitted) }
                        },
                    ) {
                        Text(stringResource(R.string.delete_confirm_action))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            password = ""
                            viewModel.cancelDelete()
                        },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        ProfileViewModel.DeleteState.NeedsGoogle -> {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text(stringResource(R.string.delete_confirm_google_title)) },
                text = { Text(stringResource(R.string.delete_needs_password_message)) },
                confirmButton = {
                    TextButton(onClick = { coroutines.launch { viewModel.confirmWithGoogle() } }) {
                        Text(stringResource(R.string.delete_confirm_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }

        ProfileViewModel.DeleteState.Done -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteResult() },
                title = { Text(stringResource(R.string.delete_done_title)) },
                text = { Text(stringResource(R.string.delete_done_message)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissDeleteResult() }) { Text(stringResource(R.string.ok)) }
                },
            )
        }

        else -> {
            Unit
        }
    }
}

@Composable
private fun AccountSection(
    viewModel: ProfileViewModel,
    onSignIn: () -> Unit,
) {
    val coroutines = rememberCoroutineScope()
    SectionHeader(stringResource(R.string.account))
    SectionCard {
        val account = viewModel.account
        if (account == null) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.account_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_sign_in))
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    account.displayName?.takeIf { it.isNotBlank() } ?: account.email.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        if (account.provider == Account.Provider.GOOGLE) {
                            R.string.account_provider_google
                        } else {
                            R.string.account_provider_password
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { coroutines.launch { viewModel.signOut() } }) {
                    Text(stringResource(R.string.sign_out))
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(
    viewModel: ProfileViewModel,
    onDelete: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val isWorking = viewModel.deleteState == ProfileViewModel.DeleteState.Working

    SectionHeader(stringResource(R.string.privacy))
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(PRIVACY_URL) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.privacy_policy),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionDivider()
        Button(
            onClick = onDelete,
            enabled = viewModel.account != null && !isWorking,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.red, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.delete_account_data))
        }
    }
}

@Composable
private fun SectionFooterMessage(error: Throwable) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { FailureMessage(error) }
}

package br.com.joaoduartemariucio.kmais_app_android.presentation.maintenance

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.app.LocalDependencies
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.DateField
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.FailureMessage
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.FormDialog
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.FormState
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.LabeledNumberField
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.NumberInput
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.OdometerHint
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionCard
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionDivider
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionFooter
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionHeader
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ValidationFooter
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.keepsKeyboard
import kotlinx.coroutines.launch

@Composable
fun ServiceEntryFormDialog(
    mode: ServiceEntryFormViewModel.Mode,
    otherReadings: List<OdometerSample>,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dependencies = LocalDependencies.current
    val viewModel =
        remember {
            ServiceEntryFormViewModel(
                mode = mode,
                repository = dependencies.serviceRepository,
                dateProvider = dependencies.dateProvider,
                otherReadings = otherReadings,
            )
        }
    val coroutines = rememberCoroutineScope()

    FormDialog(
        title = stringResource(if (mode is ServiceEntryFormViewModel.Mode.Create) R.string.new_service else R.string.edit_service),
        state = viewModel.state,
        canSave = viewModel.canSave,
        onSave = {
            coroutines.launch {
                viewModel.save()
                if (viewModel.state == FormState.Saved) {
                    onSaved()
                    onDismiss()
                }
            }
        },
        onCancel = onDismiss,
    ) {
        item {
            SectionHeader(stringResource(R.string.service_type))
            SectionCard { ServiceKindGrid(viewModel.kind) { viewModel.kind = it } }
        }

        item {
            SectionCard(Modifier.padding(top = 16.dp)) {
                DateField(stringResource(R.string.date), viewModel.date, { viewModel.date = it }, dependencies.dateProvider.zone)
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.odometer),
                    unit = stringResource(R.string.unit_km),
                    style = NumberInput.Style.KILOMETERS,
                    text = viewModel.odometer,
                    onTextChange = { viewModel.odometer = it },
                )
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.cost),
                    style = NumberInput.Style.MONEY,
                    text = viewModel.cost,
                    onTextChange = { viewModel.cost = it },
                )
            }
            SectionFooter { OdometerHint(viewModel.previousReading) }
        }

        item {
            SectionHeader(stringResource(R.string.notes))
            SectionCard {
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    placeholder = { Text(stringResource(R.string.notes_prompt)) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().keepsKeyboard(),
                )
            }
        }

        val state = viewModel.state
        if (state is FormState.Failed) {
            item { SectionCard(Modifier.padding(top = 16.dp)) { FailureMessage(state.error) } }
        }

        if (viewModel.issues.isNotEmpty()) {
            item { SectionCard(Modifier.padding(top = 16.dp)) { ValidationFooter(viewModel.issues) } }
        }
    }
}

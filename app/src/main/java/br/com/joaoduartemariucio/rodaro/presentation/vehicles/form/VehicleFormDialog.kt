package br.com.joaoduartemariucio.rodaro.presentation.vehicles.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleKind
import br.com.joaoduartemariucio.rodaro.presentation.common.FailureMessage
import br.com.joaoduartemariucio.rodaro.presentation.common.FormDialog
import br.com.joaoduartemariucio.rodaro.presentation.common.FormState
import br.com.joaoduartemariucio.rodaro.presentation.common.LabeledNumberField
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionCard
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionDivider
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionFooter
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionHeader
import br.com.joaoduartemariucio.rodaro.presentation.common.ValidationFooter
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.InterstitialAds
import br.com.joaoduartemariucio.rodaro.presentation.common.icon
import br.com.joaoduartemariucio.rodaro.presentation.common.keepsKeyboard
import br.com.joaoduartemariucio.rodaro.presentation.common.label
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.form.components.ServiceRuleRow
import kotlinx.coroutines.launch

@Composable
fun VehicleFormDialog(
    mode: VehicleFormViewModel.Mode,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    existingRules: List<ServiceRule> = emptyList(),
) {
    val dependencies = LocalDependencies.current
    val viewModel =
        remember {
            VehicleFormViewModel(
                mode = mode,
                existingRules = existingRules,
                vehicleRepository = dependencies.vehicleRepository,
                serviceRepository = dependencies.serviceRepository,
                dateProvider = dependencies.dateProvider,
            )
        }
    val coroutines = rememberCoroutineScope()

    FormDialog(
        title = stringResource(if (mode is VehicleFormViewModel.Mode.Create) R.string.new_vehicle else R.string.edit_vehicle),
        state = viewModel.state,
        canSave = viewModel.canSave,
        onSave = {
            coroutines.launch {
                viewModel.save()
                if (viewModel.state == FormState.Saved) {
                    if (mode is VehicleFormViewModel.Mode.Create) {
                        InterstitialAds.show()
                    }
                    onSaved()
                    onDismiss()
                }
            }
        },
        onCancel = onDismiss,
    ) {
        item {
            val label = stringResource(R.string.vehicle_type)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp).semantics { contentDescription = label }) {
                VehicleKind.entries.forEachIndexed { index, kind ->
                    SegmentedButton(
                        selected = viewModel.kind == kind,
                        onClick = { viewModel.kind = kind },
                        shape = SegmentedButtonDefaults.itemShape(index, VehicleKind.entries.size),
                        icon = { Icon(kind.icon, contentDescription = null) },
                        label = { Text(stringResource(kind.label)) },
                    )
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.identification))
            SectionCard {
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text(stringResource(R.string.name)) },
                    placeholder = { Text(stringResource(R.string.name_prompt)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().keepsKeyboard(),
                )
                OutlinedTextField(
                    value = viewModel.plate,
                    onValueChange = { viewModel.plate = it.uppercase() },
                    label = { Text(stringResource(R.string.plate)) },
                    placeholder = { Text(stringResource(R.string.plate_prompt)) },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).keepsKeyboard(),
                )
            }
        }

        item {
            SectionHeader(stringResource(R.string.specs))
            SectionCard {
                LabeledNumberField(
                    title = stringResource(R.string.initial_odometer),
                    unit = stringResource(R.string.unit_km),
                    style = NumberInput.Style.KILOMETERS,
                    text = viewModel.initialOdometer,
                    onTextChange = { viewModel.initialOdometer = it },
                )
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.tank_capacity),
                    unit = stringResource(R.string.unit_liters),
                    style = NumberInput.Style.LITERS,
                    text = viewModel.tankCapacity,
                    onTextChange = { viewModel.tankCapacity = it },
                )
            }
        }

        item {
            SectionHeader(stringResource(R.string.service_reminders))
            SectionCard {
                viewModel.rules.forEachIndexed { index, rule ->
                    ServiceRuleRow(rule, onRemove = { viewModel.removeRules(setOf(index)) })
                    SectionDivider()
                }
                TextButton(onClick = viewModel::addRule) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null)
                    Text(stringResource(R.string.add_reminder), modifier = Modifier.padding(start = 8.dp))
                }
            }
            SectionFooter {
                Text(
                    stringResource(R.string.reminders_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

package br.com.joaoduartemariucio.kmais_app_android.presentation.fueling

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.Palette
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionCard
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionDivider
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionFooter
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionHeader
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ValidationFooter
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedCurrency
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun FuelEntryFormDialog(
    mode: FuelEntryFormViewModel.Mode,
    otherReadings: List<OdometerSample>,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    suggestedPrice: BigDecimal? = null,
) {
    val dependencies = LocalDependencies.current
    val viewModel =
        remember {
            FuelEntryFormViewModel(
                mode = mode,
                repository = dependencies.fuelEntryRepository,
                dateProvider = dependencies.dateProvider,
                otherReadings = otherReadings,
                suggestedPrice = suggestedPrice,
            )
        }
    val coroutines = rememberCoroutineScope()

    FormDialog(
        title = stringResource(if (mode is FuelEntryFormViewModel.Mode.Create) R.string.new_fuel_up else R.string.edit_fuel_up),
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
            SectionCard(Modifier.padding(top = 8.dp)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val total = viewModel.totalCost
                    AnimatedContent(targetState = total?.formattedCurrency ?: "—", label = "total") { text ->
                        Text(
                            text,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = if (total == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.fuel))
            SectionCard {
                LabeledNumberField(
                    title = stringResource(R.string.liters),
                    unit = stringResource(R.string.unit_liters),
                    style = NumberInput.Style.LITERS,
                    text = viewModel.liters,
                    onTextChange = { viewModel.liters = it },
                )
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.price_per_liter),
                    style = NumberInput.Style.FUEL_PRICE,
                    text = viewModel.pricePerLiter,
                    onTextChange = { viewModel.pricePerLiter = it },
                )
                SectionDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = Palette.fuel)
                    Text(stringResource(R.string.full_tank), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = viewModel.isFullTank, onCheckedChange = { viewModel.isFullTank = it })
                }
            }
            if (viewModel.isUsingSuggestedPrice) {
                SectionFooter {
                    Text(
                        stringResource(R.string.price_from_last_fuel_up),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.when_header))
            SectionCard {
                DateField(stringResource(R.string.date), viewModel.date, { viewModel.date = it }, dependencies.dateProvider.zone)
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.odometer),
                    unit = stringResource(R.string.unit_km),
                    style = NumberInput.Style.KILOMETERS,
                    text = viewModel.odometer,
                    onTextChange = { viewModel.odometer = it },
                )
            }
            SectionFooter { OdometerHint(viewModel.previousReading, viewModel.distanceSincePrevious) }
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

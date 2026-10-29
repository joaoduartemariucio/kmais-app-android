package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TripCost
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.presentation.common.EmptyState
import br.com.joaoduartemariucio.rodaro.presentation.common.LabeledNumberField
import br.com.joaoduartemariucio.rodaro.presentation.common.LoadingIndicator
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.common.Palette
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionCard
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionDivider
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionFooter
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionHeader
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AdBanner
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.InterstitialAds
import br.com.joaoduartemariucio.rodaro.presentation.common.dismissesKeyboard
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedCurrency
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedLiters
import br.com.joaoduartemariucio.rodaro.presentation.common.icon
import br.com.joaoduartemariucio.rodaro.presentation.common.message
import br.com.joaoduartemariucio.rodaro.presentation.common.rememberLocalizer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCostScreen() {
    val dependencies = LocalDependencies.current
    val viewModel =
        viewModel {
            TripCostViewModel(
                vehicleRepository = dependencies.vehicleRepository,
                fuelEntryRepository = dependencies.fuelEntryRepository,
            )
        }
    val coroutines = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.trip_cost)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = viewModel.state) {
                TripCostViewModel.State.Loading -> {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }

                is TripCostViewModel.State.Failed -> {
                    EmptyState(
                        title = stringResource(R.string.could_not_load_vehicles),
                        icon = Icons.Filled.Warning,
                        message = state.error.message(rememberLocalizer()),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                TripCostViewModel.State.Loaded -> {
                    TripForm(viewModel, onSelect = { vehicle -> coroutines.launch { viewModel.select(vehicle) } })
                }
            }
        }
    }
}

@Composable
private fun TripForm(
    viewModel: TripCostViewModel,
    onSelect: (Vehicle) -> Unit,
) {
    LaunchedEffect(viewModel.cost != null) { if (viewModel.cost != null) InterstitialAds.show() }

    LazyColumn(
        Modifier.fillMaxSize().dismissesKeyboard(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ResultCard(viewModel.cost) }

        if (viewModel.vehicles.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.vehicle))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.vehicles.forEach { vehicle ->
                        FilterChip(
                            selected = viewModel.selectedVehicle?.id == vehicle.id,
                            onClick = { onSelect(vehicle) },
                            label = { Text(vehicle.name) },
                            leadingIcon = { Icon(vehicle.kind.icon, contentDescription = null) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.trip))
            SectionCard {
                LabeledNumberField(
                    title = stringResource(R.string.distance),
                    unit = stringResource(R.string.unit_km),
                    style = NumberInput.Style.KILOMETERS,
                    text = viewModel.distance,
                    onTextChange = { viewModel.distance = it },
                )
                SectionDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.round_trip), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = viewModel.isRoundTrip, onCheckedChange = { viewModel.isRoundTrip = it })
                }
            }
        }

        item {
            SectionHeader(stringResource(R.string.fuel))
            SectionCard {
                LabeledNumberField(
                    title = stringResource(R.string.consumption),
                    unit = stringResource(R.string.unit_km_per_liter),
                    style = NumberInput.Style.CONSUMPTION,
                    text = viewModel.consumption,
                    onTextChange = { viewModel.consumption = it },
                )
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.price_per_liter),
                    style = NumberInput.Style.FUEL_PRICE,
                    text = viewModel.pricePerLiter,
                    onTextChange = { viewModel.pricePerLiter = it },
                )
            }
            val footers =
                buildList {
                    when {
                        viewModel.isUsingAverageConsumption -> add(R.string.consumption_from_average)
                        viewModel.selectedVehicle != null && viewModel.consumption.isEmpty() -> add(R.string.consumption_missing)
                    }
                    if (viewModel.isUsingLastPrice) add(R.string.price_from_last_fuel_up)
                }
            if (footers.isNotEmpty()) {
                SectionFooter {
                    footers.forEach { footer ->
                        Text(
                            stringResource(footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { AdBanner(Modifier.padding(top = 24.dp)) }
    }
}

@Composable
private fun ResultCard(cost: TripCost?) {
    SectionCard(Modifier.padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.fuel_cost),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(targetState = cost?.fuelCost?.formattedCurrency ?: "—", label = "trip-cost") { text ->
                Text(
                    text,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (cost == null) MaterialTheme.colorScheme.onSurfaceVariant else Palette.fuel,
                    maxLines = 1,
                )
            }
            if (cost == null) {
                Text(
                    stringResource(R.string.trip_cost_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SectionDivider()
                Metric(stringResource(R.string.total_distance), cost.distance.formattedKilometers())
                Metric(stringResource(R.string.liters), cost.liters.formattedLiters())
                Metric(stringResource(R.string.cost_per_km), cost.costPerKilometer.formattedCurrency)
            }
        }
    }
}

@Composable
private fun Metric(
    title: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

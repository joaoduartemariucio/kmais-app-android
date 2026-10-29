package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TripCost
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
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
import br.com.joaoduartemariucio.rodaro.presentation.common.consumptionUnit
import br.com.joaoduartemariucio.rodaro.presentation.common.dismissesKeyboard
import br.com.joaoduartemariucio.rodaro.presentation.common.distanceUnit
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedCurrency
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedLiters
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedPerKilometer
import br.com.joaoduartemariucio.rodaro.presentation.common.icon
import br.com.joaoduartemariucio.rodaro.presentation.common.message
import br.com.joaoduartemariucio.rodaro.presentation.common.perDistanceTitle
import br.com.joaoduartemariucio.rodaro.presentation.common.rememberLocalizer
import br.com.joaoduartemariucio.rodaro.presentation.common.volumePriceTitle
import br.com.joaoduartemariucio.rodaro.presentation.common.volumeTitle
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCostScreen() {
    val dependencies = LocalDependencies.current
    val viewModel =
        viewModel {
            TripCostViewModel(
                vehicleRepository = dependencies.vehicleRepository,
                fuelEntryRepository = dependencies.fuelEntryRepository,
                routeRepository = dependencies.routeRepository,
                routeEstimateProvider = dependencies.routeEstimateProvider,
                dateProvider = dependencies.dateProvider,
                analytics = dependencies.analytics,
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
                    TripForm(
                        viewModel,
                        onSelect = { vehicle -> coroutines.launch { viewModel.select(vehicle) } },
                        onLookup = { coroutines.launch { viewModel.lookupRealDistance() } },
                        onSave = { coroutines.launch { if (viewModel.saveRoute()) InterstitialAds.show() } },
                        onRestore = viewModel::restoreRoute,
                        onDelete = { id -> coroutines.launch { viewModel.deleteRoute(id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun TripForm(
    viewModel: TripCostViewModel,
    onSelect: (Vehicle) -> Unit,
    onLookup: () -> Unit,
    onSave: () -> Unit,
    onRestore: (SavedRoute) -> Unit,
    onDelete: (UUID) -> Unit,
) {
    LaunchedEffect(viewModel.cost != null) {
        if (viewModel.cost != null) {
            viewModel.reportCalculation()
            InterstitialAds.show()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().dismissesKeyboard(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ResultCard(viewModel.cost, viewModel.hasTolls) }

        viewModel.saveError?.let { error ->
            item {
                SectionFooter {
                    Text(
                        stringResource(R.string.save_calculation_failed) + " " + error.message(rememberLocalizer()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

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
                PlaceField(
                    title = stringResource(R.string.route_origin),
                    text = viewModel.origin,
                    onTextChange = { viewModel.origin = it },
                )
                SectionDivider()
                PlaceField(
                    title = stringResource(R.string.route_destination),
                    text = viewModel.destination,
                    onTextChange = { viewModel.destination = it },
                )
                SectionDivider()
                when (val lookup = viewModel.lookupState) {
                    is TripCostViewModel.LookupState.Working -> {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(Modifier.heightIn(min = 24.dp), strokeWidth = 2.dp)
                            Text(
                                stringResource(R.string.looking_up_distance),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is TripCostViewModel.LookupState.Failed -> {
                        Text(
                            if (lookup.error is RouteLookupError.NoApiKey) {
                                stringResource(R.string.lookup_no_api_key)
                            } else {
                                stringResource(R.string.lookup_failed)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    else -> {
                        Unit
                    }
                }
                if (viewModel.lookupState !is TripCostViewModel.LookupState.Working) {
                    OutlinedButton(
                        onClick = onLookup,
                        enabled = viewModel.canLookupRealDistance,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                        Text(stringResource(R.string.lookup_real_distance), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                SectionDivider()
                LabeledNumberField(
                    title = stringResource(R.string.distance),
                    unit = distanceUnit(),
                    style = NumberInput.Style.KILOMETERS,
                    text = viewModel.distance,
                    onTextChange = { viewModel.distance = it },
                )
                SectionDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable { viewModel.isRoundTrip = !viewModel.isRoundTrip }
                        .padding(end = 4.dp),
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
                    unit = consumptionUnit(),
                    style = NumberInput.Style.CONSUMPTION,
                    text = viewModel.consumption,
                    onTextChange = { viewModel.consumption = it },
                )
                SectionDivider()
                LabeledNumberField(
                    title = volumePriceTitle(),
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

        item {
            Button(
                onClick = onSave,
                enabled = viewModel.canSaveRoute,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Text(stringResource(R.string.save_calculation), modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (viewModel.savedRoutes.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.saved_routes)) }
            items(viewModel.savedRoutes.size) { index ->
                val route = viewModel.savedRoutes[index]
                SavedRouteRow(route, onRestore = { onRestore(route) }, onDelete = { onDelete(route.id) })
            }
        }

        item { AdBanner(Modifier.padding(top = 24.dp)) }
    }
}

@Composable
private fun PlaceField(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text(title) },
        placeholder = { Text(stringResource(R.string.route_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun SavedRouteRow(
    route: SavedRoute,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onRestore)) {
                Text(
                    "${route.origin} → ${route.destination}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${route.totalDistanceKm.formattedKilometers()} · ${route.fuelCost.formattedCurrency}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (route.hasTolls) {
                    Text(
                        stringResource(R.string.tolls_yes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Filled.Replay, contentDescription = stringResource(R.string.restore_route))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_route),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    cost: TripCost?,
    hasTolls: Boolean?,
) {
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
                Metric(volumeTitle(), cost.liters.formattedLiters())
                Metric(perDistanceTitle(), cost.costPerKilometer.formattedPerKilometer(rememberLocalizer()))
                if (hasTolls != null) {
                    Metric(
                        stringResource(R.string.tolls),
                        stringResource(if (hasTolls) R.string.tolls_yes else R.string.tolls_no),
                    )
                }
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

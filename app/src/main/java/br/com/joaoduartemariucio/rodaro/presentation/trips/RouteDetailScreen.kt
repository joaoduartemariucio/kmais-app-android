package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.presentation.common.Chip
import br.com.joaoduartemariucio.rodaro.presentation.common.EmptyState
import br.com.joaoduartemariucio.rodaro.presentation.common.LoadingIndicator
import br.com.joaoduartemariucio.rodaro.presentation.common.MetricTileCard
import br.com.joaoduartemariucio.rodaro.presentation.common.Palette
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionCard
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionDivider
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionFooter
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionHeader
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedConsumption
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedCurrency
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedFullDate
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedLiters
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedPerKilometer
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedPerLiter
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
fun RouteDetailScreen(
    routeId: UUID,
    onEdit: (SavedRoute) -> Unit,
    onBack: () -> Unit,
) {
    val dependencies = LocalDependencies.current
    val viewModel =
        viewModel {
            RouteDetailViewModel(
                routeRepository = dependencies.routeRepository,
                vehicleRepository = dependencies.vehicleRepository,
                analytics = dependencies.analytics,
            )
        }
    val coroutines = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, routeId) { viewModel.load(routeId) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.route_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    viewModel.route?.let { route ->
                        IconButton(onClick = { onEdit(route) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_route_title))
                        }
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete_route),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
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
                RouteDetailViewModel.State.Loading -> {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }

                RouteDetailViewModel.State.Missing -> {
                    EmptyState(
                        title = stringResource(R.string.route_missing),
                        icon = Icons.Filled.Route,
                        message = "",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is RouteDetailViewModel.State.Failed -> {
                    EmptyState(
                        title = stringResource(R.string.saved_routes_load_failed),
                        icon = Icons.Filled.Warning,
                        message = state.error.message(rememberLocalizer()),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is RouteDetailViewModel.State.Loaded -> {
                    RouteDetail(viewModel, state.route, onEdit = { onEdit(state.route) })
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.delete_route_confirm_title)) },
            text = { Text(stringResource(R.string.delete_route_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    coroutines.launch { if (viewModel.delete()) onBack() }
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RouteDetail(
    viewModel: RouteDetailViewModel,
    route: SavedRoute,
    onEdit: () -> Unit,
) {
    val localizer = rememberLocalizer()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RouteTimeline(route.origin, route.destination, emphasized = true)
                    SectionDivider()
                    Text(
                        stringResource(if (route.tollCost.signum() > 0) R.string.trip_total_cost else R.string.fuel_cost),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        route.totalCost.formattedCurrency,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.fuel,
                        maxLines = 1,
                    )
                    if (route.tollCost.signum() > 0) {
                        Text(
                            stringResource(
                                R.string.trip_cost_breakdown,
                                route.fuelCost.formattedCurrency,
                                route.tollCost.formattedCurrency,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        viewModel.vehicle?.let { Chip(it.name, it.kind.icon, MaterialTheme.colorScheme.primary) }
                        Chip(
                            stringResource(if (route.isRoundTrip) R.string.round_trip else R.string.one_way),
                            Icons.Filled.SwapHoriz,
                            Palette.service,
                        )
                        if (route.hasTolls) {
                            Chip(
                                route.tollCost.takeIf { it.signum() > 0 }?.let {
                                    stringResource(
                                        R.string.route_tolls_cost,
                                        it.formattedCurrency,
                                    )
                                }
                                    ?: stringResource(R.string.tolls),
                                Icons.Filled.Toll,
                                Palette.orange,
                            )
                        }
                    }
                    RouteMapCard(path = route.path, origin = route.origin, destination = route.destination)
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTileCard(
                    title = stringResource(R.string.total_distance),
                    value = route.totalDistanceKm.formattedKilometers(),
                    caption =
                        if (route.isRoundTrip) {
                            stringResource(R.string.one_way_distance) + " " +
                                route.distanceKm.formattedKilometers()
                        } else {
                            null
                        },
                    icon = Icons.Filled.Route,
                    tint = Palette.blue,
                    modifier = Modifier.weight(1f),
                )
                MetricTileCard(
                    title = volumeTitle(),
                    value = route.liters.formattedLiters(1),
                    caption = null,
                    icon = Icons.Filled.LocalGasStation,
                    tint = Palette.fuel,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTileCard(
                    title = perDistanceTitle(),
                    value = route.costPerKilometer.formattedPerKilometer(localizer),
                    caption = null,
                    icon = Icons.Filled.Paid,
                    tint = Palette.green,
                    modifier = Modifier.weight(1f),
                )
                MetricTileCard(
                    title = stringResource(R.string.consumption),
                    value = route.kilometersPerLiter.takeIf { it > 0 }?.formattedConsumption(localizer),
                    caption = null,
                    icon = Icons.Filled.Speed,
                    tint = Palette.service,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionCard { TankPlanContent(viewModel.tankPlan) }
        }

        item {
            SectionHeader(stringResource(R.string.calculation_inputs))
            SectionCard {
                InputRow(stringResource(R.string.vehicle), viewModel.vehicle?.name ?: "—")
                SectionDivider()
                InputRow(
                    stringResource(R.string.consumption),
                    if (route.kilometersPerLiter > 0) route.kilometersPerLiter.formattedConsumption(localizer) else "—",
                )
                SectionDivider()
                InputRow(
                    volumePriceTitle(),
                    if (route.pricePerLiter.signum() > 0) route.pricePerLiter.formattedPerLiter(localizer) else "—",
                )
            }
            SectionFooter {
                Text(
                    stringResource(R.string.calculation_snapshot_note, route.createdAt.formattedFullDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        viewModel.actionError?.let { error ->
            item {
                SectionFooter {
                    Text(
                        stringResource(R.string.delete_route_failed) + " " + error.message(localizer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text(stringResource(R.string.edit_route_title), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InputRow(
    title: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

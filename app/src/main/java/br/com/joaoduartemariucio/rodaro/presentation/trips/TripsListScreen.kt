package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.presentation.common.Chip
import br.com.joaoduartemariucio.rodaro.presentation.common.EmptyState
import br.com.joaoduartemariucio.rodaro.presentation.common.LoadingIndicator
import br.com.joaoduartemariucio.rodaro.presentation.common.Palette
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionFooter
import br.com.joaoduartemariucio.rodaro.presentation.common.SectionHeader
import br.com.joaoduartemariucio.rodaro.presentation.common.SwipeAction
import br.com.joaoduartemariucio.rodaro.presentation.common.SwipeActionsBox
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AdBanner
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedCurrency
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedDay
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedLiters
import br.com.joaoduartemariucio.rodaro.presentation.common.icon
import br.com.joaoduartemariucio.rodaro.presentation.common.message
import br.com.joaoduartemariucio.rodaro.presentation.common.rememberLocalizer
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsListScreen(
    onNewRoute: () -> Unit,
    onOpenRoute: (SavedRoute) -> Unit,
) {
    val dependencies = LocalDependencies.current
    val viewModel =
        viewModel {
            TripsListViewModel(
                routeRepository = dependencies.routeRepository,
                vehicleRepository = dependencies.vehicleRepository,
                analytics = dependencies.analytics,
            )
        }
    val coroutines = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var pendingDelete by remember { mutableStateOf<SavedRoute?>(null) }

    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.route)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (viewModel.routes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onNewRoute,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_route_calculation)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = viewModel.state) {
                TripsListViewModel.State.Loading -> {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }

                is TripsListViewModel.State.Failed -> {
                    EmptyState(
                        title = stringResource(R.string.saved_routes_load_failed),
                        icon = Icons.Filled.Warning,
                        message = state.error.message(rememberLocalizer()),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                TripsListViewModel.State.Loaded -> {
                    if (viewModel.routes.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.saved_routes_empty_title),
                            icon = Icons.Filled.Route,
                            message = stringResource(R.string.saved_routes_empty_message),
                            modifier = Modifier.align(Alignment.Center),
                            actions = {
                                Button(onClick = onNewRoute, modifier = Modifier.padding(top = 8.dp)) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Text(stringResource(R.string.new_route_calculation), modifier = Modifier.padding(start = 8.dp))
                                }
                            },
                        )
                    } else {
                        RouteList(
                            viewModel = viewModel,
                            onOpenRoute = onOpenRoute,
                            onDelete = { pendingDelete = it },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { route ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_route_confirm_title)) },
            text = { Text(stringResource(R.string.delete_route_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    coroutines.launch { viewModel.deleteRoute(route) }
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RouteList(
    viewModel: TripsListViewModel,
    onOpenRoute: (SavedRoute) -> Unit,
    onDelete: (SavedRoute) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item { Summary(viewModel) }

        if (viewModel.filterVehicles.size > 1) {
            item { VehicleFilter(viewModel.filterVehicles, viewModel.vehicleFilter, viewModel::filter) }
        }

        item {
            SectionHeader(
                stringResource(R.string.saved_routes),
                trailing = pluralStringResource(R.plurals.route_count, viewModel.visibleRoutes.size, viewModel.visibleRoutes.size),
            )
        }

        items(viewModel.visibleRoutes, key = { it.id }) { route ->
            SwipeActionsBox(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                endAction = SwipeAction(stringResource(R.string.delete), Icons.Filled.Delete, Palette.red) { onDelete(route) },
            ) {
                SavedRouteCard(route = route, vehicle = viewModel.vehicle(route), onOpen = { onOpenRoute(route) })
            }
        }

        viewModel.actionError?.let { error ->
            item {
                SectionFooter {
                    Text(
                        stringResource(R.string.delete_route_failed) + " " + error.message(rememberLocalizer()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item { AdBanner(Modifier.padding(top = 24.dp)) }
    }
}

@Composable
private fun Summary(viewModel: TripsListViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = Palette.fuel.copy(alpha = 0.12f),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.total_estimated),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    viewModel.visibleTotalCost.formattedCurrency,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.fuel,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.total_distance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    viewModel.visibleTotalDistance.formattedKilometers(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun VehicleFilter(
    vehicles: List<Vehicle>,
    selected: UUID?,
    onSelect: (UUID?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.all_vehicles)) },
            )
        }
        items(vehicles, key = { it.id }) { vehicle ->
            FilterChip(
                selected = selected == vehicle.id,
                onClick = { onSelect(vehicle.id) },
                label = { Text(vehicle.name) },
                leadingIcon = { Icon(vehicle.kind.icon, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun SavedRouteCard(
    route: SavedRoute,
    vehicle: Vehicle?,
    onOpen: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RouteTimeline(route.origin, route.destination, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        route.totalCost.formattedCurrency,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Palette.fuel,
                        maxLines = 1,
                    )
                    Text(
                        route.liters.formattedLiters(1),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(route.totalDistanceKm.formattedKilometers(), Icons.Filled.Route, Palette.blue)
                if (route.isRoundTrip) Chip(stringResource(R.string.round_trip), Icons.Filled.SwapHoriz, Palette.service)
                if (route.hasTolls) Chip(stringResource(R.string.tolls), Icons.Filled.Toll, Palette.orange)
            }
            Text(
                listOfNotNull(vehicle?.name, route.createdAt.formattedDay()).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

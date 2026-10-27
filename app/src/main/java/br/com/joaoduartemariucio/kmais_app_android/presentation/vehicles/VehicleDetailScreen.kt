package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.app.LocalDependencies
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.EmptyState
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.Palette
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionCard
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionDivider
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SectionHeader
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SwipeAction
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SwipeActionsBox
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedCurrency
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedMonthAndYear
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.message
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.rememberLocalizer
import br.com.joaoduartemariucio.kmais_app_android.presentation.fueling.FuelEntryFormDialog
import br.com.joaoduartemariucio.kmais_app_android.presentation.fueling.FuelEntryFormViewModel
import br.com.joaoduartemariucio.kmais_app_android.presentation.fueling.FuelEntryRow
import br.com.joaoduartemariucio.kmais_app_android.presentation.maintenance.ServiceEntryFormDialog
import br.com.joaoduartemariucio.kmais_app_android.presentation.maintenance.ServiceEntryFormViewModel
import br.com.joaoduartemariucio.kmais_app_android.presentation.maintenance.ServiceEntryRow
import kotlinx.coroutines.launch

private sealed interface DetailSheet {
    data object NewFuel : DetailSheet

    data object NewService : DetailSheet

    data class EditFuel(
        val entry: FuelEntry,
    ) : DetailSheet

    data class EditService(
        val entry: ServiceEntry,
    ) : DetailSheet

    data class EditVehicle(
        val rules: List<ServiceRule>,
    ) : DetailSheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicle: Vehicle,
    onBack: () -> Unit,
) {
    val dependencies = LocalDependencies.current
    val viewModel =
        viewModel {
            VehicleDetailViewModel(
                vehicle = vehicle,
                fuelEntryRepository = dependencies.fuelEntryRepository,
                serviceRepository = dependencies.serviceRepository,
                dateProvider = dependencies.dateProvider,
                reminders = dependencies.reminderSync,
            )
        }
    val coroutines = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<DetailSheet?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(viewModel) { viewModel.load() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text(vehicle.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { sheet = DetailSheet.EditVehicle(viewModel.rules) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_vehicle))
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
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AddButton(stringResource(R.string.fuel_up), Icons.Filled.LocalGasStation, Palette.fuel, Modifier.weight(1f)) {
                    sheet = DetailSheet.NewFuel
                }
                AddButton(stringResource(R.string.service), Icons.Filled.Build, Palette.service, Modifier.weight(1f)) {
                    sheet = DetailSheet.NewService
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = viewModel.state) {
                VehicleDetailViewModel.State.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is VehicleDetailViewModel.State.Failed -> {
                    EmptyState(
                        title = stringResource(R.string.could_not_load_vehicle),
                        icon = Icons.Filled.Warning,
                        message = state.error.message(rememberLocalizer()),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is VehicleDetailViewModel.State.Loaded -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            coroutines.launch {
                                isRefreshing = true
                                viewModel.load()
                                isRefreshing = false
                            }
                        },
                    ) {
                        Loaded(
                            content = state.content,
                            viewModel = viewModel,
                            onSheet = { sheet = it },
                            onDelete = { item -> coroutines.launch { viewModel.delete(item) } },
                        )
                    }
                }
            }
        }
    }

    val reload: () -> Unit = { coroutines.launch { viewModel.load() } }
    val dismiss = { sheet = null }
    when (val current = sheet) {
        DetailSheet.NewFuel -> {
            FuelEntryFormDialog(
                mode = FuelEntryFormViewModel.Mode.Create(vehicle.id),
                otherReadings = viewModel.odometerReadings(),
                suggestedPrice = viewModel.lastPricePerLiter,
                onSaved = reload,
                onDismiss = dismiss,
            )
        }

        is DetailSheet.EditFuel -> {
            FuelEntryFormDialog(
                mode = FuelEntryFormViewModel.Mode.Edit(current.entry),
                otherReadings = viewModel.odometerReadings(excluding = VehicleTimeline.Item.Fuel(current.entry)),
                onSaved = reload,
                onDismiss = dismiss,
            )
        }

        DetailSheet.NewService -> {
            ServiceEntryFormDialog(
                mode = ServiceEntryFormViewModel.Mode.Create(vehicle.id),
                otherReadings = viewModel.odometerReadings(),
                onSaved = reload,
                onDismiss = dismiss,
            )
        }

        is DetailSheet.EditService -> {
            ServiceEntryFormDialog(
                mode = ServiceEntryFormViewModel.Mode.Edit(current.entry),
                otherReadings = viewModel.odometerReadings(excluding = VehicleTimeline.Item.Service(current.entry)),
                onSaved = reload,
                onDismiss = dismiss,
            )
        }

        is DetailSheet.EditVehicle -> {
            VehicleFormDialog(
                mode = VehicleFormViewModel.Mode.Edit(vehicle),
                existingRules = current.rules,
                onSaved = reload,
                onDismiss = dismiss,
            )
        }

        null -> {
            Unit
        }
    }
}

@Composable
private fun AddButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null)
        Text(text, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Loaded(
    content: VehicleDetailViewModel.Content,
    viewModel: VehicleDetailViewModel,
    onSheet: (DetailSheet) -> Unit,
    onDelete: (VehicleTimeline.Item) -> Unit,
) {
    val stats = content.stats
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)) {
        item(key = "hero") { SectionCard(contentPadding = PaddingValues(0.dp)) { VehicleHeroCard(viewModel.vehicle, stats) } }
        item(key = "summary") { Box(Modifier.padding(top = 12.dp)) { VehicleSummaryCard(stats) } }

        item(key = "maintenance") {
            SectionHeader(stringResource(R.string.maintenance))
            SectionCard {
                if (stats.dues.isEmpty()) {
                    TextButton(onClick = { onSheet(DetailSheet.EditVehicle(content.rules)) }) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Text(stringResource(R.string.set_up_service_reminders), modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    stats.dues.forEachIndexed { index, item ->
                        if (index > 0) SectionDivider()
                        ServiceDueRow(item)
                    }
                }
            }
        }

        if (stats.consumptionHistory.size >= 2) {
            item(key = "consumption") {
                SectionCard(Modifier.padding(top = 16.dp), contentPadding = PaddingValues(0.dp)) {
                    ConsumptionChartCard(stats.consumptionHistory, stats.averageConsumption)
                }
            }
        }

        if (content.spending.any { it.total.signum() > 0 }) {
            item(key = "spending") {
                SectionCard(Modifier.padding(top = 16.dp), contentPadding = PaddingValues(0.dp)) { SpendingChartCard(content.spending) }
            }
        }

        if (viewModel.hasHistory) {
            item(key = "history") {
                SectionHeader(stringResource(R.string.history))
                val label = stringResource(R.string.show)
                SingleChoiceSegmentedButtonRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = label },
                ) {
                    val filters = VehicleTimeline.Filter.entries
                    filters.forEachIndexed { index, filter ->
                        SegmentedButton(
                            selected = viewModel.filter == filter,
                            onClick = { viewModel.filter = filter },
                            shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                            label = { Text(stringResource(filter.label)) },
                        )
                    }
                }
            }
            for (section in viewModel.visibleSections) {
                item(key = "month-${section.month}") {
                    SectionHeader(section.month.formattedMonthAndYear(), trailing = section.total.formattedCurrency)
                }
                items(section.items, key = { it.id }) { item ->
                    HistoryRow(item, stats, onSheet, onDelete)
                }
            }
        } else {
            item(key = "empty") {
                SectionCard(Modifier.padding(top = 16.dp)) {
                    EmptyState(
                        title = stringResource(R.string.no_history_yet),
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        message = stringResource(R.string.history_empty_message),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: VehicleTimeline.Item,
    stats: VehicleStats,
    onSheet: (DetailSheet) -> Unit,
    onDelete: (VehicleTimeline.Item) -> Unit,
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        SwipeActionsBox(endAction = SwipeAction(stringResource(R.string.delete), Icons.Filled.Delete, Palette.red) { onDelete(item) }) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Box(
                    Modifier
                        .clickable {
                            when (item) {
                                is VehicleTimeline.Item.Fuel -> onSheet(DetailSheet.EditFuel(item.entry))
                                is VehicleTimeline.Item.Service -> onSheet(DetailSheet.EditService(item.entry))
                            }
                        }.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    when (item) {
                        is VehicleTimeline.Item.Fuel -> FuelEntryRow(item.entry, stats.consumptionByEntry[item.entry.id])
                        is VehicleTimeline.Item.Service -> ServiceEntryRow(item.entry)
                    }
                }
            }
        }
    }
}

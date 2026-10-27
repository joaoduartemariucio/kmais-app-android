package br.com.joaoduartemariucio.kmais_app_android.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles.VehicleDetailScreen
import br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles.VehicleListScreen
import br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles.VehicleListViewModel

@Composable
fun RootView() {
    val backStack = remember { mutableStateListOf<AppRoute>(AppRoute.Vehicles) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<AppRoute.Vehicles> {
                    VehicleListScreen(
                        scope = VehicleListViewModel.Scope.ACTIVE,
                        onOpen = { backStack += it },
                        onBack = null,
                    )
                }
                entry<AppRoute.ArchivedVehicles> {
                    VehicleListScreen(
                        scope = VehicleListViewModel.Scope.ARCHIVED,
                        onOpen = { backStack += it },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<AppRoute.VehicleDetail> { route ->
                    VehicleDetailScreen(vehicle = route.vehicle, onBack = { backStack.removeLastOrNull() })
                }
            },
    )
}

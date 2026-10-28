package br.com.joaoduartemariucio.rodaro.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleKind

val VehicleKind.label: Int
    get() =
        when (this) {
            VehicleKind.CAR -> R.string.vehicle_car
            VehicleKind.MOTORCYCLE -> R.string.vehicle_motorcycle
        }

val VehicleKind.icon: ImageVector
    get() =
        when (this) {
            VehicleKind.CAR -> Icons.Filled.DirectionsCar
            VehicleKind.MOTORCYCLE -> Icons.Filled.TwoWheeler
        }

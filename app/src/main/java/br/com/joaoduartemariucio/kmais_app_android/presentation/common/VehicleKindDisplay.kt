package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.VehicleKind

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

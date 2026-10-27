package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind

val ServiceKind.label: Int
    get() =
        when (this) {
            ServiceKind.OIL_CHANGE -> R.string.service_oil_change
            ServiceKind.OIL_FILTER -> R.string.service_oil_filter
            ServiceKind.AIR_FILTER -> R.string.service_air_filter
            ServiceKind.FUEL_FILTER -> R.string.service_fuel_filter
            ServiceKind.SPARK_PLUGS -> R.string.service_spark_plugs
            ServiceKind.BRAKES -> R.string.service_brakes
            ServiceKind.TIRES -> R.string.service_tires
            ServiceKind.CHAIN -> R.string.service_chain
            ServiceKind.COOLANT -> R.string.service_coolant
            ServiceKind.INSPECTION -> R.string.service_inspection
            ServiceKind.OTHER -> R.string.service_other
        }

val ServiceKind.icon: ImageVector
    get() =
        when (this) {
            ServiceKind.OIL_CHANGE, ServiceKind.OIL_FILTER -> Icons.Filled.OilBarrel
            ServiceKind.AIR_FILTER -> Icons.Filled.Air
            ServiceKind.FUEL_FILTER -> Icons.Filled.FilterAlt
            ServiceKind.SPARK_PLUGS -> Icons.Filled.Bolt
            ServiceKind.BRAKES -> Icons.Filled.DiscFull
            ServiceKind.TIRES -> Icons.Filled.TireRepair
            ServiceKind.CHAIN -> Icons.Filled.Link
            ServiceKind.COOLANT -> Icons.Filled.Thermostat
            ServiceKind.INSPECTION -> Icons.Filled.Checklist
            ServiceKind.OTHER -> Icons.Filled.Build
        }

package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.Chip
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.Palette
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedCurrency

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GarageOverviewCard(overview: VehicleListViewModel.Overview) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.spent_this_month),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                overview.spentThisMonth.formattedCurrency,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(
                pluralStringResource(R.plurals.vehicle_count, overview.vehicleCount, overview.vehicleCount),
                Icons.Filled.DirectionsCar,
                MaterialTheme.colorScheme.primary,
            )
            if (overview.overdueCount > 0) {
                Chip(
                    pluralStringResource(R.plurals.overdue_count, overview.overdueCount, overview.overdueCount),
                    Icons.Filled.Warning,
                    Palette.red,
                )
            }
            if (overview.dueSoonCount > 0) {
                Chip(
                    pluralStringResource(R.plurals.due_soon_count, overview.dueSoonCount, overview.dueSoonCount),
                    Icons.Filled.Schedule,
                    Palette.orange,
                )
            }
            if (overview.overdueCount == 0 && overview.dueSoonCount == 0) {
                Chip(stringResource(R.string.maintenance_up_to_date), Icons.Filled.Verified, Palette.green)
            }
        }
    }
}

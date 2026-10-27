package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.MaintenanceStatus
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.PlateView
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.StatusCapsule
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SymbolBadge
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.color
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedConsumption
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedPerKilometer
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.icon
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.label
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.rememberLocalizer

@Composable
fun VehicleCard(
    row: VehicleListViewModel.Row,
    modifier: Modifier = Modifier,
) {
    val localizer = rememberLocalizer()
    val stats = row.stats
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SymbolBadge(row.vehicle.kind.icon, MaterialTheme.colorScheme.primary, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    row.vehicle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(row.vehicle.kind.label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    row.vehicle.plate?.let { PlateView(it) }
                }
            }
            stats.attentionLevel?.takeIf { it != MaintenanceStatus.Level.ON_TRACK }?.let { StatusCapsule(it) }
        }

        Row {
            Figure(stats.currentOdometer.formattedKilometers(), stringResource(R.string.odometer), Modifier.weight(1f))
            Figure(stats.averageConsumption?.formattedConsumption(localizer), stringResource(R.string.consumption), Modifier.weight(1f))
            Figure(stats.cost.costPerKilometer?.formattedPerKilometer(localizer), stringResource(R.string.cost_per_km), Modifier.weight(1f))
        }

        stats.nextDue?.let { next ->
            val level = next.status.level
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(next.due.rule.kind.icon, contentDescription = null, tint = level.color, modifier = Modifier.size(18.dp))
                Text(stringResource(next.due.rule.kind.label), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(
                    next.relativeDue(localizer),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (level == MaintenanceStatus.Level.ON_TRACK) MaterialTheme.colorScheme.onSurfaceVariant else level.color,
                )
            }
        }
    }
}

@Composable
private fun Figure(
    value: String?,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

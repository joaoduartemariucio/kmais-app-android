package br.com.joaoduartemariucio.kmais_app_android.presentation.fueling

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.Palette
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.SymbolBadge
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.Tag
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedConsumption
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedCurrency
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedDay
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedLiters
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.formattedPerLiter
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.rememberLocalizer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuelEntryRow(
    entry: FuelEntry,
    consumption: Double?,
) {
    val localizer = rememberLocalizer()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SymbolBadge(Icons.Filled.LocalGasStation, Palette.fuel)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.liters.formattedLiters(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.pricePerLiter.formattedPerLiter(localizer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(entry.totalCost.formattedCurrency, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${entry.date.formattedDay()}  ${entry.odometer.formattedKilometers()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                consumption?.let { Tag(it.formattedConsumption(localizer), Palette.fuel) }
                Tag(
                    stringResource(if (entry.isFullTank) R.string.full_tank else R.string.partial_fill),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

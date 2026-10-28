package br.com.joaoduartemariucio.rodaro.presentation.vehicles.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.domain.fueling.calculations.ConsumptionInterval
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedConsumption
import br.com.joaoduartemariucio.rodaro.presentation.common.rememberLocalizer

@Composable
fun ConsumptionChartCard(
    intervals: List<ConsumptionInterval>,
    average: Double?,
) {
    val localizer = rememberLocalizer()
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.consumption), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (average != null) {
                Text(
                    stringResource(R.string.average_value, average.formattedConsumption(localizer)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ConsumptionChart(intervals, average, stringResource(R.string.consumption_over_time))
    }
}

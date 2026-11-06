package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TankPlan
import br.com.joaoduartemariucio.rodaro.presentation.common.Formats
import br.com.joaoduartemariucio.rodaro.presentation.common.Palette
import br.com.joaoduartemariucio.rodaro.presentation.common.SymbolBadge
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedLiters
import kotlin.math.ceil

private const val MAX_GAUGE_TANKS = 6

@Composable
fun RouteTimeline(
    origin: String,
    destination: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(
        modifier.clearAndSetSemantics { contentDescription = "$origin → $destination" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Box(
                Modifier
                    .width(2.dp)
                    .height(if (emphasized) 28.dp else 18.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Icon(Icons.Filled.Place, contentDescription = null, tint = Palette.red, modifier = Modifier.size(14.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (emphasized) 14.dp else 6.dp)) {
            Text(origin, style = style, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(destination, style = style, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TankPlanContent(
    plan: TankPlan?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SymbolBadge(Icons.Filled.LocalGasStation, if (plan == null) MaterialTheme.colorScheme.onSurfaceVariant else Palette.fuel)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.tank_plan),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (plan == null) {
                    Text(
                        stringResource(R.string.tank_plan_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        pluralStringResource(R.plurals.tank_fill_ups, plan.fillUps, plan.fillUps),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (plan != null) {
                Text(
                    stringResource(R.string.format_tanks, Formats.number(plan.tanks, 1)),
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.fuel,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (plan != null) {
            TankGauge(plan.tanks)
            Text(
                if (plan.fitsInOneTank) {
                    stringResource(R.string.fits_in_one_tank)
                } else {
                    pluralStringResource(R.plurals.stops_on_the_way, plan.stopsOnTheWay, plan.stopsOnTheWay)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.range_per_tank),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${plan.rangePerTank.formattedKilometers()} · ${plan.tankCapacity.formattedLiters(0)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TankGauge(tanks: Double) {
    val count = ceil(tanks - 1e-6).toInt().coerceAtLeast(1)
    if (count > MAX_GAUGE_TANKS) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val fill = (tanks - index).coerceIn(0.0, 1.0).toFloat()
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(5.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(maxOf(fill, 0.04f))
                        .fillMaxHeight()
                        .background(Palette.fuel, RoundedCornerShape(5.dp)),
                )
            }
        }
    }
}

@Composable
fun SourceNote(
    text: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

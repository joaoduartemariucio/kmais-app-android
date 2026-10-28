package br.com.joaoduartemariucio.rodaro.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceStatus

@Composable
fun SymbolBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Box(
        modifier.size(size).background(tint.copy(alpha = 0.15f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun StatusCapsule(
    level: MaintenanceStatus.Level,
    modifier: Modifier = Modifier,
) {
    Chip(stringResource(level.label), level.icon, level.color, modifier)
}

@Composable
fun Chip(
    text: String,
    icon: ImageVector?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.background(tint.copy(alpha = 0.14f), CircleShape).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint, maxLines = 1)
    }
}

@Composable
fun Tag(
    text: String,
    tint: Color,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        maxLines = 1,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f), CircleShape)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun PlateView(plate: String) {
    Text(
        plate,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
fun ProgressBar(
    value: Double,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = value.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxWidth(maxOf(fraction, 0.02f))
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.75f), tint)), CircleShape),
        )
    }
}

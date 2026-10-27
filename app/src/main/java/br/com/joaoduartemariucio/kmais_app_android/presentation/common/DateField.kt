package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.joaoduartemariucio.kmais_app_android.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    title: String,
    date: Instant,
    onDateChange: (Instant) -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var isPicking by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = { isPicking = true }) { Text(date.formattedFullDate(zone)) }
    }

    if (isPicking) {
        val local = date.atZone(zone)
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    local
                        .toLocalDate()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { isPicking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateChange(combine(day, local.toLocalTime(), zone))
                    }
                    isPicking = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = { isPicking = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun combine(
    day: LocalDate,
    time: java.time.LocalTime,
    zone: ZoneId,
): Instant = day.atTime(time).atZone(zone).toInstant()

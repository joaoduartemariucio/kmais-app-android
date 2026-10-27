package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun ValidationFooter(
    issues: List<ValidationIssue>,
    modifier: Modifier = Modifier,
) {
    if (issues.isEmpty()) return
    val localizer = rememberLocalizer()
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (issue in issues) {
            val tint = if (issue.isBlocking) Palette.red else Palette.orange
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    if (issue.isBlocking) Icons.Filled.Warning else Icons.Filled.Info,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
                Text(issue.message(localizer), style = MaterialTheme.typography.bodySmall, color = tint)
            }
        }
    }
}

@Composable
fun FailureMessage(error: Throwable) {
    val localizer = rememberLocalizer()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Palette.red, modifier = Modifier.size(18.dp))
        Text(error.message(localizer), color = Palette.red, style = MaterialTheme.typography.bodyMedium)
    }
}

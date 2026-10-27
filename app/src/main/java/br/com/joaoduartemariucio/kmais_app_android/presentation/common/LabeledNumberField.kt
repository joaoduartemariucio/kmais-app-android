package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun LabeledNumberField(
    title: String,
    style: NumberInput.Style,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    val focusManager = LocalFocusManager.current
    var value by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    if (value.text != text) value = TextFieldValue(text, TextRange(text.length))

    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = value,
            onValueChange = { changed ->
                val masked = NumberInput.masked(changed.text, value.text, style)
                value = TextFieldValue(masked, TextRange(masked.length))
                if (masked != text) onTextChange(masked)
            },
            modifier = Modifier.weight(1f).keepsKeyboard().semantics { contentDescription = title },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.text.isEmpty()) {
                        Text(
                            NumberInput.text(BigDecimal.ZERO, style),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                }
            },
        )
        if (unit != null) {
            Text(unit, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

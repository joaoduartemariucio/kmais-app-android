package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onAction: () -> Unit,
)

@Composable
fun SwipeActionsBox(
    modifier: Modifier = Modifier,
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val coroutines = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        onDismiss = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> startAction?.onAction?.invoke()
                SwipeToDismissBoxValue.EndToStart -> endAction?.onAction?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            coroutines.launch { state.reset() }
        },
        backgroundContent = {
            val action =
                when (state.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> startAction
                    SwipeToDismissBoxValue.EndToStart -> endAction
                    SwipeToDismissBoxValue.Settled -> null
                }
            if (action != null) {
                Box(
                    Modifier.fillMaxSize().background(action.color).padding(horizontal = 24.dp),
                    contentAlignment =
                        if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(action.icon, contentDescription = null, tint = Color.White)
                        Text(action.label, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
    ) {
        content()
    }
}

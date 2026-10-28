package br.com.joaoduartemariucio.rodaro.presentation.common

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private const val LOADING_DELAY_MILLIS = 400L

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_DELAY_MILLIS)
        isVisible = true
    }
    if (isVisible) CircularProgressIndicator(modifier)
}

package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

class KeyboardTapTracker {
    var isOnTextInput = false
    var isPointerDown = false
}

val LocalKeyboardTapTracker = staticCompositionLocalOf { KeyboardTapTracker() }

@Composable
fun Modifier.dismissesKeyboard(): Modifier {
    val focusManager = LocalFocusManager.current
    val tracker = LocalKeyboardTapTracker.current
    val connection =
        remember(focusManager, tracker) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (tracker.isPointerDown && source == NestedScrollSource.UserInput && available.y != 0f) focusManager.clearFocus()
                    return Offset.Zero
                }
            }
        }
    return nestedScroll(connection).pointerInput(focusManager, tracker) {
        awaitEachGesture {
            tracker.isOnTextInput = false
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            tracker.isPointerDown = true
            val up = waitForUpOrCancellation(PointerEventPass.Final)
            tracker.isPointerDown = false
            if (up != null && !tracker.isOnTextInput) focusManager.clearFocus()
        }
    }
}

@Composable
fun Modifier.keepsKeyboard(): Modifier {
    val tracker = LocalKeyboardTapTracker.current
    return pointerInput(tracker) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            tracker.isOnTextInput = true
        }
    }
}

package io.github.ianpogi5.nightstand.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/**
 * Current wall-clock time, updated on each minute boundary while the
 * lifecycle is at least STARTED (no ticking with the screen off).
 */
@Composable
fun rememberMinuteTime(): State<LocalDateTime> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val time = remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                time.value = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }
    return time
}

@Composable
fun rememberIs24HourFormat(): Boolean {
    val context = LocalContext.current
    return remember(context) { DateFormat.is24HourFormat(context) }
}

package io.github.ianpogi5.nightstand.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class BatteryStatus(val percent: Int?, val charging: Boolean)

/**
 * Battery level and charging state from the sticky ACTION_BATTERY_CHANGED
 * broadcast — needs no permission and no polling.
 */
@Composable
fun rememberBatteryStatus(): BatteryStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(BatteryStatus(percent = null, charging = false)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                status = intent.toBatteryStatus()
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { status = it.toBatteryStatus() }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return status
}

private fun Intent.toBatteryStatus(): BatteryStatus {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val state = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return BatteryStatus(
        percent = if (level >= 0 && scale > 0) level * 100 / scale else null,
        charging = state == BatteryManager.BATTERY_STATUS_CHARGING ||
            state == BatteryManager.BATTERY_STATUS_FULL,
    )
}

@Composable
fun BatteryIndicator(
    status: BatteryStatus,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val percent = status.percent ?: return
    Text(
        text = if (status.charging) "⚡ $percent%" else "$percent%",
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Light,
        modifier = modifier,
    )
}

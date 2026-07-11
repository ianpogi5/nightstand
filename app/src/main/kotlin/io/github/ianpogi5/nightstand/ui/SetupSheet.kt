package io.github.ianpogi5.nightstand.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.ianpogi5.nightstand.Prefs
import io.github.ianpogi5.nightstand.R
import io.github.ianpogi5.nightstand.trigger.ChargingTriggerService

@Composable
fun SetupSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var autoLaunch by remember { mutableStateOf(Prefs.autoLaunchEnabled(context)) }
    var landscapeOnly by remember { mutableStateOf(Prefs.landscapeOnly(context)) }
    var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // The overlay grant happens in system settings; re-check when we return.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canOverlay = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* FGS runs either way; the notification is just hidden if denied */ }

    fun setAutoLaunch(enabled: Boolean) {
        autoLaunch = enabled
        Prefs.setAutoLaunchEnabled(context, enabled)
        if (enabled) {
            if (Build.VERSION.SDK_INT >= 33) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            ChargingTriggerService.start(context)
            if (!canOverlay) context.openOverlaySettings()
        } else {
            ChargingTriggerService.stop(context)
        }
    }

    Surface(
        color = Color(0xF2121216),
        contentColor = Color(0xFFEDEDED),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.width(440.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = stringResource(R.string.setup_title), fontSize = 22.sp)

            SwitchRow(
                label = stringResource(R.string.setup_auto_launch),
                checked = autoLaunch,
                onCheckedChange = ::setAutoLaunch,
            )
            SwitchRow(
                label = stringResource(R.string.setup_landscape_only),
                checked = landscapeOnly,
                onCheckedChange = {
                    landscapeOnly = it
                    Prefs.setLandscapeOnly(context, it)
                },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.setup_overlay),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                if (canOverlay) {
                    Text(
                        text = stringResource(R.string.setup_overlay_granted),
                        fontSize = 15.sp,
                        color = Color(0xFF7FBF7F),
                    )
                } else {
                    TextButton(onClick = { context.openOverlaySettings() }) {
                        Text(stringResource(R.string.setup_overlay_open))
                    }
                }
            }
            Text(
                text = stringResource(R.string.setup_overlay_rationale),
                fontSize = 13.sp,
                color = Color(0xFF9A9A9A),
            )

            Text(
                text = stringResource(R.string.setup_oneui_note),
                fontSize = 13.sp,
                color = Color(0xFF9A9A9A),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { context.openAppInfo() }) {
                    Text(stringResource(R.string.setup_app_info))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.setup_done))
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun android.content.Context.openOverlaySettings() {
    startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        ),
    )
}

private fun android.content.Context.openAppInfo() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName"),
        ),
    )
}

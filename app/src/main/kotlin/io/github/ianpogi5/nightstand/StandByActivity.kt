package io.github.ianpogi5.nightstand

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.ianpogi5.nightstand.trigger.ChargingTriggerService
import io.github.ianpogi5.nightstand.ui.SetupSheet
import io.github.ianpogi5.nightstand.ui.StandByScreen

class StandByActivity : ComponentActivity() {

    // "Unplug → app exits": only ever delivered as a transition, so a
    // manually opened app simply stays up until unplugged or backed out.
    private val unplugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        if (Prefs.autoLaunchEnabled(this)) {
            ChargingTriggerService.start(this)
        }

        setContent {
            // Bumped whenever the setup sheet writes a pref, so reads refresh.
            var settingsRev by rememberSaveable { mutableStateOf(0) }
            val nightBrightness = remember(settingsRev) { Prefs.nightBrightness(this) }
            val clockStyle = remember(settingsRev) { Prefs.clockStyle(this) }
            val hiddenCalendars = remember(settingsRev) { Prefs.hiddenCalendars(this) }

            var nightDim by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(nightDim, nightBrightness) {
                setScreenBrightness(
                    if (nightDim) {
                        nightBrightness
                    } else {
                        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
                )
            }

            var hasCalendarPermission by rememberSaveable {
                mutableStateOf(hasPermission(Manifest.permission.READ_CALENDAR))
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted -> hasCalendarPermission = granted }
            LaunchedEffect(Unit) {
                if (!hasCalendarPermission) {
                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            }

            var showSetup by rememberSaveable { mutableStateOf(!Prefs.setupShown(this)) }

            Box(modifier = Modifier.fillMaxSize()) {
                StandByScreen(
                    dimmed = nightDim,
                    onToggleDim = { nightDim = !nightDim },
                    hasCalendarPermission = hasCalendarPermission,
                    clockStyle = clockStyle,
                    hiddenCalendars = hiddenCalendars,
                    onOpenSetup = { showSetup = true },
                )
                if (showSetup) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SetupSheet(
                            hasCalendarPermission = hasCalendarPermission,
                            onSettingsChanged = { settingsRev++ },
                            onDismiss = {
                                showSetup = false
                                Prefs.setSetupShown(this@StandByActivity)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(unplugReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
    }

    override fun onStop() {
        unregisterReceiver(unplugReceiver)
        super.onStop()
    }

    private fun setScreenBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

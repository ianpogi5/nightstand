package io.github.ianpogi5.nightstand.trigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.IBinder
import android.provider.Settings
import io.github.ianpogi5.nightstand.Prefs
import io.github.ianpogi5.nightstand.R
import io.github.ianpogi5.nightstand.StandByActivity
import kotlin.math.abs

/**
 * Waits for the charger. ACTION_POWER_CONNECTED is not on the implicit
 * broadcast exceptions list, so a manifest receiver never fires on
 * API 26+; a runtime-registered receiver inside a foreground service is
 * the only reliable, poll-free way to hear it.
 *
 * The accelerometer is only registered between power-connected and
 * either launch or power-disconnected, so the service draws nothing
 * while the phone is off the charger.
 */
class ChargingTriggerService : Service() {

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> stopWatchingOrientation()
            }
        }
    }

    private var orientationListener: SensorEventListener? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        registerReceiver(
            powerReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            },
        )
        if (isCharging()) onPowerConnected()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopWatchingOrientation()
        unregisterReceiver(powerReceiver)
        super.onDestroy()
    }

    private fun onPowerConnected() {
        if (!Prefs.landscapeOnly(this)) {
            launchStandBy()
        } else {
            startWatchingOrientation()
        }
    }

    private fun startWatchingOrientation() {
        if (orientationListener != null) return
        val sensorManager = getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            launchStandBy()
            return
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Gravity mostly along the device x-axis = held sideways.
                if (abs(event.values[0]) > LANDSCAPE_GRAVITY_THRESHOLD) {
                    stopWatchingOrientation()
                    launchStandBy()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        orientationListener = listener
    }

    private fun stopWatchingOrientation() {
        orientationListener?.let {
            getSystemService(SensorManager::class.java).unregisterListener(it)
        }
        orientationListener = null
    }

    private fun launchStandBy() {
        // Background activity launch is only exempted while the user has
        // granted "Display over other apps"; without it, do nothing.
        if (!Settings.canDrawOverlays(this)) return
        startActivity(
            Intent(this, StandByActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun isCharging(): Boolean {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.trigger_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.trigger_notification_title))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "charging_trigger"
        private const val LANDSCAPE_GRAVITY_THRESHOLD = 7f

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, ChargingTriggerService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChargingTriggerService::class.java))
        }
    }
}

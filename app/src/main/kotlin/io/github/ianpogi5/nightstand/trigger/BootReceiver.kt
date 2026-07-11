package io.github.ianpogi5.nightstand.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.ianpogi5.nightstand.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs.autoLaunchEnabled(context)) {
            ChargingTriggerService.start(context)
        }
    }
}

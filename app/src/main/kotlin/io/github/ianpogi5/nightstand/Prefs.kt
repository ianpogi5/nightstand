package io.github.ianpogi5.nightstand

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SharedPreferences instead of DataStore: the boot receiver needs a
 * synchronous read before it can decide to start the trigger service,
 * and the whole settings surface is a handful of booleans.
 */
object Prefs {
    private const val FILE = "nightstand"

    private const val KEY_AUTO_LAUNCH = "auto_launch"
    private const val KEY_LANDSCAPE_ONLY = "landscape_only"
    private const val KEY_SETUP_SHOWN = "setup_shown"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun autoLaunchEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_LAUNCH, false)

    fun setAutoLaunchEnabled(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_AUTO_LAUNCH, value) }

    fun landscapeOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LANDSCAPE_ONLY, true)

    fun setLandscapeOnly(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_LANDSCAPE_ONLY, value) }

    fun setupShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_SHOWN, false)

    fun setSetupShown(context: Context) =
        prefs(context).edit { putBoolean(KEY_SETUP_SHOWN, true) }
}

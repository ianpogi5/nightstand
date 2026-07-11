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
    private const val KEY_NIGHT_BRIGHTNESS = "night_brightness"
    private const val KEY_CLOCK_STYLE = "clock_style"
    private const val KEY_HIDDEN_CALENDARS = "hidden_calendars"

    const val CLOCK_DIGITAL = "digital"
    const val CLOCK_ANALOG = "analog"

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

    fun nightBrightness(context: Context): Float =
        prefs(context).getFloat(KEY_NIGHT_BRIGHTNESS, 0.05f)

    fun setNightBrightness(context: Context, value: Float) =
        prefs(context).edit { putFloat(KEY_NIGHT_BRIGHTNESS, value) }

    fun clockStyle(context: Context): String =
        prefs(context).getString(KEY_CLOCK_STYLE, CLOCK_DIGITAL) ?: CLOCK_DIGITAL

    fun setClockStyle(context: Context, value: String) =
        prefs(context).edit { putString(KEY_CLOCK_STYLE, value) }

    fun hiddenCalendars(context: Context): Set<Long> =
        prefs(context).getStringSet(KEY_HIDDEN_CALENDARS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun setHiddenCalendars(context: Context, ids: Set<Long>) =
        prefs(context).edit {
            putStringSet(KEY_HIDDEN_CALENDARS, ids.map(Long::toString).toSet())
        }
}

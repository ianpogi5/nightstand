package io.github.ianpogi5.nightstand.calendar

import android.content.Context
import android.provider.CalendarContract

data class CalendarInfo(val id: Long, val name: String, val color: Int)

object Calendars {

    /** All calendars on the device, for the picker. Needs READ_CALENDAR. */
    fun query(context: Context): List<CalendarInfo> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val calendars = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars += CalendarInfo(
                    id = cursor.getLong(0),
                    name = cursor.getString(1).orEmpty().ifBlank { "(unnamed)" },
                    color = cursor.getInt(2),
                )
            }
        }
        return calendars
    }
}

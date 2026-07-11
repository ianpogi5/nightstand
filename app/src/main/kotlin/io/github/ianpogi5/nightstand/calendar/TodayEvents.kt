package io.github.ianpogi5.nightstand.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class EventInstance(
    val eventId: Long,
    val title: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    val color: Int,
)

object TodayEvents {

    /**
     * Instances from all visible calendars between now and the end of the
     * local day, soonest first. Caller must hold READ_CALENDAR.
     */
    fun query(
        context: Context,
        hiddenCalendars: Set<Long> = emptySet(),
        max: Int = 5,
    ): List<EventInstance> {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, startOfDay.toEpochMilli())
            ContentUris.appendId(it, endOfDay.toEpochMilli())
            it.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        var selection = "${CalendarContract.Instances.VISIBLE} = 1 AND " +
            "${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != " +
            "${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        if (hiddenCalendars.isNotEmpty()) {
            selection += " AND ${CalendarContract.Instances.CALENDAR_ID} NOT IN " +
                hiddenCalendars.joinToString(",", "(", ")")
        }

        val events = mutableListOf<EventInstance>()
        context.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext() && events.size < max) {
                val end = Instant.ofEpochMilli(cursor.getLong(3))
                if (end < now) continue // already over
                events += EventInstance(
                    eventId = cursor.getLong(0),
                    title = cursor.getString(1).orEmpty().ifBlank { "(untitled)" },
                    begin = Instant.ofEpochMilli(cursor.getLong(2)),
                    end = end,
                    allDay = cursor.getInt(4) == 1,
                    color = cursor.getInt(5),
                )
            }
        }
        return events
    }
}

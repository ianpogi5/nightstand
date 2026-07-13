package io.github.ianpogi5.nightstand.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

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
     * All of today's instances from all visible calendars, soonest first —
     * including ones already over, so the caller can cache the list and
     * filter by the current time itself. Caller must hold READ_CALENDAR.
     */
    fun query(
        context: Context,
        hiddenCalendars: Set<Long> = emptySet(),
    ): List<EventInstance> {
        val zone = ZoneId.systemDefault()
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
            while (cursor.moveToNext()) {
                val allDay = cursor.getInt(4) == 1
                var begin = Instant.ofEpochMilli(cursor.getLong(2))
                var end = Instant.ofEpochMilli(cursor.getLong(3))
                if (allDay) {
                    // All-day instances are stored as UTC midnights; remap them to
                    // local-day boundaries so yesterday's events don't linger past
                    // local midnight (and tomorrow's don't appear early).
                    begin = begin.utcDateAtStartOfDay(zone)
                    end = end.utcDateAtStartOfDay(zone)
                }
                if (end < startOfDay || begin >= endOfDay) continue // not today
                events += EventInstance(
                    eventId = cursor.getLong(0),
                    title = cursor.getString(1).orEmpty().ifBlank { "(untitled)" },
                    begin = begin,
                    end = end,
                    allDay = allDay,
                    color = cursor.getInt(5),
                )
            }
        }
        // Re-sort: remapping all-day begins to local midnight can reorder them
        // relative to the provider's BEGIN ASC ordering.
        return events.sortedBy { it.begin }
    }

    private fun Instant.utcDateAtStartOfDay(zone: ZoneId): Instant =
        atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant()
}

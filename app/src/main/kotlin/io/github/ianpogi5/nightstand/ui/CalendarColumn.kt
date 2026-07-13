package io.github.ianpogi5.nightstand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ianpogi5.nightstand.calendar.EventInstance
import io.github.ianpogi5.nightstand.calendar.TodayEvents
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How many upcoming events the screen shows at once. */
private const val MAX_EVENTS = 5

/**
 * Today's remaining events. The provider is queried on launch, at local-day
 * rollover, and when calendar data changes (ContentObserver); the minute
 * tick only re-filters the cached list, so an all-night session isn't
 * issuing a ContentResolver query every minute.
 */
@Composable
fun rememberTodayEvents(
    now: LocalDateTime,
    hasPermission: Boolean,
    hiddenCalendars: Set<Long>,
): List<EventInstance> {
    val context = LocalContext.current

    var calendarRev by remember { mutableStateOf(0) }
    DisposableEffect(context, hasPermission) {
        if (!hasPermission) {
            onDispose {}
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    calendarRev++
                }
            }
            context.contentResolver.registerContentObserver(
                CalendarContract.CONTENT_URI,
                true,
                observer,
            )
            onDispose { context.contentResolver.unregisterContentObserver(observer) }
        }
    }

    var todaysEvents by remember { mutableStateOf(emptyList<EventInstance>()) }
    LaunchedEffect(now.toLocalDate(), hasPermission, hiddenCalendars, calendarRev) {
        todaysEvents = if (hasPermission) {
            withContext(Dispatchers.IO) { TodayEvents.query(context, hiddenCalendars) }
        } else {
            emptyList()
        }
    }

    val nowInstant = now.atZone(ZoneId.systemDefault()).toInstant()
    return remember(todaysEvents, now.hour to now.minute) {
        todaysEvents.filter { it.end >= nowInstant }.take(MAX_EVENTS)
    }
}

@Composable
fun CalendarColumn(
    events: List<EventInstance>,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val titleColor = if (dimmed) Color(0xFF8A8A8A) else Color(0xFFEDEDED)
    val timeColor = if (dimmed) Color(0xFF565656) else Color(0xFF9A9A9A)
    val is24Hour = rememberIs24HourFormat()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        events.forEach { event ->
            EventRow(event, titleColor, timeColor, dimmed, is24Hour)
        }
    }
}

@Composable
private fun EventRow(
    event: EventInstance,
    titleColor: Color,
    timeColor: Color,
    dimmed: Boolean,
    is24Hour: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = Color(event.color).copy(alpha = if (dimmed) 0.5f else 1f),
                    shape = CircleShape,
                ),
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = event.title,
                color = titleColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = event.timeLabel(is24Hour),
                color = timeColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

private fun EventInstance.timeLabel(is24Hour: Boolean): String {
    if (allDay) return "All day"
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return "${begin.toLocalLabel(formatter)} – ${end.toLocalLabel(formatter)}"
}

private fun Instant.toLocalLabel(formatter: DateTimeFormatter): String =
    atZone(ZoneId.systemDefault()).toLocalTime().format(formatter)

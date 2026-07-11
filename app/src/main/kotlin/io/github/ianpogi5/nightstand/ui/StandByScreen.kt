package io.github.ianpogi5.nightstand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ianpogi5.nightstand.Prefs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/** Max burn-in shift in dp, applied in each axis every minute. */
private const val SHIFT_RANGE_DP = 8

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

@Composable
fun StandByScreen(
    dimmed: Boolean,
    onToggleDim: () -> Unit,
    hasCalendarPermission: Boolean,
    clockStyle: String,
    hiddenCalendars: Set<Long>,
    onOpenSetup: () -> Unit,
) {
    val now by rememberMinuteTime()
    val battery = rememberBatteryStatus()
    val events = rememberTodayEvents(now, hasCalendarPermission, hiddenCalendars)

    // Fresh pseudo-random offset each minute so no pixel stays lit in place.
    val shift = remember(now.hour to now.minute) {
        IntOffset(
            Random.nextInt(-SHIFT_RANGE_DP, SHIFT_RANGE_DP + 1),
            Random.nextInt(-SHIFT_RANGE_DP, SHIFT_RANGE_DP + 1),
        )
    }

    val contentColor = if (dimmed) Color(0xFF8A8A8A) else Color(0xFFEDEDED)
    val secondaryColor = if (dimmed) Color(0xFF565656) else Color(0xFF9A9A9A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleDim,
            ),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(shift.x.dp.roundToPx(), shift.y.dp.roundToPx()) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (clockStyle == Prefs.CLOCK_ANALOG) {
                    AnalogClock(
                        time = now,
                        color = contentColor,
                        secondaryColor = secondaryColor,
                    )
                } else {
                    Clock(time = now, color = contentColor)
                }
                Text(
                    text = now.toLocalDate().format(dateFormatter),
                    color = secondaryColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                )
            }
            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.width(72.dp))
                CalendarColumn(
                    events = events,
                    dimmed = dimmed,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
        }

        BatteryIndicator(
            status = battery,
            color = secondaryColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .offset { IntOffset(shift.x.dp.roundToPx(), shift.y.dp.roundToPx()) },
        )

        Text(
            text = "⚙",
            color = secondaryColor.copy(alpha = 0.6f),
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenSetup,
                )
                .padding(8.dp),
        )
    }
}

@Composable
private fun Clock(time: LocalDateTime, color: Color) {
    val pattern = if (rememberIs24HourFormat()) "HH:mm" else "h:mm"
    Text(
        text = time.format(DateTimeFormatter.ofPattern(pattern)),
        color = color,
        fontSize = 170.sp,
        fontWeight = FontWeight.Thin,
    )
}

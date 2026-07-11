package io.github.ianpogi5.nightstand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Minute-resolution analog face: hour ticks and two hands, no seconds —
 * a sweeping hand would keep pixels lit and defeat the burn-in shift.
 */
@Composable
fun AnalogClock(
    time: LocalDateTime,
    color: Color,
    secondaryColor: Color,
    size: Dp = 300.dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val radius = min(this.size.width, this.size.height) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        repeat(12) { hour ->
            val angle = Math.toRadians(hour * 30.0)
            val dir = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
            drawLine(
                color = secondaryColor,
                start = center + dir * (radius * 0.92f),
                end = center + dir * radius,
                strokeWidth = if (hour % 3 == 0) 6f else 3f,
                cap = StrokeCap.Round,
            )
        }

        val minuteAngle = Math.toRadians(time.minute * 6.0)
        val hourAngle = Math.toRadians((time.hour % 12 + time.minute / 60.0) * 30.0)

        drawLine(
            color = color,
            start = center,
            end = center + Offset(
                sin(hourAngle).toFloat(),
                -cos(hourAngle).toFloat(),
            ) * (radius * 0.5f),
            strokeWidth = 10f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = center,
            end = center + Offset(
                sin(minuteAngle).toFloat(),
                -cos(minuteAngle).toFloat(),
            ) * (radius * 0.78f),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = 8f, center = center)
    }
}

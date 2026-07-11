package io.github.ianpogi5.nightstand

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.ianpogi5.nightstand.ui.StandByScreen

/** Hardware brightness while night-dimmed, on a 0..1 scale. */
private const val NIGHT_BRIGHTNESS = 0.05f

class StandByActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            var nightDim by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(nightDim) {
                setScreenBrightness(
                    if (nightDim) {
                        NIGHT_BRIGHTNESS
                    } else {
                        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
                )
            }
            StandByScreen(
                dimmed = nightDim,
                onToggleDim = { nightDim = !nightDim },
            )
        }
    }

    private fun setScreenBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }
}

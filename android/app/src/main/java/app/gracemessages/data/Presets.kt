package app.gracemessages.data

import android.graphics.Color
import androidx.core.graphics.ColorUtils

data class GradientPreset(val name: String, val c1: Int, val c2: Int)
data class SolidPreset(val name: String, val color: Int)

/** Background presets, all chosen so white text stays readable (contrast >= 4.5 against white). */
object Presets {
    private fun c(argb: Long) = argb.toInt()

    val gradients = listOf(
        GradientPreset("Twilight", c(0xFF2E2366), c(0xFF8E3B6B)),
        GradientPreset("Ocean", c(0xFF0F2027), c(0xFF2C5364)),
        GradientPreset("Royal", c(0xFF141E30), c(0xFF35577D)),
        GradientPreset("Ember", c(0xFF5A1621), c(0xFFA8432E)),
        GradientPreset("Forest", c(0xFF0F3B3A), c(0xFF2E6B45)),
        GradientPreset("Midnight", c(0xFF000428), c(0xFF0B4A86)),
        GradientPreset("Dusk", c(0xFF3A1C71), c(0xFFA23F62)),
        GradientPreset("Bronze", c(0xFF3B2412), c(0xFF8A5A1E)),
    )

    val solids = listOf(
        SolidPreset("Indigo", c(0xFF2E2366)),
        SolidPreset("Navy", c(0xFF1E2A4A)),
        SolidPreset("Teal", c(0xFF0F4C5C)),
        SolidPreset("Forest", c(0xFF234D3A)),
        SolidPreset("Plum", c(0xFF4A2545)),
        SolidPreset("Burgundy", c(0xFF6B1E2E)),
        SolidPreset("Brown", c(0xFF4E3426)),
        SolidPreset("Charcoal", c(0xFF2B2B2E)),
    )

    /** Contrast ratio of white text on [color] (alpha ignored). */
    fun whiteContrast(color: Int): Double =
        ColorUtils.calculateContrast(Color.WHITE, ColorUtils.setAlphaComponent(color, 255))
}

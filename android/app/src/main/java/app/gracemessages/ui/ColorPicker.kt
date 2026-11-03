package app.gracemessages.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gracemessages.data.Presets
import android.graphics.Color as AColor

private fun hexOf(color: Int) = "%06X".format(color and 0xFFFFFF)

/**
 * Standard picker: a saturation/brightness square, a hue bar, and a hex field,
 * with a live preview of white serif text on the chosen colour.
 */
@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val start = remember(initial) { FloatArray(3).also { AColor.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(start[0]) }
    var s by remember { mutableFloatStateOf(start[1]) }
    var v by remember { mutableFloatStateOf(start[2]) }
    val color = AColor.HSVToColor(floatArrayOf(h, s, v))
    var hexText by remember { mutableStateOf(hexOf(initial)) }
    var editingHex by remember { mutableStateOf(false) }
    if (!editingHex && hexText != hexOf(color)) hexText = hexOf(color)
    val lowContrast = Presets.whiteContrast(color) < 3.0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(color) }) { Text("Use colour") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Pick a colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SatValPanel(h, s, v) { ns, nv -> editingHex = false; s = ns; v = nv }
                HueBar(h) { nh -> editingHex = false; h = nh }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(width = 96.dp, height = 56.dp).background(Color(color), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text("Aa", color = Color.White, fontFamily = FontFamily.Serif, fontSize = 22.sp) }
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            val t = raw.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            editingHex = true
                            hexText = t
                            if (t.length == 6) {
                                val out = FloatArray(3)
                                AColor.colorToHSV(AColor.parseColor("#$t"), out)
                                h = out[0]; s = out[1]; v = out[2]
                            }
                        },
                        prefix = { Text("#") },
                        singleLine = true,
                        label = { Text("Hex") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (lowContrast) {
                    Text(
                        "White text may be hard to read on this colour.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

/** Drag/tap anywhere: x = saturation, y = brightness. */
@Composable
private fun SatValPanel(h: Float, s: Float, v: Float, onChange: (Float, Float) -> Unit) {
    val update by rememberUpdatedState(onChange)
    val hueColor = Color.hsv(h, 1f, 1f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                fun at(p: Offset) = update(
                    (p.x / size.width).coerceIn(0f, 1f),
                    1f - (p.y / size.height).coerceIn(0f, 1f),
                )
                awaitEachGesture {
                    val down = awaitFirstDown()
                    at(down.position); down.consume()
                    drag(down.id) { c -> at(c.position); c.consume() }
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val c = Offset(s * size.width, (1f - v) * size.height)
        drawCircle(Color.Black.copy(alpha = 0.5f), radius = 11.dp.toPx(), center = c, style = Stroke(4.dp.toPx()))
        drawCircle(Color.White, radius = 11.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
    }
}

/** Rainbow bar: x = hue. */
@Composable
private fun HueBar(h: Float, onChange: (Float) -> Unit) {
    val update by rememberUpdatedState(onChange)
    val rainbow = remember { (0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) } }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                fun at(p: Offset) = update((p.x / size.width).coerceIn(0f, 1f) * 359.9f)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    at(down.position); down.consume()
                    drag(down.id) { c -> at(c.position); c.consume() }
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(rainbow))
        val c = Offset(h / 360f * size.width, size.height / 2)
        drawCircle(Color.Black.copy(alpha = 0.5f), radius = 11.dp.toPx(), center = c, style = Stroke(4.dp.toPx()))
        drawCircle(Color.White, radius = 11.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
    }
}

package app.gracemessages.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.gracemessages.data.WidgetSettings
import app.gracemessages.widget.TextFit

fun backgroundBrush(gradient: Boolean, c1: Int, c2: Int): Brush =
    if (gradient) Brush.linearGradient(listOf(Color(c1), Color(c2)), start = Offset.Zero, end = Offset.Infinite)
    else SolidColor(Color(c1))

fun Modifier.messageBackground(s: WidgetSettings, shape: Shape): Modifier =
    background(backgroundBrush(s.gradient, s.color1, s.color2), shape)

/** Mirrors the home-screen widget: white serif text, shrunk to fit. */
@Composable
fun WidgetPreview(text: String, settings: WidgetSettings, shape: Shape, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier.messageBackground(settings, shape).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val w = maxWidth.value
        val h = maxHeight.value
        val sp = remember(text, w, h, settings.fontSizeSp) {
            TextFit.fitSp(context, text, w, h, settings.fontSizeSp.toFloat())
        }
        Text(
            text,
            style = TextStyle(
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontSize = sp.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

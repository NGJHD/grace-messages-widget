package app.gracemessages.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue

object TextFit {
    const val MIN_SP = 10f

    /**
     * Largest font size (<= [maxSp]) at which [text], set in the system serif face,
     * fits inside a box of [widthDp] x [heightDp].
     */
    fun fitSp(context: Context, text: String, widthDp: Float, heightDp: Float, maxSp: Float): Float {
        if (widthDp <= 0f || heightDp <= 0f || text.isEmpty()) return maxSp
        var sp = maxSp
        while (sp > MIN_SP) {
            if (fits(context, text, widthDp, heightDp, sp)) return sp
            sp -= 0.5f
        }
        return MIN_SP
    }

    /**
     * Roughly how many characters of typical text fit in a widget of the given outer size
     * at [sp] without shrinking. [sample] is representative text (e.g. real messages).
     */
    fun capacity(context: Context, widgetWidthDp: Float, widgetHeightDp: Float, sp: Float, sample: String): Int {
        val pad = widgetPadding(widgetHeightDp)
        val w = widgetWidthDp - 2 * pad
        val h = widgetHeightDp - 2 * pad
        if (w <= 0f || h <= 0f || sample.isEmpty()) return 0
        var text = sample
        while (text.length < 1200) text += " $sample"
        var lo = 0
        var hi = 1200
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (fits(context, text.take(mid).trimEnd(), w, h, sp)) lo = mid else hi = mid - 1
        }
        return lo
    }

    private fun fits(context: Context, text: String, widthDp: Float, heightDp: Float, sp: Float): Boolean {
        val dm = context.resources.displayMetrics
        val widthPx = (widthDp * dm.density).toInt().coerceAtLeast(1)
        val heightPx = heightDp * dm.density * 0.95f // small safety margin
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.SERIF
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm)
        }
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(true)
            .build()
        return layout.height <= heightPx
    }
}

/** Inner padding of the widget; tighter on short (1-row) widgets so they hold more text. */
fun widgetPadding(heightDp: Float): Float = if (heightDp < 100f) 10f else 16f

/** Small top-left -> bottom-right gradient; stretched to fill the widget. */
fun gradientBitmap(c1: Int, c2: Int, size: Int = 160): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        isDither = true
        shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), c1, c2, Shader.TileMode.CLAMP)
    }
    Canvas(bmp).drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    return bmp
}

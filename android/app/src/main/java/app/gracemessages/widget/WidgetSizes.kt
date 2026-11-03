package app.gracemessages.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF

/** Sizes (in dp) of the Grace Message widgets currently placed on the home/cover screens. */
object WidgetSizes {
    data class Size(val widthDp: Float, val heightDp: Float)

    /**
     * One entry per placed widget. Launchers report a size per screen/orientation
     * (e.g. Fold inner vs. portrait/landscape); we take the smallest, so text fits everywhere.
     */
    fun placed(context: Context): List<Size> {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MessageWidgetReceiver::class.java))
        return ids.toList().mapNotNull { id -> smallest(mgr.getAppWidgetOptions(id)) }
    }

    private fun smallest(o: Bundle): Size? {
        val reported: List<SizeF> = if (Build.VERSION.SDK_INT >= 33) {
            o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
        } else {
            @Suppress("DEPRECATION")
            o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        } ?: emptyList()
        val candidates = reported.map { Size(it.width, it.height) }.ifEmpty {
            listOf(
                Size(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat(),
                    o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toFloat()),
                Size(o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).toFloat(),
                    o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).toFloat()),
            )
        }.filter { it.widthDp > 0f && it.heightDp > 0f }
        return candidates.minByOrNull { it.widthDp * it.heightDp }
    }
}

package app.gracemessages.widget

import android.content.Context
import app.gracemessages.data.MessageRepository

/**
 * The character limit is derived, not set: it's how much text the placed widgets hold
 * at the chosen font size. With several widgets (e.g. cover + inner screen), the
 * smallest one decides, so the message fits everywhere.
 */
object FitLimit {
    data class Fit(val size: WidgetSizes.Size, val chars: Int)

    @Volatile private var sample: String? = null

    /** Representative text (the real messages) used to estimate characters per area. */
    private fun sample(context: Context): String = sample
        ?: MessageRepository.dataset(context).messages.joinToString(" ") { it.text }.also { sample = it }

    fun perWidget(context: Context, fontSp: Int): List<Fit> {
        val text = sample(context)
        return WidgetSizes.placed(context).map {
            Fit(it, TextFit.capacity(context, it.widthDp, it.heightDp, fontSp.toFloat(), text))
        }
    }

    /** Max characters for the current message, or null when no widget is placed (no limit). */
    fun limit(context: Context, fontSp: Int): Int? = perWidget(context, fontSp).minOfOrNull { it.chars }
}

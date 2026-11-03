package app.gracemessages.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.gracemessages.MainActivity
import app.gracemessages.data.MessageRepository
import app.gracemessages.data.WidgetSettings
import app.gracemessages.data.dataStore
import app.gracemessages.data.toSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val EMPTY_TEXT = "Open Grace Messages to get started."

class MessageWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        MessageRepository.current(context) // roll over / re-fit if needed
        val initial = context.dataStore.data.first()
        provideContent {
            // Observe stored state so app changes show up even while this session is alive.
            val prefs by context.dataStore.data.collectAsState(initial)
            val settings = prefs.toSettings()
            val text = MessageRepository.storedCurrent(context, prefs)?.text ?: EMPTY_TEXT
            val bg = remember(settings.gradient, settings.color1, settings.color2) {
                if (settings.gradient) gradientBitmap(settings.color1, settings.color2) else null
            }
            WidgetContent(settings, text, bg)
        }
    }
}

@Composable
private fun WidgetContent(settings: WidgetSettings, text: String, gradient: Bitmap?) {
    val context = LocalContext.current
    val size = LocalSize.current
    val pad = widgetPadding(size.height.value)
    // Safety net only: messages are chosen to fit, but the estimate is approximate.
    val fontSp = TextFit.fitSp(
        context, text,
        widthDp = size.width.value - 2 * pad,
        heightDp = size.height.value - 2 * pad,
        maxSp = settings.fontSizeSp.toFloat(),
    )
    val base = GlanceModifier.fillMaxSize()
        .cornerRadius(20.dp)
        .clickable(actionStartActivity<MainActivity>())
    val withBg = if (gradient != null) base.background(ImageProvider(gradient), ContentScale.FillBounds)
    else base.background(Color(settings.color1))

    Box(modifier = withBg.padding(pad.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = fontSp.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** Process-wide scope for short fire-and-forget work from receivers. */
internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

class MessageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MessageWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refit(context)
        val app = context.applicationContext
        appScope.launch { RefreshScheduler.schedule(app) }
    }

    /** Widget resized (or moved between screens): the character limit may have changed. */
    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refit(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        refit(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        RefreshScheduler.cancel(context.applicationContext)
    }

    private fun refit(context: Context) {
        val app = context.applicationContext
        appScope.launch {
            MessageRepository.current(app)
            MessageWidget().updateAll(app)
        }
    }
}

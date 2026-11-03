package app.gracemessages.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.gracemessages.data.CategoryFilter
import app.gracemessages.data.Dataset
import app.gracemessages.data.MessageRepository
import app.gracemessages.widget.FitLimit
import app.gracemessages.widget.MessageWidgetReceiver
import java.text.NumberFormat
import kotlin.math.roundToInt


/**
 * Shows how many characters the placed widgets hold at [fontSp] and how much of the
 * dataset that allows. The limit itself is applied automatically.
 */
@Composable
fun WidgetFitCard(dataset: Dataset, category: CategoryFilter, fontSp: Int) {
    val context = LocalContext.current
    // Bumped on resume so we re-read widget sizes after the user resizes / adds a widget.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val fits = remember(tick, fontSp) { FitLimit.perWidget(context, fontSp) }
    val limit = fits.minOfOrNull { it.chars }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (fits.isNotEmpty()) {
                fits.forEachIndexed { i, f ->
                    val label = if (fits.size == 1) "Your widget" else "Widget ${i + 1}"
                    Text(
                        "$label holds about ${f.chars} characters",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    )
                }
                if (fits.size > 1) {
                    Text("Messages are chosen to fit the smallest one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Coverage(dataset, category, limit)
            } else {
                Text("No widget on your home screen yet",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Once it's added, messages are picked to fit its size at this font size.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val mgr = remember { AppWidgetManager.getInstance(context) }
                if (mgr.isRequestPinAppWidgetSupported) {
                    FilledTonalButton(onClick = {
                        mgr.requestPinAppWidget(ComponentName(context, MessageWidgetReceiver::class.java), null, null)
                    }) { Text("Add widget to home screen") }
                }
            }
        }
    }
}

@Composable
private fun Coverage(dataset: Dataset, category: CategoryFilter, limit: Int?) {
    val total = remember(dataset, category) { MessageRepository.eligible(dataset, category, null).size }
    val fit = remember(dataset, category, limit) { MessageRepository.eligible(dataset, category, limit).size }
    val pct = if (total == 0) 0 else (fit * 100f / total).roundToInt()
    val what = if (category == CategoryFilter.ALL) "messages" else "${category.label.lowercase()} messages"
    Text(
        "$pct% of $what fit (${fmt(fit)} of ${fmt(total)})",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    if (fit == 0 && total > 0) {
        Text(
            "Nothing fits at this size, so messages will be shrunk. Try a smaller font or a bigger widget.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    } else if (pct < 30) {
        Text(
            "A smaller font or a bigger widget will let more messages through.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun fmt(n: Int): String = NumberFormat.getIntegerInstance().format(n)

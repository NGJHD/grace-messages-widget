package app.gracemessages.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.gracemessages.data.CategoryFilter
import app.gracemessages.data.Dataset
import app.gracemessages.data.Presets
import app.gracemessages.data.WidgetSettings
import java.text.NumberFormat
import kotlin.math.roundToInt

private const val SAMPLE_TEXT = "You are not fighting for victory; you are fighting from victory."

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: WidgetSettings,
    dataset: Dataset,
    previewText: String?,
    onBack: () -> Unit,
    onLookChange: ((WidgetSettings) -> WidgetSettings) -> Unit,
    onFilterChange: ((WidgetSettings) -> WidgetSettings) -> Unit,
    onRefreshChange: (Int) -> Unit,
    onFontChange: (Int) -> Unit,
    shownCount: Int,
    onClearHistory: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    // Slider values are held locally while dragging and saved when released.
    var font by remember(settings.fontSizeSp) { mutableFloatStateOf(settings.fontSizeSp.toFloat()) }
    var editingColor by remember { mutableIntStateOf(0) } // 0 = none, 1 or 2
    val live = settings.copy(fontSizeSp = font.roundToInt())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Preview
                WidgetPreview(
                    text = previewText ?: SAMPLE_TEXT,
                    settings = live,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                )

                // Category
                Section("Category") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        CategoryFilter.entries.forEachIndexed { i, c ->
                            SegmentedButton(
                                selected = settings.category == c,
                                onClick = { if (settings.category != c) onFilterChange { it.copy(category = c) } },
                                shape = SegmentedButtonDefaults.itemShape(i, CategoryFilter.entries.size),
                            ) { Text(c.label) }
                        }
                    }
                }

                // Font
                Section("Font size") {
                    Text("${live.fontSizeSp} sp", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = font,
                        onValueChange = { font = it.roundToInt().toFloat() },
                        valueRange = WidgetSettings.MIN_FONT.toFloat()..WidgetSettings.MAX_FONT.toFloat(),
                        onValueChangeFinished = {
                            val v = font.roundToInt()
                            if (v != settings.fontSizeSp) onFontChange(v)
                        },
                    )
                    WidgetFitCard(dataset, settings.category, live.fontSizeSp)
                }

                // Background
                Section("Background") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(false to "Solid", true to "Gradient").forEachIndexed { i, (g, label) ->
                            SegmentedButton(
                                selected = settings.gradient == g,
                                onClick = { onLookChange { it.copy(gradient = g) } },
                                shape = SegmentedButtonDefaults.itemShape(i, 2),
                            ) { Text(label) }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (settings.gradient) {
                            Presets.gradients.forEach { p ->
                                Swatch(
                                    gradient = true, c1 = p.c1, c2 = p.c2,
                                    selected = settings.color1 == p.c1 && settings.color2 == p.c2,
                                    onClick = { onLookChange { it.copy(color1 = p.c1, color2 = p.c2) } },
                                )
                            }
                        } else {
                            Presets.solids.forEach { p ->
                                Swatch(
                                    gradient = false, c1 = p.color, c2 = p.color,
                                    selected = settings.color1 == p.color,
                                    onClick = { onLookChange { it.copy(color1 = p.color) } },
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Custom", style = MaterialTheme.typography.bodyLarge)
                        LabeledSwatch(if (settings.gradient) "Top-left" else "Colour", settings.color1) { editingColor = 1 }
                        if (settings.gradient) LabeledSwatch("Bottom-right", settings.color2) { editingColor = 2 }
                    }
                    val low = listOfNotNull(settings.color1, settings.color2.takeIf { settings.gradient })
                        .any { Presets.whiteContrast(it) < 3.0 }
                    if (low) Hint("White text may be hard to read on this colour. Try a darker shade.", warn = true)
                }

                // Refresh
                Section("New message") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetSettings.REFRESH_OPTIONS.forEach { h ->
                            FilterChip(
                                selected = settings.refreshHours == h,
                                onClick = { if (settings.refreshHours != h) onRefreshChange(h) },
                                label = { Text(if (h == 0) "Daily" else "Every ${h}h") },
                            )
                        }
                    }
                    Hint(
                        if (settings.refreshHours == 0) "Changes at midnight."
                        else "Changes every ${settings.refreshHours} hour${if (settings.refreshHours > 1) "s" else ""}, on the hour.",
                    )
                }

                // Read history
                Section("Read history") {
                    Hint("${fmt(shownCount)} of ${fmt(dataset.messages.size)} messages shown so far. Messages don't repeat until every eligible one has been shown.")
                    val red = MaterialTheme.colorScheme.error
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        enabled = shownCount > 0,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = red),
                        border = BorderStroke(1.dp, if (shownCount > 0) red else red.copy(alpha = 0.38f)),
                    ) {
                        Text("Clear read history")
                    }
                }

                Hint("${fmt(dataset.messages.size)} messages from ${fmt(dataset.sermons.size)} sermon${if (dataset.sermons.size == 1) "" else "s"}")
                Box(Modifier.height(24.dp))
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear read history?") },
            text = { Text("All messages can be shown again, and Previous starts afresh. The current message stays.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmClear = false; onClearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }

    if (editingColor != 0) {
        val initial = if (editingColor == 1) settings.color1 else settings.color2
        val which = editingColor
        ColorPickerDialog(
            initial = initial,
            onDismiss = { editingColor = 0 },
            onConfirm = { c ->
                editingColor = 0
                onLookChange { if (which == 1) it.copy(color1 = c) else it.copy(color2 = c) }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif)
        content()
    }
}

@Composable
private fun Hint(text: String, warn: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Swatch(gradient: Boolean, c1: Int, c2: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .background(backgroundBrush(gradient, c1, c2), CircleShape)
            .border(
                if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White)
    }
}

@Composable
private fun LabeledSwatch(label: String, color: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(40.dp)
                .background(Color(color), RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun fmt(n: Int): String = NumberFormat.getIntegerInstance().format(n)

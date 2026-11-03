package app.gracemessages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gracemessages.data.Message
import app.gracemessages.data.Period
import app.gracemessages.data.Sermon
import app.gracemessages.data.WidgetSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: WidgetSettings,
    message: Message?,
    sermon: Sermon?,
    onNext: () -> Unit,
    canGoBack: Boolean,
    onPrevious: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grace Messages", fontFamily = FontFamily.Serif) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp) // keeps line length comfortable on an unfolded Fold
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (message == null) {
                    EmptyState(onOpenSettings)
                } else {
                    MessageBody(settings, message, sermon)
                    Text(
                        nextChangeLabel(settings.refreshHours),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // FlowRow: side by side, wrapping onto two lines on the narrow cover screen.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = onPrevious, enabled = canGoBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Previous")
                        }
                        OutlinedButton(onClick = onNext) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Show next message")
                        }
                    }
                    Spacer(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBody(settings: WidgetSettings, message: Message, sermon: Sermon?) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .messageBackground(settings, RoundedCornerShape(24.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message.text,
            style = TextStyle(
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            listOfNotNull(message.category.label, message.scripture).joinToString("  ·  "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (sermon != null) {
            Text(sermon.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            formatDate(sermon.date)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    HorizontalDivider()

    message.explanation.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.forEach { para ->
        Text(
            para.trim(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 28.sp,
            ),
        )
    }
}

@Composable
private fun EmptyState(onOpenSettings: () -> Unit) {
    Column(Modifier.padding(top = 48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("No messages match your settings.", style = MaterialTheme.typography.titleMedium)
        Text(
            "Try a longer character limit or a different category.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
    }
}

private fun formatDate(iso: String?): String? = iso?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("d MMMM yyyy")) }.getOrNull()
}

private fun nextChangeLabel(refreshHours: Int): String {
    val next = Period.nextChange(refreshHours)
    val time = next.format(DateTimeFormatter.ofPattern("h:mm a"))
    val day = if (next.toLocalDate() == LocalDateTime.now().toLocalDate()) "today" else "tomorrow"
    return "Next message at $time $day"
}

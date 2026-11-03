package app.gracemessages.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gracemessages.data.Message
import app.gracemessages.data.MessageRepository
import app.gracemessages.data.WidgetSettings
import app.gracemessages.data.dataStore
import app.gracemessages.data.toSettings
import app.gracemessages.data.updateSettings
import app.gracemessages.widget.MessageWidget
import app.gracemessages.widget.RefreshScheduler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun GraceTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsFlow = remember { context.dataStore.data.map { it.toSettings() } }
    val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    var message by remember { mutableStateOf<Message?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val dataset = remember { MessageRepository.dataset(context) }

    // Whenever the app comes to the foreground: resolve the current message, sync the widget.
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            message = MessageRepository.current(context)
            loaded = true
            MessageWidget().updateAll(context)
            RefreshScheduler.schedule(context)
        }
        onPauseOrDispose { job.cancel() }
    }

    /** Appearance-only change: just redraw the widget. */
    fun applyLook(t: (WidgetSettings) -> WidgetSettings) = scope.launch {
        context.updateSettings(t)
        MessageWidget().updateAll(context)
    }

    /** Filter change: pick a new message right away. */
    fun applyFilter(t: (WidgetSettings) -> WidgetSettings) = scope.launch {
        context.updateSettings(t)
        message = MessageRepository.next(context)
        MessageWidget().updateAll(context)
    }

    /** Font change alters how much the widget holds; re-pick only if the current one no longer fits. */
    fun applyFont(sp: Int) = scope.launch {
        context.updateSettings { it.copy(fontSizeSp = sp) }
        message = MessageRepository.current(context)
        MessageWidget().updateAll(context)
    }

    fun applyRefresh(hours: Int) = scope.launch {
        context.updateSettings { it.copy(refreshHours = hours) }
        MessageRepository.rekey(context)
        RefreshScheduler.schedule(context)
    }

    val s = settings
    BatteryGate()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (s == null || !loaded) {
            Box(Modifier.fillMaxSize())
        } else if (showSettings) {
            BackHandler { showSettings = false }
            SettingsScreen(
                settings = s,
                dataset = dataset,
                previewText = message?.text,
                onBack = { showSettings = false },
                onLookChange = ::applyLook,
                onFilterChange = ::applyFilter,
                onRefreshChange = ::applyRefresh,
                onFontChange = ::applyFont,
            )
        } else {
            HomeScreen(
                settings = s,
                message = message,
                sermon = message?.let { MessageRepository.sermonFor(context, it) },
                onNext = {
                    scope.launch {
                        message = MessageRepository.next(context)
                        MessageWidget().updateAll(context)
                    }
                },
                onOpenSettings = { showSettings = true },
            )
        }
    }
}

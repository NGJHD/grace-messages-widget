package app.gracemessages.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Setup gate (as in the PSI widget): battery usage must be "Unrestricted", or One UI
 * defers the rotation alarm and the widget changes late. Re-checked on every resume and
 * deliberately unskippable: no "Later"; Back closes the app instead of the dialog.
 *
 * Data Saver is not checked: it only restricts background network, and this app is offline.
 */
object PowerSetup {
    fun isUnrestricted(context: Context): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)

    /** App info is where One UI shows Battery ▸ Unrestricted; needs no special permission. */
    fun openBatterySetting(activity: Activity) {
        val intents = listOf(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )
        for (i in intents) {
            try { activity.startActivity(i); return } catch (_: Exception) { }
        }
        Toast.makeText(activity, "Open Settings ▸ Apps ▸ Grace Messages ▸ Battery ▸ Unrestricted",
            Toast.LENGTH_LONG).show()
    }
}

@Composable
fun BatteryGate() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var restricted by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        restricted = !PowerSetup.isUnrestricted(context)
        onPauseOrDispose { }
    }
    if (!restricted) return

    AlertDialog(
        onDismissRequest = { activity.finish() }, // Back leaves the app; carrying on is not allowed
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { Text("Battery setting delays the widget") },
        text = {
            Text(
                "Grace Messages needs its battery usage set to Unrestricted. Without it, " +
                    "Android delays the widget's change to a new message, sometimes by hours.\n\n" +
                    "Tap Open App info, then Battery ▸ Unrestricted.",
            )
        },
        confirmButton = {
            TextButton(onClick = { PowerSetup.openBatterySetting(activity) }) { Text("Open App info") }
        },
    )
}

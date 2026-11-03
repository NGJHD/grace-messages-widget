package app.gracemessages.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import app.gracemessages.data.MessageRepository
import app.gracemessages.data.Period
import app.gracemessages.data.readSettings
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Message rotation schedule: one tick at each period boundary (midnight, or every N hours).
 *
 * Same design as the PSI widget, which measured the alternatives on a Fold:
 *  - **AlarmManager chain, not WorkManager.** WorkManager jobs of apps that aren't battery
 *    "Unrestricted" get parked by One UI for a long time. Each tick arms the next.
 *  - **Inexact and non-wakeup** (`setWindow(RTC, …)`). An alarm that falls due while the phone
 *    sleeps is held and delivered at the next wake: exactly the "catch up when it wakes"
 *    behaviour we want, without waking the phone to redraw an unseen widget.
 *  - Alarms are wiped by reboot and app update, and a clock / time-zone change moves "midnight",
 *    so [TickReceiver] re-arms on those broadcasts too.
 *  - Backstops: the widget's 30-minute updatePeriodMillis, and the app/widget re-checking the
 *    clock whenever they render ([MessageRepository.current] rolls over a missed period).
 */
object RefreshScheduler {
    private const val TAG = "GM_SCHED"
    private const val WINDOW_MS = 60_000L
    const val ACTION_TICK = "app.gracemessages.action.TICK"

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, TickReceiver::class.java).setAction(ACTION_TICK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Arms the next boundary tick, replacing any pending one. Safe to call repeatedly. */
    suspend fun schedule(context: Context) {
        val hours = context.readSettings().refreshHours
        val at = Period.nextChange(hours).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + 2_000
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.setWindow(AlarmManager.RTC, at, WINDOW_MS, pending(context))
        Log.i(TAG, "next tick at ${java.util.Date(at)}")
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(pending(context))
    }
}

/** Handles the boundary tick plus boot / update / clock changes. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        appScope.launch {
            try {
                MessageRepository.current(app) // rolls over to the new period's message
                MessageWidget().updateAll(app)
                RefreshScheduler.schedule(app)
            } finally {
                result.finish()
            }
        }
    }
}

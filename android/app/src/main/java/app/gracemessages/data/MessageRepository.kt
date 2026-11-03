package app.gracemessages.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.gracemessages.widget.FitLimit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Time periods for message rotation, aligned to local midnight / local hour boundaries. */
object Period {
    fun key(refreshHours: Int, now: LocalDateTime = LocalDateTime.now()): String =
        if (refreshHours <= 0) {
            "d:${now.toLocalDate().toEpochDay()}"
        } else {
            val span = refreshHours * 3600L
            "h$refreshHours:${Math.floorDiv(now.toEpochSecond(ZoneOffset.UTC), span)}"
        }

    fun nextChange(refreshHours: Int, now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
        if (refreshHours <= 0) {
            now.toLocalDate().plusDays(1).atStartOfDay()
        } else {
            val span = refreshHours * 3600L
            val idx = Math.floorDiv(now.toEpochSecond(ZoneOffset.UTC), span)
            LocalDateTime.ofEpochSecond((idx + 1) * span, 0, ZoneOffset.UTC)
        }
}

object MessageRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private const val MAX_HISTORY = 50

    @Volatile private var cached: Dataset? = null

    fun dataset(context: Context): Dataset = cached ?: synchronized(this) {
        cached ?: context.applicationContext.assets.open("messages.json").bufferedReader().use {
            json.decodeFromString(Dataset.serializer(), it.readText())
        }.also { cached = it }
    }

    fun sermonFor(context: Context, message: Message): Sermon? =
        dataset(context).sermons.firstOrNull { it.id == message.sermonId }

    /** The message stored as current, without any period/fit checks (for rendering). */
    fun storedCurrent(context: Context, prefs: Preferences): Message? =
        prefs[Keys.CURRENT_ID]?.let { id -> dataset(context).messages.firstOrNull { it.id == id } }

    /** Messages in the category that fit [limit] characters (null = no limit). */
    fun eligible(ds: Dataset, category: CategoryFilter, limit: Int?): List<Message> =
        ds.messages.filter { category.matches(it.category) && (limit == null || it.text.length <= limit) }

    /**
     * The pool to pick from. If nothing fits the widget (tiny widget / huge font), fall back
     * to the whole category; the widget then shrinks the text to fit.
     */
    private fun pool(ds: Dataset, s: WidgetSettings, limit: Int?): List<Message> =
        eligible(ds, s.category, limit).ifEmpty { eligible(ds, s.category, null) }

    /**
     * The message for the current period. Picks a new one when the period has rolled over,
     * or when the current one no longer fits (widget resized, font changed, category changed).
     */
    suspend fun current(context: Context): Message? = mutex.withLock {
        val prefs = context.dataStore.data.first()
        val s = prefs.toSettings()
        val ds = dataset(context)
        val limit = FitLimit.limit(context, s.fontSizeSp)
        val pool = pool(ds, s, limit)
        val key = Period.key(s.refreshHours)
        val cur = storedCurrent(context, prefs)
        if (cur != null && prefs[Keys.PERIOD_KEY] == key && cur in pool) cur
        else pick(context, pool, key, exclude = cur?.id)
    }

    /** Immediately replaces the current message with a new one (shuffle / category change). */
    suspend fun next(context: Context): Message? = mutex.withLock {
        val prefs = context.dataStore.data.first()
        val s = prefs.toSettings()
        val pool = pool(dataset(context), s, FitLimit.limit(context, s.fontSizeSp))
        pick(context, pool, Period.key(s.refreshHours), exclude = prefs[Keys.CURRENT_ID])
    }

    /**
     * Steps back to the message shown before the current one. Skips earlier messages that no
     * longer match the category / fit the widget. Returns null (and changes nothing) if none.
     */
    suspend fun previous(context: Context): Message? = mutex.withLock {
        val prefs = context.dataStore.data.first()
        val s = prefs.toSettings()
        val ds = dataset(context)
        val pool = pool(ds, s, FitLimit.limit(context, s.fontSizeSp)).associateBy { it.id }
        var result: Message? = null
        context.dataStore.edit { p ->
            val history = readHistory(p).toMutableList()
            while (history.isNotEmpty() && result == null) {
                result = pool[history.removeAt(history.lastIndex)]
            }
            p[Keys.HISTORY] = history.joinToString(",")
            result?.let {
                p[Keys.CURRENT_ID] = it.id
                p[Keys.PERIOD_KEY] = Period.key(s.refreshHours) // stays until the next rotation
            }
        }
        result
    }

    /** Forgets which messages were shown (so all can come round again) and the Previous chain. */
    suspend fun clearHistory(context: Context) = mutex.withLock {
        context.dataStore.edit { p ->
            p.remove(Keys.HISTORY)
            p[Keys.SHOWN] = setOfNotNull(p[Keys.CURRENT_ID])
        }
    }

    fun shownCount(prefs: Preferences): Int = prefs[Keys.SHOWN]?.size ?: 0

    fun hasPrevious(prefs: Preferences): Boolean = !prefs[Keys.HISTORY].isNullOrEmpty()

    private fun readHistory(p: Preferences): List<String> =
        p[Keys.HISTORY]?.split(',')?.filter { it.isNotEmpty() } ?: emptyList()

    /** Keeps the current message but re-aligns it to the (changed) refresh interval. */
    suspend fun rekey(context: Context) = mutex.withLock {
        context.dataStore.edit { it[Keys.PERIOD_KEY] = Period.key(it.toSettings().refreshHours) }
    }

    /** Random pick that avoids repeats until every eligible message has been shown. */
    private suspend fun pick(context: Context, all: List<Message>, key: String, exclude: String?): Message? {
        var result: Message? = null
        context.dataStore.edit { p ->
            var shown = p[Keys.SHOWN] ?: emptySet()
            var pool = all.filter { it.id !in shown && it.id != exclude }
            if (pool.isEmpty()) {
                shown = emptySet()
                pool = all.filter { it.id != exclude }.ifEmpty { all }
            }
            val chosen = pool.randomOrNull()
            if (chosen != null) {
                val old = p[Keys.CURRENT_ID]
                if (old != null && old != chosen.id) {
                    p[Keys.HISTORY] = (readHistory(p) + old).takeLast(MAX_HISTORY).joinToString(",")
                }
                p[Keys.CURRENT_ID] = chosen.id
                p[Keys.SHOWN] = shown + chosen.id
            } else {
                p.remove(Keys.CURRENT_ID)
            }
            p[Keys.PERIOD_KEY] = key
            result = chosen
        }
        return result
    }
}

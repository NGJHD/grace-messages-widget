package app.gracemessages.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    @SerialName("inspirational") INSPIRATIONAL,
    @SerialName("insight") INSIGHT;

    val label: String get() = if (this == INSPIRATIONAL) "Inspirational" else "Insight"
}

@Serializable
data class Sermon(
    val id: String,
    val title: String,
    val date: String? = null,
)

@Serializable
data class Message(
    val id: String,
    val sermonId: String,
    val category: Category,
    /** Short text shown on the widget. */
    val text: String,
    /** One or two paragraphs expounding on the text, separated by blank lines. */
    val explanation: String,
    val scripture: String? = null,
)

@Serializable
data class Dataset(
    val version: Int = 1,
    val sermons: List<Sermon>,
    val messages: List<Message>,
)

enum class CategoryFilter(val label: String) {
    ALL("All"),
    INSPIRATIONAL("Inspirational"),
    INSIGHT("Insights");

    fun matches(c: Category): Boolean = when (this) {
        ALL -> true
        INSPIRATIONAL -> c == Category.INSPIRATIONAL
        INSIGHT -> c == Category.INSIGHT
    }
}

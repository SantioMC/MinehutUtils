package me.santio.minehututils.tags

import me.santio.minehututils.database.models.Tag

enum class SearchAlgorithm(
    val id: String,
    val predicate: Tag.(query: String) -> Boolean,
    val placeholder: String
) {

    REGEX("regex", { query ->
        regex?.let { return@let query.matches(it) } == true
    }, "Enter the regex value (ex: [A-Z]{3})"),

    CONTAINS("contains", { query ->
        searchValue.split('|').any { query.contains(it, ignoreCase = true) }
    }, "Enter the search value (ex: bedrock|mobile)"),

    EXACT("exact", { query ->
        searchValue.split('|').any { query.equals(it, ignoreCase = true) }
    }, "Enter the search value (ex: how do I join?)"),
    DISABLED("disabled", { false }, "Enter a placeholder value (ex: !downtime)"),
    ;

    companion object {
        fun from(id: String): SearchAlgorithm? {
            return SearchAlgorithm.entries.firstOrNull { it.id == id }
        }
    }

}

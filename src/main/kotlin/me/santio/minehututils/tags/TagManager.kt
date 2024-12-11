package me.santio.minehututils.tags

import me.santio.minehututils.database.models.Tag
import me.santio.minehututils.iron

/**
 * Manages the tags registered to the bot, this adds an in-memory cache layer to the database
 * to make searching for tags faster.
 * @author santio
 */
object TagManager {

    private val tags = mutableSetOf<Tag>()

    suspend fun preload() {
        tags.addAll(this.fetchAll())
    }

    suspend fun add(tag: Tag) {
        tags.add(tag)
        iron.prepare(
            "INSERT INTO tags(search_alg, search_value, body, created_at, updated_at, created_by) VALUES (?, ?, ?, ?, ?, ?)",
            tag.searchAlg().id,
            tag.searchValue,
            tag.body,
            tag.createdAt,
            tag.updatedAt,
            tag.createdBy
        )
    }

    suspend fun remove(tag: Tag) {
        tags.remove(tag)
        iron.prepare(
            "DELETE FROM tags WHERE id = ?",
            tag.id
        )
    }

    fun get(id: Int): Tag? {
        return tags.firstOrNull { it.id == id }
    }

    fun all(): List<Tag> {
        return tags.toList()
    }

    suspend fun fetchAll(): List<Tag> {
        return iron.prepare("SELECT * FROM tags").all<Tag>()
    }

    suspend fun save(tag: Tag) {
        tag.updatedAt = System.currentTimeMillis()

        this.tags.removeIf { it.id == tag.id }
        this.tags.add(tag)

        iron.prepare(
            "UPDATE tags SET search_alg = ?, search_value = ?, body = ?, uses = ?, updated_at = ? WHERE id = ?",
            tag.searchAlg().id,
            tag.searchValue,
            tag.body,
            tag.uses,
            tag.updatedAt,
            tag.id
        )
    }

}

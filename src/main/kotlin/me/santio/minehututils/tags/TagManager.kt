package me.santio.minehututils.tags

import com.google.auto.service.AutoService
import me.santio.minehututils.database.DatabaseHook
import me.santio.minehututils.database.models.Tag
import me.santio.minehututils.iron

/**
 * Manages the tags registered to the bot, this adds an in-memory cache layer to the database
 * to make searching for tags faster.
 * @author santio
 */
object TagManager: DatabaseHook {

    private val tags = mutableSetOf<Tag>()

    override suspend fun onHook() {
        tags.addAll(this.fetchAll())
    }

    suspend fun add(tag: Tag) {
        tags.add(tag)

        val id = iron.prepare(
            "INSERT INTO tags(search_alg, search_value, body, created_at, updated_at, created_by) VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            tag.searchAlg().id,
            tag.searchValue,
            tag.body,
            tag.createdAt,
            tag.updatedAt,
            tag.createdBy
        ).single<Int>()

        tag.id = id
    }

    suspend fun remove(tag: Tag) {
        tags.remove(tag)
        iron.prepare(
            "UPDATE tags SET deleted_at = ? WHERE id = ?",
            System.currentTimeMillis(),
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
        return iron.prepare("SELECT * FROM tags WHERE deleted_at IS NULL").all()
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

    suspend fun addUse(tag: Tag) {
        tag.uses++
        this.save(tag)
    }

}

@AutoService(DatabaseHook::class)
class TagManagerProxy: DatabaseHook by TagManager

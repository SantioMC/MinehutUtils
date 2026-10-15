package me.santio.minehututils.tags

import com.google.auto.service.AutoService
import me.santio.minehututils.database.DatabaseHook
import me.santio.minehututils.database.models.Tag
import me.santio.minehututils.iron
import java.util.concurrent.ConcurrentSkipListMap

/**
 * Manages the tags registered to the bot, this adds an in-memory cache layer to the database
 * to make searching for tags faster.
 * @author santio
 */
object TagManager: DatabaseHook {

    // Keyed by id: tags are read on every message while uses are saved from other threads
    private val tags = ConcurrentSkipListMap<Int, Tag>()

    override suspend fun onHook() {
        this.fetchAll().forEach { tags[it.id!!] = it }
    }

    suspend fun add(tag: Tag) {
        val id = iron.prepare(
            "INSERT INTO tags(search_alg, search_value, body, created_at, updated_at, created_by, guild_id) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
            tag.searchAlg().id,
            tag.searchValue,
            tag.body,
            tag.createdAt,
            tag.updatedAt,
            tag.createdBy,
            tag.guildId
        ).single<Int>()

        tag.id = id
        tags[id] = tag
    }

    suspend fun remove(tag: Tag) {
        tag.id?.let { tags.remove(it) }
        iron.prepare(
            "UPDATE tags SET deleted_at = ? WHERE id = ?",
            System.currentTimeMillis(),
            tag.id
        )
    }

    fun get(id: Int): Tag? {
        return tags[id]
    }

    fun getTags(guild: String): List<Tag> {
        return tags.values.filter { it.guildId == guild || it.guildId == null }
    }

    fun find(guild: String, message: String): Tag? {
        return tags.values.firstOrNull { (it.guildId == guild || it.guildId == null) && it.isIncluded(message) }
    }

    suspend fun fetchAll(): List<Tag> {
        return iron.prepare("SELECT * FROM tags WHERE deleted_at IS NULL").all()
    }

    suspend fun save(tag: Tag, updateTime: Boolean = true, updateLastUsed: Boolean = false) {
        if (updateTime) tag.updatedAt = System.currentTimeMillis()
        if (updateLastUsed) tag.lastUsed = System.currentTimeMillis()

        tag.id?.let { this.tags.replace(it, tag) }

        iron.prepare(
            """
            UPDATE tags SET search_alg = ?, 
                search_value = ?, 
                body = ?, 
                uses = ?, 
                updated_at = ?,
                last_used = ?,
                guild_id = ? 
            WHERE id = ?
            """.trimIndent(),

            tag.searchAlg().id,
            tag.searchValue,
            tag.body,
            tag.uses,
            tag.updatedAt,
            tag.lastUsed,
            tag.guildId,
            tag.id
        )
    }

    suspend fun addUse(tag: Tag) {
        synchronized(tag) { tag.uses++ }
        this.save(tag, updateTime = false, updateLastUsed = true)
    }

}

@AutoService(DatabaseHook::class)
class TagManagerProxy: DatabaseHook by TagManager

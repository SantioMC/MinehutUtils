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

    fun getTags(guild: String): List<Tag> {
        return tags.filter { it.guildId == guild || it.guildId == null }
    }

    suspend fun fetchAll(): List<Tag> {
        return iron.prepare("SELECT * FROM tags WHERE deleted_at IS NULL").all()
    }

    suspend fun save(tag: Tag, updateTime: Boolean = true, updateLastUsed: Boolean = false) {
        if (updateTime) tag.updatedAt = System.currentTimeMillis()
        if (updateLastUsed) tag.lastUsed = System.currentTimeMillis()

        this.tags.removeIf { it.id == tag.id }
        this.tags.add(tag)

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
        tag.uses++
        this.save(tag, updateTime = false, updateLastUsed = true)
    }

}

@AutoService(DatabaseHook::class)
class TagManagerProxy: DatabaseHook by TagManager

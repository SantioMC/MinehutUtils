package me.santio.minehututils.boosterpass

import com.google.auto.service.AutoService
import me.santio.minehututils.bot
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.database.DatabaseHook
import me.santio.minehututils.database.models.BoosterPass
import me.santio.minehututils.iron
import net.dv8tion.jda.api.entities.Role

object BoosterPassManager: DatabaseHook {

    private val boosterPasses = mutableListOf<BoosterPass>()

    override suspend fun onHook() {
        boosterPasses.addAll(fetchAll())
    }

    private suspend fun fetchAll(): List<BoosterPass> {
        return iron.prepare("SELECT * FROM booster_pass").all()
    }

    suspend fun getMaxBoosterPasses(guild: String): Int {
        return DatabaseHandler.getSettings(guild).maxBoosterPasses
    }

    suspend fun give(pass: BoosterPass) {
        iron.prepare(
            "INSERT INTO booster_pass (guild_id, giver, receiver, given_at) VALUES (:guildId, :giver, :receiver, :givenAt)",
            pass.bindings()
        )
        boosterPasses.add(pass)
    }

    suspend fun remove(pass: BoosterPass) {
        boosterPasses.removeIf { it.id == pass.id }
        iron.prepare(
            "DELETE FROM booster_pass WHERE id = ?",
            pass.id
        )
    }

    suspend fun revoke(guild: String, giver: String?, receiver: String?): List<BoosterPass> {
        val toRemove = if (giver != null && receiver != null) {
            iron.prepare(
                "DELETE FROM booster_pass WHERE giver = ? AND receiver = ? AND guild_id = ?",
                giver, receiver, guild
            )
            boosterPasses.filter { it.giver == giver && it.receiver == receiver && it.guildId == guild }
        } else if (giver != null) {
            iron.prepare(
                "DELETE FROM booster_pass WHERE giver = ? AND guild_id = ?",
                giver, guild
            )
            boosterPasses.filter { it.giver == giver && it.guildId == guild }
        } else if (receiver != null) {
            iron.prepare(
                "DELETE FROM booster_pass WHERE receiver = ? AND guild_id = ?",
                receiver, guild
            )
            boosterPasses.filter { it.receiver == receiver && it.guildId == guild }
        } else {
            emptyList()
        }
        boosterPasses.removeAll(toRemove)
        return toRemove
    }

    fun getGivenBoosterPasses(guild: String, giver: String): List<BoosterPass> {
        return boosterPasses.filter { it.guildId == guild && it.giver == giver }
    }

    fun getReceivedBoosterPasses(guild: String, receiver: String): List<BoosterPass> {
        return boosterPasses.filter { it.guildId == guild && it.receiver == receiver }
    }

    suspend fun getBoosterPassRole(guild: String): Role? {
        return DatabaseHandler.getSettings(guild).boosterPassRole?.let { roleId ->
            bot.getGuildById(guild)?.getRoleById(roleId)
        }
    }

}

@AutoService(DatabaseHook::class)
class BoosterPassManagerProxy: DatabaseHook by BoosterPassManager

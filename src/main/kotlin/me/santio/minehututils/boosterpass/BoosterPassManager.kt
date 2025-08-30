package me.santio.minehututils.boosterpass

import com.google.auto.service.AutoService
import gg.ingot.iron.bindings.bind
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
        val statement = when {
            giver != null && receiver != null -> "SELECT * FROM booster_pass WHERE giver = :giver AND receiver = :receiver AND guild_id = :guild_id"
            giver != null -> "SELECT * FROM booster_pass WHERE giver = :giver AND guild_id = :guild_id"
            receiver != null -> "SELECT * FROM booster_pass WHERE receiver = :receiver AND guild_id = :guild_id"
            else -> return emptyList()
        }
        iron.prepare(
            statement,
            bind {
                "giver" to giver
                "receiver" to receiver
                "guild_id" to guild
            }
        )

        return boosterPasses.filter {
            ((giver != null && it.giver == giver) || (receiver != null && it.receiver == receiver))
                && it.guildId == guild
        }.onEach { pass ->
            remove(pass)
        }
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

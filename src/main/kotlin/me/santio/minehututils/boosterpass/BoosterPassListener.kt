package me.santio.minehututils.boosterpass

import kotlinx.coroutines.launch
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object BoosterPassListener : ListenerAdapter() {

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val guild = event.guild
        val boosterRole = guild.boostRole ?: return
        if (!event.roles.contains(boosterRole)) return

        checkPasses(guild, event.user)
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        checkPasses(event.guild, event.user)
    }

    private fun checkPasses(guild: Guild, user: User) {
        scope.launch {
            val boosterPassRole = BoosterPassManager.getBoosterPassRole(guild.id) ?: return@launch
            BoosterPassManager.revoke(guild.id, user.id, null).forEach { pass ->
                val receivedPasses = BoosterPassManager.getReceivedBoosterPasses(guild.id, pass.receiver)
                if (receivedPasses.isEmpty()) guild.removeRoleFromMember(UserSnowflake.fromId(pass.receiver), boosterPassRole).queue()
            }
        }
    }

}

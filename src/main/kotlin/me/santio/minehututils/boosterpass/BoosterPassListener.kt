package me.santio.minehututils.boosterpass

import kotlinx.coroutines.launch
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object BoosterPassListener : ListenerAdapter() {

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val guild = event.guild
        val boosterRole = guild.boostRole ?: return
        if (!event.roles.contains(boosterRole)) return

        scope.launch {
            val boosterPassRole = BoosterPassManager.getBoosterPassRole(guild.id) ?: return@launch
            BoosterPassManager.revoke(guild.id, event.member.id, null).forEach { pass ->
                guild.removeRoleFromMember(UserSnowflake.fromId(pass.receiver), boosterPassRole).queue()
            }
        }
    }

}

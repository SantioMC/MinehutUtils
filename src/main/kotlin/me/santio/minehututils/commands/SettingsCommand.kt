package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.annotations.Permission
import me.santio.minehututils.database
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.Permission as JDAPermission

@Command
@Description("Manage specific settings for this discord")
@Permission(JDAPermission.MANAGE_CHANNEL)
class SettingsCommand {

    @Command("advertise")
    @Description("Set the channel to advertise in")
    fun setAdvertise(e: SlashCommandInteractionEvent, channel: TextChannel) {
        val guild = e.guild ?: return
        database.guildSettingsQueries.setAdvertChannel(guild.id, channel.id)

        e.reply(EmbedFactory.success("Successfully set advertise channel to ${channel.asMention}", guild)).queue()
   }

    @Command("marketplace")
    @Description("Set the channel to post listings in")
    fun setMarketplace(e: SlashCommandInteractionEvent, channel: TextChannel) {
        val guild = e.guild ?: return
        database.guildSettingsQueries.setMarketChannel(guild.id, channel.id)

        e.reply(EmbedFactory.success("Successfully set marketplace channel to ${channel.asMention}", guild)).queue()
    }

    @Command("view")
    @Description("View the current settings")
    fun view(e: SlashCommandInteractionEvent) {
        val guild = e.guild ?: return
        val settings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()
            ?: return e.reply(EmbedFactory.error("No settings found", guild)).queue()

        e.reply(EmbedFactory.default("""
        | **Server Settings**
        | 
        | Advertise Channel: ${settings.advertChannel?.let { "<#$it>" } ?: "Not set"}
        | Marketplace Channel: ${settings.marketChannel?.let { "<#$it>" } ?: "Not set"}
        """.trimMargin())).queue()
    }

}
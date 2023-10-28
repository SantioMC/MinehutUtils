package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.annotations.Permission
import me.santio.minehututils.database
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.Logger
import me.santio.minehututils.resolvers.DurationResolver.pretty
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Duration
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

        e.reply(EmbedFactory.success("Successfully set advertise channel to ${channel.asMention}", guild))
            .setEphemeral(true).queue()

        Logger.of(guild).log(
            "The advertisement channel's configuration was changed",
            ":gear: Channel was set to ${channel.asMention} (${channel.id})"
        ).withContext(e).titled("Channel Modified").post()
   }

    @Command("marketplace")
    @Description("Set the channel to post listings in")
    fun setMarketplace(e: SlashCommandInteractionEvent, channel: TextChannel) {
        val guild = e.guild ?: return
        database.guildSettingsQueries.setMarketChannel(guild.id, channel.id)

        e.reply(EmbedFactory.success("Successfully set marketplace channel to ${channel.asMention}", guild))
            .setEphemeral(true).queue()

        Logger.of(guild).log(
            "The marketplace channel's configuration was changed",
            ":gear: Channel was set to ${channel.asMention} (${channel.id})"
        ).withContext(e).titled("Channel Modified").post()
    }

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

    class Cooldown {

        @Description("Set the cooldown for advertising")
        fun advertise(e: SlashCommandInteractionEvent, cooldown: Duration) {
            val guild = e.guild ?: return
            database.guildSettingsQueries.setAdvertCooldown(guild.id, cooldown.toSeconds())

            e.reply(EmbedFactory.success("Successfully set the advertisement cooldown to ${cooldown.pretty()}", guild))
                .setEphemeral(true).queue()

            Logger.of(guild).log(
                "The advertisement channel's configuration was changed",
                ":gear: Cooldown was set to ${cooldown.pretty()}"
            ).withContext(e).titled("Channel Modified").post()
        }

        @Description("Set the cooldown for posting listings")
        fun marketplace(e: SlashCommandInteractionEvent, cooldown: Duration) {
            val guild = e.guild ?: return
            database.guildSettingsQueries.setMarketCooldown(guild.id, cooldown.toSeconds())

            e.reply(EmbedFactory.success("Successfully set the marketplace cooldown to ${cooldown.pretty()}", guild))
                .setEphemeral(true).queue()

            Logger.of(guild).log(
                "The marketplace channel's configuration was changed",
                ":gear: Cooldown was set to ${cooldown.pretty()}"
            ).withContext(e).titled("Channel Modified").post()
        }

    }

}
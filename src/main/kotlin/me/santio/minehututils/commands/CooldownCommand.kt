package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.annotations.Permission
import me.santio.minehututils.database
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.Logger
import me.santio.minehututils.minehut.api.ServerModel
import me.santio.minehututils.resolvers.DurationResolver
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Duration
import kotlin.math.floor
import net.dv8tion.jda.api.Permission as JDAPermission

@Command
class CooldownCommand {

    @Description("View your channel cooldowns")
    fun info(e: SlashCommandInteractionEvent) {
        val guild = e.guild ?: return

        val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

        if (
            guildSettings == null
            || (guildSettings.advertChannel == null
                && guildSettings.marketChannel == null)
        ) {
            e.reply(EmbedFactory.error("This guild has no configured channels.")).queue()
            return
        }

        val now = floor(System.currentTimeMillis() / 1000.0).toInt()
        val cooldowns = database.cooldownQueries.getCooldowns(e.user.id).executeAsList()

        var body = """
        | :clipboard: Channel Cooldowns
        | 
        | **Server Cooldowns**
        ${guildSettings.advertChannel?.let { 
            if (guildSettings.advertCooldown == 0L) return@let ""
            val duration = Duration.ofSeconds(guildSettings.advertCooldown)
            "| <#$it>: ${DurationResolver.pretty(duration)}"
        } ?: ""}
        ${guildSettings.marketChannel?.let {
            if (guildSettings.marketCooldown == 0L) return@let ""
            val duration = Duration.ofSeconds(guildSettings.marketCooldown)
            "| <#$it>: ${DurationResolver.pretty(duration)}\n"
        } ?: ""}
        | **Your Cooldowns**
        | 
        """.trimMargin()

        for (category in listOf("advert", "market-offer", "market-request")) {
            val cooldown = cooldowns.firstOrNull { it.category == category } ?: continue

            val channel = when (category) {
                "advert" -> guildSettings.advertChannel
                "market-offer", "market-request" -> guildSettings.marketChannel
                else -> null
            } ?: continue

            val extra = when (category) {
                "market-offer" -> " (Offering)"
                "market-request" -> " (Requesting)"
                else -> ""
            }

            var duration = Duration.ofSeconds(cooldown.time_end - now)
            if (duration.toSeconds() <= 0) duration = Duration.ofSeconds(1)
            body += "<#$channel>$extra: ${DurationResolver.pretty(duration)}\n"
        }

        if (cooldowns.isEmpty()) {
            body += "You have no active cooldowns"
        }

        e.reply(EmbedFactory.default(body))
            .setEphemeral(true).queue()
    }

    @Permission(JDAPermission.MESSAGE_MANAGE)
    class Set {

        @Description("Set a user's marketplace cooldown for offers")
        fun offers(e: SlashCommandInteractionEvent, user: Member, duration: Duration) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.setCooldown(
                user.id,
                "market-offer",
                duration.toSeconds() + floor(System.currentTimeMillis() / 1000.0).toInt()
            )

            e.reply(EmbedFactory.success("Set marketplace cooldown (offers) for ${user.asMention} to ${DurationResolver.pretty(duration)}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${user.asMention} *(${user.id})* was modified",
                    ":gear: Cooldown set to ${DurationResolver.pretty(duration)}",
                    ":label: Cooldown Changed: Marketplace (offers)"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }

        @Description("Set a user's marketplace cooldown for requests")
        fun requests(e: SlashCommandInteractionEvent, user: Member, duration: Duration) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.setCooldown(
                user.id,
                "market-request",
                duration.toSeconds() + floor(System.currentTimeMillis() / 1000.0).toInt()
            )

            e.reply(EmbedFactory.success("Set marketplace cooldown (requests) for ${user.asMention} to ${DurationResolver.pretty(duration)}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${user.asMention} *(${user.id})* was modified",
                    ":gear: Cooldown set to ${DurationResolver.pretty(duration)}",
                    ":label: Cooldown Changed: Marketplace (requests)"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }

        @Description("Set a user's cooldown for advertisements")
        fun advertise(e: SlashCommandInteractionEvent, user: Member, duration: Duration) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.setCooldown(
                user.id,
                "advert",
                duration.toSeconds() + floor(System.currentTimeMillis() / 1000.0).toInt()
            )

            e.reply(EmbedFactory.success("Set advertisement cooldown for ${user.asMention} to ${DurationResolver.pretty(duration)}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${user.asMention} *(${user.id})* was modified",
                    ":gear: Cooldown set to ${DurationResolver.pretty(duration)}",
                    ":label: Cooldown Changed: Advertisements"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }
        
        @Description("Set a server's cooldown for advertisements")
        fun server(e: SlashCommandInteractionEvent, server: ServerModel, duration: Duration) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.setCooldown(
                server.id,
                "advert",
                duration.toSeconds() + floor(System.currentTimeMillis() / 1000.0).toInt()
            )

            e.reply(EmbedFactory.success("Set advertisement cooldown for ${server.name} to ${DurationResolver.pretty(duration)}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${server.name} *(${server.id})* was modified",
                    ":gear: Cooldown set to ${DurationResolver.pretty(duration)}",
                    ":label: Cooldown Changed: Advertisements (Server-Based)"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }

    }

    class Reset {

        @Description("Reset the user's marketplace cooldown for offers")
        fun offers(e: SlashCommandInteractionEvent, user: Member) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.resetCooldown(
                user.id,
                "market-offer"
            )

            e.reply(EmbedFactory.success("Reset the marketplace cooldown (offers) for ${user.asMention}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${user.asMention} *(${user.id})* was modified",
                    ":gear: Cooldown was reset",
                    ":label: Cooldown Changed: Marketplace (offers)"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }


        @Description("Reset the user's marketplace cooldown for requests")
        fun requests(e: SlashCommandInteractionEvent, user: Member) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.resetCooldown(
                user.id,
                "market-request"
            )

            e.reply(EmbedFactory.success("Reset the marketplace cooldown (requests) for ${user.asMention}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${user.asMention} *(${user.id})* was modified",
                    ":gear: Cooldown was reset",
                    ":label: Cooldown Changed: Marketplace (requests)"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }

        @Description("Reset the user's cooldown for advertisements")
        fun advertise(e: SlashCommandInteractionEvent, user: Member) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.resetCooldown(
                user.id,
                "advert"
            )

            e.reply(EmbedFactory.success("Reset the advertisement cooldown for ${user.asMention}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${user.asMention} *(${user.id})* was modified",
                    ":gear: Cooldown was reset",
                    ":label: Cooldown Changed: Advertisements"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }

        @Description("Reset a server's cooldown for advertisements")
        fun server(e: SlashCommandInteractionEvent, server: ServerModel) {
            val guild = e.guild ?: return
            val guildSettings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()

            if (guildSettings?.advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.")).queue()
                return
            }

            database.cooldownQueries.resetCooldown(
                server.id,
                "advert"
            )

            e.reply(EmbedFactory.success("Reset the advertisement cooldown for ${server.name}"))
                .setEphemeral(true).queue()

            Logger.of(guild)
                .log(
                    "Cooldown for ${server.name} *(${server.id})* was modified",
                    ":gear: Cooldown was reset",
                    ":label: Cooldown Changed: Advertisements (Server-Based)"
                )
                .titled("Cooldown Changed")
                .withContext(e)
                .post()
        }

    }

}

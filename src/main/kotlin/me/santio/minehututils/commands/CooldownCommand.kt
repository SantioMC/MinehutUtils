package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.annotations.Permission
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownRegistry
import me.santio.minehututils.data.Channel
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.Logger
import me.santio.minehututils.minehut.api.ServerModel
import me.santio.minehututils.resolvers.DurationResolver
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Duration
import net.dv8tion.jda.api.Permission as JDAPermission

@Command
class CooldownCommand {

    @Description("View your channel cooldowns")
    fun info(e: SlashCommandInteractionEvent) {
        val guild = e.guild ?: return

        val channels = Channel.entries.map { it.get() }
        val anyConfigured = channels.any { it != null }

        if (!anyConfigured) {
            e.reply(EmbedFactory.error("This guild has no configured channels.", guild)).queue()
            return
        }

        val cooldowns = CooldownRegistry.cooldowns[e.user.id] ?: emptySet()
        val defaultCooldowns = Cooldown.Kind.entries.distinctBy { it.channel }

        var body = """
        | :clipboard: Channel Cooldowns
        | 
        | **Server Cooldowns**
        ${defaultCooldowns.joinToString("\n") {
            val name = it.channel.display
            val duration = DurationResolver.pretty(it.getDuration())

            "| $name: $duration"
        }}
        | 
        | **Your Cooldowns**
        ${cooldowns.joinToString("\n") {
            val name = it.key.display
            val timestamp = it.expiresAt()
            
            "| $name: <t:$timestamp:R>"
        }}
        """.trimMargin()

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
            val marketChannel = Channel.MARKETPLACE.get()

            if (marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.", guild)).queue()
                return
            }

            CooldownRegistry.setCooldown(user.id, Cooldown.Kind.MARKET_OFFER, duration)
            e.reply(EmbedFactory.success("Set marketplace cooldown (offers) for ${user.asMention} to ${
                DurationResolver.pretty(duration)
            }", guild)).setEphemeral(true).queue()

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
            val marketChannel = Channel.MARKETPLACE.get()

            if (marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.", guild)).queue()
                return
            }

            CooldownRegistry.setCooldown(user.id, Cooldown.Kind.MARKET_REQUEST, duration)
            e.reply(EmbedFactory.success("Set marketplace cooldown (requests) for ${user.asMention} to ${DurationResolver.pretty(duration)}", guild))
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
            val advertChannel = Channel.ADVERTISEMENTS.get()

            if (advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured advertisement channel.", guild)).queue()
                return
            }

            CooldownRegistry.setCooldown(user.id, Cooldown.Kind.ADVERTISEMENT_USER, duration)
            e.reply(EmbedFactory.success("Set advertisement cooldown for ${user.asMention} to ${
                DurationResolver.pretty(duration)
            }", guild)).setEphemeral(true).queue()

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
            val advertChannel = Channel.ADVERTISEMENTS.get()

            if (advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured advertisement channel.", guild)).queue()
                return
            }

            CooldownRegistry.setCooldown(server.id, Cooldown.Kind.ADVERTISEMENT_SERVER, duration)
            e.reply(EmbedFactory.success("Set advertisement cooldown for ${server.name} to ${
                DurationResolver.pretty(duration)
            }", guild)).setEphemeral(true).queue()

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

    @Permission(JDAPermission.MESSAGE_MANAGE)
    class Reset {

        @Description("Reset the user's marketplace cooldown for offers")
        fun offers(e: SlashCommandInteractionEvent, user: Member) {
            val guild = e.guild ?: return
            val marketChannel = Channel.MARKETPLACE.get()

            if (marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.", guild)).queue()
                return
            }

            CooldownRegistry.resetCooldown(user.id, Cooldown.Kind.MARKET_OFFER)
            e.reply(EmbedFactory.success("Reset the marketplace cooldown (offers) for ${user.asMention}", guild))
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
            val marketChannel = Channel.MARKETPLACE.get()

            if (marketChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured market channel.", guild)).queue()
                return
            }

            CooldownRegistry.resetCooldown(user.id, Cooldown.Kind.MARKET_REQUEST)
            e.reply(EmbedFactory.success("Reset the marketplace cooldown (requests) for ${user.asMention}", guild))
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
            val advertChannel = Channel.ADVERTISEMENTS.get()

            if (advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured advertisement channel.", guild)).queue()
                return
            }

            CooldownRegistry.resetCooldown(user.id, Cooldown.Kind.ADVERTISEMENT_USER)
            e.reply(EmbedFactory.success("Reset the advertisement cooldown for ${user.asMention}", guild))
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
            val advertChannel = Channel.ADVERTISEMENTS.get()

            if (advertChannel == null) {
                e.reply(EmbedFactory.error("This guild has no configured advertisement channel.", guild)).queue()
                return
            }

            CooldownRegistry.resetCooldown(server.id, Cooldown.Kind.ADVERTISEMENT_SERVER)
            e.reply(EmbedFactory.success("Reset the advertisement cooldown for ${server.name}", guild))
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

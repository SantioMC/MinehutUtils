package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.gui.button.Button
import me.santio.coffee.jda.gui.showModal
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownRegistry
import me.santio.minehututils.data.Channel
import me.santio.minehututils.ext.reply
import me.santio.minehututils.ext.sendMessage
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.modals.AdvertisementModal
import me.santio.minehututils.resolvers.AutoModResolver
import me.santio.minehututils.resolvers.EmojiResolver
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import java.time.Duration

@Command
@Description("Advertise your Minehut server")
class AdvertiseCommand {
    
    fun advertise(e: SlashCommandInteractionEvent) {
        val guild = e.guild ?: return
        val member = e.member ?: return

        val advertChannel = Channel.ADVERTISEMENTS.get() ?: run {
            e.reply(EmbedFactory.error("This server hasn't setup a advertisement channel!", e.guild!!)).setEphemeral(true).queue()
            return
        }
        
        val channel = guild.getTextChannelById(advertChannel) ?: run {
            e.reply(EmbedFactory.error("Failed to find the advertisement channel, was it deleted?", guild)).setEphemeral(true).queue()
            return
        }

        // Check if they're on cooldown
        val cooldown = CooldownRegistry.getCooldown(member.id, Cooldown.Kind.ADVERTISEMENT_USER)
        if (cooldown != null) {
            e.reply(EmbedFactory.error(
                "You are currently on cooldown, try again in ${cooldown.remaining()}",
                guild
            )).setEphemeral(true).queue()
            return
        }
        
        e.showModal(AdvertisementModal::class.java) { m, event ->
            // Run description through automod
            AutoModResolver.parse(guild, m.description, member, e).thenAccept { passes ->
                if (!passes) {
                    event.reply(EmbedFactory.error(
                        "Your message was caught by the filter and the request has been discarded.",
                        guild
                    )).setEphemeral(true).queue()
                    return@thenAccept
                }

                val serverCooldown = CooldownRegistry.getCooldown(m.server.id, Cooldown.Kind.ADVERTISEMENT_SERVER)
                if (serverCooldown != null) {
                    e.reply(EmbedFactory.error(
                        "This server is currently on cooldown, try again in ${serverCooldown.remaining()}",
                        guild
                    )).setEphemeral(true).queue()
                    return@thenAccept
                }

                if (m.description.lines().size > 25) {
                    event.reply(EmbedFactory.error(
                        "Your description is too long, please shorten it to 25 lines or less.",
                        event.guild!!
                    )).setEphemeral(true).queue()
                    return@thenAccept
                }

                CooldownRegistry.setCooldown(member.id, Cooldown.Kind.ADVERTISEMENT_USER)
                CooldownRegistry.setCooldown(m.server.id, Cooldown.Kind.ADVERTISEMENT_SERVER)

                channel.sendMessage(EmbedFactory.default("""
                | ${EmojiResolver.find(guild, "minehut")?.formatted ?: ""} **${m.server.name}**
                | 
                | ${m.description}
                | 
                | ${EmojiResolver.find(guild, "java")?.formatted ?: ""} Play at `${m.server.nameLower}.minehut.gg`
                | ${EmojiResolver.find(guild, "bedrock")?.formatted ?: ""} Bedrock: `${m.server.nameLower}.bedrock.minehut.gg`
                """.trimMargin())
                    .setFooter(
                        "Advertisement posted by ${member.user.name} (${member.id})",
                        member.avatarUrl
                    ))
                    .setContent("Advertisement made by ${member.asMention}")
                    .addActionRow(
                        Button.create("Learn More", ButtonStyle.SECONDARY, expiry = Duration.ofMinutes(30)) {
                            it.event.reply(
                                ServerCommand.buildServerEmbed(guild, m.server)
                            ).setEphemeral(true).queue()
                        }.build()
                    )
                    .queue( {
                        event.reply(EmbedFactory.default("""
                        | **Success** ${EmojiResolver.yes(guild)?.formatted ?: ""}
                        | 
                        | You have successfully posted your server advertisement!
                        | ${
                            EmojiResolver.find(guild, "pickaxe", Emoji.fromUnicode("⛏️"))?.formatted ?: ""
                        } `${m.server.nameLower}.minehut.gg`
                        | 
                        | Check it out! :point_right: ${it.jumpUrl}
                        """.trimMargin())).setEphemeral(true).queue()
                    }, {
                        event.reply(EmbedFactory.exception("Failed to post advertisement, see below:", guild, it) {})
                    })
            }
        }
    }
    
}
package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.gui.button.Button
import me.santio.coffee.jda.gui.showModal
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.database
import me.santio.minehututils.ext.*
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
        val settings = database.guildSettingsQueries.from(guild.id).executeAsOneOrNull()
        
        if (settings?.advertChannel == null) {
            e.reply(EmbedFactory.error("This server hasn't setup server advertisements!", guild)).setEphemeral(true).queue()
            return
        }
        
        val channel = guild.getTextChannelById(settings.advertChannel) ?: run {
            e.reply(EmbedFactory.error("Failed to find the advertisement channel, was it deleted?", guild)).setEphemeral(true).queue()
            return
        }

        // Check if they're on cooldown
        if (Cooldown.ADVERTISE_USER.get(member)?.isElapsed() == false) {
            e.reply(EmbedFactory.error(
                "You are currently on cooldown, try again ${Cooldown.ADVERTISE_USER.get(member)?.toTime() ?: "0 seconds"}",
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

                Cooldown.ADVERTISE_USER.set(member, Duration.ofSeconds(settings.advertCooldown).toCooldown())
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
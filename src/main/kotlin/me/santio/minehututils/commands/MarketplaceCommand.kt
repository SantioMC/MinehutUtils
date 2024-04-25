package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.gui.dropdown.Dropdown
import me.santio.coffee.jda.gui.showModal
import me.santio.minehututils.bot
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownRegistry
import me.santio.minehututils.data.Channel
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.modals.MarketplaceModal
import me.santio.minehututils.resolvers.AutoModResolver
import me.santio.minehututils.resolvers.EmojiResolver
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction
import net.dv8tion.jda.api.utils.MarkdownSanitizer

@Command
@Description("Request or offer services")
class MarketplaceCommand {

    fun marketplace(e: SlashCommandInteractionEvent) {
        if (e.guild == null) return

        val marketChannel = Channel.MARKETPLACE.get() ?: run {
            e.reply(EmbedFactory.error("This server hasn't setup a server marketplace!", e.guild!!)).setEphemeral(true).queue()
            return
        }

        lateinit var message: InteractionHook
        e.reply("Are you looking to offer or request?")
            .addActionRow(Dropdown.from(
                SelectOption.of("Offering", "offer")
                    .withEmoji(Emoji.fromFormatted("\uD83D\uDCE2"))
                    .withDescription("Offer your services to the community"),
                SelectOption.of("Requesting", "request")
                    .withEmoji(Emoji.fromFormatted("📝"))
                    .withDescription("Request services, staff, or code from the community")
            ) {

                val kind = when (it.selected.first()) {
                    "offer" -> Cooldown.Kind.MARKET_OFFER
                    "request" -> Cooldown.Kind.MARKET_REQUEST
                    else -> error("Failed to get cooldown kind")
                }

                val cooldown = CooldownRegistry.getCooldown(e.user.id, kind)

                if (cooldown != null) {
                    it.event.reply(EmbedFactory.error(
                        "You are currently on cooldown, try again in ${cooldown.remaining()}",
                        e.guild!!
                    )).setEphemeral(true).queue()
                    return@from
                }

                handlePosting(it.event as StringSelectInteraction, marketChannel)
            }.onExpire {
                message.editOriginal("This interaction has been automatically closed").queue()
            }.build())
            .setEphemeral(true)
            .queue {
                message = it
            }
    }

    private fun handlePosting(e: StringSelectInteraction, channelId: String) {
        val value = e.selectedOptions.first().value.lowercase()
        e.showModal(MarketplaceModal::class.java) { modal, event ->

            // Run everything through auto-mod
            AutoModResolver.parse(event.guild!!, event.member!!, e, modal.title, modal.description).thenAccept { passes ->
                if (!passes) {
                    event.reply(EmbedFactory.error(
                        "Your message was caught by the filter and the request has been discarded.",
                        event.guild!!
                    )).setEphemeral(true).queue()
                    return@thenAccept
                }
            }

            if (modal.description.lines().size > 25) {
                event.reply(EmbedFactory.error(
                    "Your description is too long, please shorten it to 25 lines or less.",
                    event.guild!!
                )).setEphemeral(true).queue()
                return@showModal
            }

            postListing(value, modal, event, channelId)
        }
    }

    private fun postListing(type: String, modal: MarketplaceModal, event: ModalInteractionEvent, channelId: String) {

        val channel = bot.getTextChannelById(channelId) ?: run {
            event.reply(EmbedFactory.error("Failed to find the marketplace channel, was it deleted?", event.guild!!)).setEphemeral(true).queue()
            return
        }

        val embed = EmbedFactory.default("""
            | ${EmojiResolver.find(event.guild, "minehut")?.formatted ?: ""} **${type}ing | ${
                MarkdownSanitizer.sanitize(modal.title.replace("*", ""))
                    .ifEmpty { "Untitled" }
            }**
            |
            | ${modal.description}
            | ​
            """.trimMargin()) {

            it.setFooter(
                "Listing made by ${event.user.name} (${event.user.id})",
                event.user.avatarUrl
            )
        }

        channel.sendMessage("Listing posted by ${event.user.asMention}")
            .addEmbeds(embed.build())
            .queue {
                event.reply(EmbedFactory.default("""
                    | **Success** ${EmojiResolver.yes(event.guild)?.formatted ?: ""}
                    | 
                    | You have successfully posted your marketplace listing!
                    | Check it out! :point_right: ${it.jumpUrl}
                    """.trimMargin())).setEphemeral(true).queue()
            }

        val cooldown = when (type) {
            "offer" -> Cooldown.Kind.MARKET_OFFER
            "request" -> Cooldown.Kind.MARKET_REQUEST
            else -> error("Failed to get cooldown kind: $type")
        }

        CooldownRegistry.setCooldown(event.member!!.id, cooldown)
    }

}
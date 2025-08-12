package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import me.santio.minehututils.boosterpass.BoosterPassManager
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.database.models.BoosterPass
import me.santio.minehututils.ext.toTime
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class BoosterPassCommand: SlashCommand {

    override fun getData(): CommandData {
        return Command("boosterpass", "Manage booster passes") {
            setContexts(InteractionContextType.GUILD)

            addSubcommands(
                Subcommand("give", "Give a booster pass to a user") {
                    addOption(OptionType.USER, "user", "The user to give the booster pass to", true)
                },
                Subcommand("remove", "Remove a booster pass from a user") {
                    addOption(OptionType.USER, "user", "The user to remove the booster pass from", true)
                },
                Subcommand("view", "View information about a booster pass") {
                    addOption(OptionType.USER, "user", "The user to view the booster pass for", true)
                }
            )

            addSubcommandGroups(
                SubcommandGroup("staff", "Staff commands for managing booster passes") {
                    addSubcommands(
                        Subcommand("give", "Give a booster pass to a user as staff") {
                            addOptions(
                                Option<User>("user", "The user to give the booster pass to", true)
                            )
                        },
                        Subcommand("revoke", "Revoke a booster pass from a user by the giver or receiver") {
                            addOptions(
                                Option<User>("giver", "The user who gave the booster pass", false),
                                Option<User>("receiver", "The user who received the booster pass", false)
                            )
                        }
                    )
                }
            )
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            error("This command can only be used in a server.")
        }

        val boosterRole = BoosterPassManager.getBoosterPassRole(event.guild!!.id)
        val boosterPassRole = BoosterPassManager.getBoosterPassRole(event.guild!!.id)
        if (boosterRole == null || boosterPassRole == null) {
            error("Booster roles are not set up in this server. Please contact a server administrator.")
        }

        when (event.subcommandGroup) {
            "staff" -> when (event.subcommandName) {
                "give" -> staffGiveBoosterPass(event, boosterRole)
                "revoke" -> staffRevokeBoosterPass(event, boosterPassRole)
                else -> error("Unknown subcommand")
            }
            else -> when (event.subcommandName) {
                "give" -> giveBoosterPass(event, boosterPassRole)
                "remove" -> removeBoosterPass(event, boosterPassRole)
                "view" -> viewBoosterPass(event)
                else -> error("Unknown subcommand")
            }
        }
    }

    private suspend fun giveBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val user = event.getOption("user")?.asMember ?: error("User option is required")

        val guildId = event.guild!!.id
        val userId = event.user.id

        val givenPasses = BoosterPassManager.getGivenBoosterPasses(guildId, userId)
        val maxPasses = BoosterPassManager.getMaxBoosterPasses(guildId)
        if (givenPasses.size >= maxPasses) {
            return event.reply("You can only give a maximum of ${BoosterPassManager.getMaxBoosterPasses(event.guild!!.id)} booster passes.").setEphemeral(true).queue()
        }

        BoosterPassManager.give(BoosterPass(
            guildId = guildId,
            giver = userId,
            receiver = user.id,
            givenAt = System.currentTimeMillis()
        ))
        user.roles.add(boosterPassRole)

        val amountLeft = maxPasses - givenPasses.size - 1
        event.reply("✅ Gave ${user.asMention} a booster pass ($amountLeft left)").setEphemeral(true).queue()
    }

    private suspend fun removeBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val user = event.getOption("user")?.asMember ?: error("User option is required")

        val givenPasses = BoosterPassManager.getGivenBoosterPasses(event.guild!!.id, event.user.id)
        val pass = givenPasses.firstOrNull { it.receiver == user.id }
        if (pass == null) {
            return event.reply("You have not given a booster pass to ${user.asMention}").setEphemeral(true).queue()
        }

        BoosterPassManager.remove(pass)
        user.roles.remove(boosterPassRole)

        val amountLeft = BoosterPassManager.getMaxBoosterPasses(event.guild!!.id) - givenPasses.size + 1
        event.reply("✅ Removed booster pass from ${user.asMention} ($amountLeft left)").setEphemeral(true).queue()
    }

    private fun viewBoosterPass(event: SlashCommandInteractionEvent) {
        val user = event.getOption("user")?.asUser ?: return

        val givenPasses = BoosterPassManager.getGivenBoosterPasses(event.guild!!.id, user.id)
        val receivedPasses = BoosterPassManager.getReceivedBoosterPasses(event.guild!!.id, user.id)
        if (givenPasses.isEmpty() && receivedPasses.isEmpty()) {
            return event.reply("No booster passes found for ${user.asTag}").setEphemeral(true).queue()
        }

        val passInfoEmbed = EmbedFactory.default("Booster Pass Information for ${user.asTag}")
            .addField("Received From",
                if (receivedPasses.isEmpty()) "None" else receivedPasses.joinToString("\n") { "• <@${it.giver}>" },
                false
            )
            .addField("Given To",
                if (givenPasses.isEmpty()) "None" else givenPasses.joinToString("\n") { "• <@${it.receiver}>" },
                false
            )
            .build()
        event.replyEmbeds(passInfoEmbed).setEphemeral(true).queue()
    }

    private suspend fun staffGiveBoosterPass(event: SlashCommandInteractionEvent, boosterRole: Role) {
        val user = event.getOption("user")?.asMember ?: error("User option is required")

        BoosterPassManager.give(BoosterPass(
            guildId = event.guild!!.id,
            giver = event.user.id,
            receiver = user.id
        ))
        user.roles.add(boosterRole)

        event.reply("✅ Gave ${user.asMention} a booster pass").setEphemeral(true).queue()
    }

    private suspend fun staffRevokeBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val giver = event.getOption("giver")?.asMember
        val receiver = event.getOption("receiver")?.asMember

        if (giver == null && receiver == null) {
            return event.reply("You must specify either a giver or a receiver.").setEphemeral(true).queue()
        }

        val revokedPasses = BoosterPassManager.revoke(event.guild!!.id, giver?.id, receiver?.id)
        revokedPasses.forEach { pass ->
            event.guild!!.removeRoleFromMember(UserSnowflake.fromId(pass.receiver), boosterPassRole).queue()
        }

        val embed = if (revokedPasses.isEmpty()) {
            EmbedFactory.error("No booster passes found for the specified giver or receiver.", event.guild!!).build()
        } else {
            EmbedFactory.default("Revoked Booster Passes")
                .addField("Revoked Passes", revokedPasses.joinToString("\n") { "• From <@${it.giver}> to <@${it.receiver}> (${it.givenAt.toTime()})" }, false)
                .build()
        }

        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

}

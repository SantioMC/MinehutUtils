package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Subcommand
import me.santio.minehututils.boosterpass.BoosterPassManager
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.database.models.BoosterPass
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.GuildLogger
import net.dv8tion.jda.api.entities.Role
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
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            error("This command can only be used in a server.")
        }

        val guild = event.guild!!
        val boosterRole = guild.boostRole
        val boosterPassRole = BoosterPassManager.getBoosterPassRole(guild.id)
        if (boosterRole == null || boosterPassRole == null) {
            error("Booster roles are not set up in this server. Please contact a server administrator.")
        }

        when (event.subcommandName) {
            "give", "remove" -> {
                if (!event.member!!.roles.contains(boosterRole)) {
                    error("You must boost the server to use this command!")
                }

                if (event.subcommandName == "give") giveBoosterPass(event, boosterPassRole)
                else removeBoosterPass(event, boosterPassRole)
            }
            "view" -> viewBoosterPass(event)
            else -> error("Unknown subcommand")
        }
    }

    private suspend fun giveBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val user = event.getOption("user")?.asMember ?: error("User option is required")

        val guild = event.guild!!
        val guildId = guild.id
        val userId = event.user.id

        val givenPasses = BoosterPassManager.getGivenBoosterPasses(guildId, userId)
        val maxPasses = BoosterPassManager.getMaxBoosterPasses(guildId)
        if (givenPasses.size >= maxPasses) {
            return event.replyEmbeds(EmbedFactory.error("You can only give a maximum of $maxPasses booster passes.", event.guild).build()).setEphemeral(true).queue()
        }

        BoosterPassManager.give(BoosterPass(
            guildId = guildId,
            giver = userId,
            receiver = user.id
        ))
        guild.addRoleToMember(user, boosterPassRole).queue()

        GuildLogger.of(guild).log(
            "Booster pass given to ${user.asMention} by ${event.user.asMention}"
        ).withContext(event)

        val amountLeft = maxPasses - givenPasses.size - 1
        event.replyEmbeds(EmbedFactory.success("Gave ${user.asMention} a booster pass ($amountLeft left)", event.guild).build()).setEphemeral(true).queue()
    }

    private suspend fun removeBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val user = event.getOption("user")?.asMember ?: error("User option is required")

        val guild = event.guild!!
        val givenPasses = BoosterPassManager.getGivenBoosterPasses(guild.id, event.user.id)
        val pass = givenPasses.firstOrNull { it.receiver == user.id }
        if (pass == null) {
            return event.replyEmbeds(EmbedFactory.error("You have not given a booster pass to ${user.asMention}", event.guild).build()).setEphemeral(true).queue()
        }

        BoosterPassManager.remove(pass)

        val receivedPasses = BoosterPassManager.getReceivedBoosterPasses(guild.id, user.id)
        if (receivedPasses.isEmpty()) guild.removeRoleFromMember(user, boosterPassRole).queue()

        GuildLogger.of(guild).log(
            "Booster pass removed from ${user.asMention} by ${event.user.asMention}"
        ).withContext(event)

        val amountLeft = BoosterPassManager.getMaxBoosterPasses(event.guild!!.id) - givenPasses.size + 1
        event.replyEmbeds(EmbedFactory.success("Removed booster pass from ${user.asMention} ($amountLeft left)", event.guild).build()).setEphemeral(true).queue()
    }

    private fun viewBoosterPass(event: SlashCommandInteractionEvent) {
        val user = event.getOption("user")?.asUser ?: return

        val givenPasses = BoosterPassManager.getGivenBoosterPasses(event.guild!!.id, user.id)
        val receivedPasses = BoosterPassManager.getReceivedBoosterPasses(event.guild!!.id, user.id)
        if (givenPasses.isEmpty() && receivedPasses.isEmpty()) {
            return event.replyEmbeds(EmbedFactory.error("No booster passes found for ${user.asTag}", event.guild).build()).setEphemeral(true).queue()
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

}

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
import me.santio.minehututils.logger.GuildLogger
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class StaffCommand: SlashCommand {

    override fun getData(): CommandData {
        return Command("staff", "Staff commands for Minehut Utils") {
            setContexts(InteractionContextType.GUILD)
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

            addSubcommandGroups(
                SubcommandGroup("boosterpass", "Booster pass management commands for staff") {
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

        val guild = event.guild!!
        val boosterRole = guild.boostRole
        val boosterPassRole = BoosterPassManager.getBoosterPassRole(guild.id)
        if (boosterRole == null || boosterPassRole == null) {
            error("Booster roles are not set up in this server. Please contact a server administrator.")
        }

        when (event.subcommandGroup) {
            "boosterpass" -> when (event.subcommandName) {
                "give" -> staffGiveBoosterPass(event, boosterPassRole)
                "revoke" -> staffRevokeBoosterPass(event, boosterPassRole)
                else -> error("Unknown subcommand: ${event.subcommandName}")
            }
        }
    }

    private suspend fun staffGiveBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val user = event.getOption("user")?.asMember ?: error("User option is required")

        val guild = event.guild!!
        val existingPass = BoosterPassManager.getGivenBoosterPasses(guild.id, event.user.id).any { it.receiver == user.id }
        if (existingPass) {
            error("${user.asMention} already has a booster pass from you.")
        }

        BoosterPassManager.give(BoosterPass(
            guildId = guild.id,
            giver = event.user.id,
            receiver = user.id
        ))
        guild.addRoleToMember(UserSnowflake.fromId(user.id), boosterPassRole).queue()

        GuildLogger.of(guild).log(
            "Booster pass given to ${user.asMention} by ${event.user.asMention}"
        ).withContext(event)

        event.replyEmbeds(EmbedFactory.success("Gave ${user.asMention} a booster pass", event.guild).build()).setEphemeral(true).queue()
    }

    private suspend fun staffRevokeBoosterPass(event: SlashCommandInteractionEvent, boosterPassRole: Role) {
        val giver = event.getOption("giver")?.asMember
        val receiver = event.getOption("receiver")?.asMember

        if (giver == null && receiver == null) {
            return event.replyEmbeds(EmbedFactory.error("You must specify either a giver or a receiver.", event.guild).build()).setEphemeral(true).queue()
        }

        val guild = event.guild!!
        val revokedPasses = BoosterPassManager.revoke(guild.id, giver?.id, receiver?.id)
        revokedPasses.forEach { pass ->
            val receivedPasses = BoosterPassManager.getReceivedBoosterPasses(guild.id, pass.receiver)
            if (receivedPasses.isEmpty()) guild.removeRoleFromMember(UserSnowflake.fromId(pass.receiver), boosterPassRole).queue()
        }

        GuildLogger.of(guild).log(
            "${revokedPasses.size} booster pass${if (revokedPasses.size > 1) "es" else ""} revoked by ${event.user.asMention}",
            *revokedPasses.map { "From <@${it.giver}> to <@${it.receiver}> (${it.givenAt.toTime()})" }.toTypedArray()
        ).withContext(event)

        val embed = if (revokedPasses.isEmpty()) {
            EmbedFactory.error("No booster passes found for the specified giver or receiver.", guild).build()
        } else {
            EmbedFactory.default("Revoked Booster Passes")
                .addField("Revoked Passes", revokedPasses.joinToString("\n") { "• From <@${it.giver}> to <@${it.receiver}> (${it.givenAt.toTime()})" }, false)
                .build()
        }

        event.replyEmbeds(embed).setEphemeral(true).queue()
    }

}

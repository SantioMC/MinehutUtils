package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Subcommand
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.GuildLogger
import me.santio.minehututils.resolvers.DurationResolver
import me.santio.minehututils.resolvers.DurationResolver.discord
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import kotlin.time.toKotlinDuration

@AutoService(SlashCommand::class)
class CooldownCommand: SlashCommand {
    override fun getData(): CommandData {
        return Command("cooldown", "View and manage cooldowns") {
            isGuildOnly = true

            addSubcommands(
                Subcommand("view", "View your cooldowns"),
                Subcommand("reset", "Reset the cooldown for a user") {
                    defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)
                    addOption(OptionType.USER, "user", "The user to reset the cooldown for", true)
                },
                Subcommand("set", "Set the cooldown for a user") {
                    defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)
                    addOption(OptionType.USER, "user", "The user to set the cooldown for", true)
                    addOption(OptionType.STRING, "type", "The type of cooldown to set", true, true)
                    addOption(OptionType.STRING, "duration", "The duration of the cooldown", true)
                },
                Subcommand("flush", "Remove all saved cooldowns for all users") {
                    defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)
                }
            )
        }
    }

    override suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        return Cooldown.entries.map { Choice(it.humanName, it.humanName) }
    }

    private fun viewCooldown(event: SlashCommandInteractionEvent) {
        var body = ":clipboard: Cooldowns\n\n"

        for (cooldown in Cooldown.entries) {
            val userCooldown = CooldownManager.get(event.user.id, cooldown)

            body += if (userCooldown == null) "${cooldown.humanName}: None\n"
            else "${cooldown.humanName}: ${userCooldown.timeLeft().discord(true)}\n"
        }

        event.replyEmbeds(EmbedFactory.default(body).build()).setEphemeral(true).queue()
    }

    private fun resetCooldown(event: SlashCommandInteractionEvent) {
        val user = event.getOption("user")?.asUser ?: error("User not provided")
        CooldownManager.clear(user.id)

        GuildLogger.of(event.guild!!).log(
            "Cooldown for ${user.asMention} (${user.id}) was reset by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*"
        ).withContext(event).titled("Cooldowns Reset").post()

        event.replyEmbeds(EmbedFactory.success("The cooldown for ${user.asMention} was reset", event.guild!!).build())
            .queue()
    }

    private fun setCooldown(event: SlashCommandInteractionEvent) {
        val user = event.getOption("user")?.asUser ?: error("User not provided")
        val type = event.getOption("type")?.asString ?: error("Type not provided")
        val length = event.getOption("duration")?.asString ?: error("Duration not provided")

        val cooldown = Cooldown.entries.firstOrNull { it.humanName.equals(type, true) }
            ?: error("Invalid cooldown type provided")

        val duration = DurationResolver.from(length) ?: error("Invalid duration provided")
        CooldownManager.set(user.id, cooldown, duration.toKotlinDuration())

        GuildLogger.of(event.guild!!).log(
            "Cooldown for ${user.asMention} was set to `${DurationResolver.pretty(duration)}` by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":gear: Cooldown set to `${DurationResolver.pretty(duration)}`",
            ":label: Cooldown Changed: `${cooldown.humanName}`"
        ).withContext(event).titled("Cooldown Modified").post()

        event.replyEmbeds(EmbedFactory.success("The cooldown for ${user.asMention} was set to `${DurationResolver.pretty(duration)}`", event.guild!!).build())
            .queue()
    }

    private fun flushCooldowns(event: SlashCommandInteractionEvent) {
        CooldownManager.reset()

        GuildLogger.of(event.guild!!).log(
            "All cooldowns were reset by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
        ).withContext(event).titled("Cooldowns Flushed").post()

        event.replyEmbeds(EmbedFactory.success("All cooldowns were reset", event.guild!!).build()).queue()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        when(event.subcommandName) {
            "view" -> viewCooldown(event)
            "reset" -> resetCooldown(event)
            "set" -> setCooldown(event)
            "flush" -> flushCooldowns(event)
            else -> error("Subcommand not found", true)
        }
    }

}

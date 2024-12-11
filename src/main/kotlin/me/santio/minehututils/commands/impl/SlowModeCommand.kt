package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.components.getOption
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.GuildLogger
import me.santio.minehututils.resolvers.DurationResolver
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.attribute.ISlowmodeChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@AutoService(SlashCommand::class)
class SlowModeCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("slowmode", "Change how fast users can speak in the channel") {
            isGuildOnly = true
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

            addOptions(
                Option<String>("duration", "How long users need to wait between messages", true)
            )
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val time = event.getOption<String>("duration") ?: error("No duration was provided")
        val duration = DurationResolver.from(time) ?: error("Invalid duration provided")

        val seconds = min(duration.toSeconds().toInt(), ISlowmodeChannel.MAX_SLOWMODE)
        event.channel.asTextChannel().manager.setSlowmode(seconds).queue()
        val setDuration = seconds.seconds

        GuildLogger.of(event.guild!!).log(
            "Slowmode modified in ${event.channel.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":stopwatch: Slowmode was set to `${DurationResolver.pretty(setDuration)}`",
            ":package: Channel modified was ${event.channel.asMention} (${event.channel.id})",
        ).withContext(event).titled("Slowmode Modified").post()

        event.replyEmbeds(
            EmbedFactory.success(
                "Set the slowmode for ${event.channel.asMention} to `${DurationResolver.pretty(setDuration)}`",
                event.guild
            ).build()
        ).queue()
    }

}

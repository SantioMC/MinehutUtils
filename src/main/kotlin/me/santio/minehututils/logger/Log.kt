package me.santio.minehututils.logger

import me.santio.minehututils.ext.sendMessage
import me.santio.minehututils.factories.ButtonFactory
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.MarkdownSanitizer

/**
 * Represents a singular log that will be posted to the log channel
 */
@Suppress("unused")
data class Log(
    private var guildLogger: GuildLogger,
    var message: String
) {

    private var context = ""
    private var title = ""
    private var fileUpload: FileUpload? = null

    /**
     * Attach context to the log, this will be provided at
     * the end of the log message.
     * @param channel The channel to attach
     * @return The log
     */
    fun withContext(channel: GuildChannel): Log {

        context += """
        | Channel ID: ${channel.id} *(${channel.asMention})*
        """.trimMargin() + "\n"

        return this
    }

    /**
     * Attach context to the log, this will be provided at
     * the end of the log message.
     * @param user The user to attach
     * @return The log
     */
    fun withContext(user: User): Log {
        context += """
        | User ID: ${user.id} *(${user.asMention})*
        """.trimMargin() + "\n"

        return this
    }

    /**
     * Attach context to the log, this will be provided at
     * the end of the log message.
     * @param message The message to attach
     * @return The log
     */
    fun withContext(message: Message): Log {
        context += """
        | Channel ID: ${message.channel.id} *(${message.channel.asMention})*
        | Message ID: ${message.id} ${ButtonFactory.textButton("JUMP", message.jumpUrl)}
        | User ID: ${message.author.id} *(${message.author.asMention} - ${message.author.name})*
        """.trimMargin() + "\n"

        return this
    }

    /**
     * Attach context to the log, this will be provided at
     * the end of the log message.
     * @param event The SlashCommandInteractionEvent (command event) to attach
     * @return The log
     */
    fun withContext(event: Interaction): Log {
        if (event.channel != null) context += "Channel ID: ${event.channel!!.id} *(${event.channel!!.asMention})*\n"
        context += "User ID: ${event.user.id} *(${event.user.asMention} - ${event.user.name})*\n"
        if (event is SlashCommandInteractionEvent) context += "Full Command: `${MarkdownSanitizer.escape(event.commandString)}`\n"

        return this
    }

    /**
     * Sets a file to upload with the log
     * @param fileUpload The file upload to attach
     * @return The log
     */
    fun withFile(fileUpload: FileUpload): Log {
        this.fileUpload = fileUpload
        return this
    }

    /**
     * Sets the title of the log
     * @param title The title to set
     * @return The log
     */
    fun titled(title: String): Log {
        this.title = title
        return this
    }

    /**
     * Builds the embed from the data provided
     * @return A embed builder
     */
    private fun build(): EmbedBuilder {
        return EmbedFactory.default(
            """
        | :clipboard: **Log ${if (title.isNotEmpty()) "| $title" else ""}**
        | 
        | $message
        ${if (context.isNotEmpty()) "\n$context" else ""}
        """.trimMargin()
        )
    }

    /**
     * Posts the log to the log channel, if the log channel isn't defined or found
     * this will do nothing.
     * @return The log
     */
    fun post(): Log {
        if (!guildLogger.isEnabled()) return this
        val message = guildLogger.channel?.sendMessage(build())
        if (fileUpload != null) {
            message?.addFiles(fileUpload)
        }
        message?.queue()
        return this
    }

}

package me.santio.minehututils.tags

import kotlinx.coroutines.CancellationException
import me.santio.minehututils.coroutines.await
import me.santio.minehututils.database.models.Tag
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule

object TagSender {

    private val logger = LoggerFactory.getLogger(TagSender::class.java)
    private val timer = Timer()
    private val recentlySent = ConcurrentHashMap.newKeySet<String>()

    fun claim(channelId: String, tag: Tag): Boolean {
        val id = "$channelId-${tag.id}"
        if (!recentlySent.add(id)) return false

        timer.schedule(10000) { recentlySent.remove(id) }
        return true
    }

    fun canReply(channel: GuildMessageChannel): Boolean {
        return channel.canTalk() && channel.guild.selfMember.hasPermission(
            channel,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_HISTORY
        )
    }

    suspend fun reply(message: Message, tag: Tag, requestedBy: User, ping: Boolean): Boolean {
        val sent = runCatching { message.reply(tag.toMessage(requestedBy)).mentionRepliedUser(ping).await() }
            .onFailure {
                if (it is CancellationException) throw it
                logger.warn("Failed to send tag {} in {}: {}", tag.id, message.channel.id, it.toString())
            }.isSuccess

        if (sent) TagManager.addUse(tag)
        return sent
    }

}

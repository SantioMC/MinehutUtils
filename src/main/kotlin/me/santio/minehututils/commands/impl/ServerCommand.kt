package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.coroutines.await
import me.santio.minehututils.ext.formatted
import me.santio.minehututils.ext.toTime
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.Minehut.server
import me.santio.minehututils.minehut.PlayerHistory
import me.santio.minehututils.resolvers.EmojiResolver
import me.santio.minehututils.resolvers.MOTDRenderer
import me.santio.minehututils.resolvers.MOTDResolver
import me.santio.minehututils.utils.Colors
import me.santio.minehututils.utils.Images
import me.santio.minehututils.utils.TextHelper.titlecase
import me.santio.sdk.minehut.models.ListedServer
import me.santio.sdk.minehut.models.Server
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

@AutoService(SlashCommand::class)
class ServerCommand : SlashCommand {

    private companion object {
        const val ICON_SIZE = 128
        const val ICON_CANVAS = 192
        const val EMOJI_SIZE = 128
        const val DEFAULT_ACCENT = 0x488AFF
        const val MAX_IMAGE_SIZE = 512
        const val EMOJI_RETRY = 60 * 60 * 1000L
        const val ICON_RETRY = 60 * 60 * 1000L
        const val MAX_NETWORK_SERVERS = 25
        val EMOJI_INVALID = Regex("[^A-Za-z0-9_]")
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Gets a readable plan name, such as "Pro (monthly, 6GB)"
     */
    private fun planName(server: Server, listed: ListedServer?, servers: List<ListedServer>): String {
        val raw = server.serverPlan ?: return "Unknown"
        val active = server.activeServerPlan

        val name = when {
            raw.startsWith("CUSTOM") -> "Custom"
            active != null && active != raw -> active.removePrefix("YEARLY ")
            else -> return raw.replace("_", " ").titlecase()
        }

        val details = listOfNotNull(
            "monthly".takeIf { raw.startsWith("MONTHLY_") },
            "yearly".takeIf { raw.startsWith("YEARLY_") },
            ram(server, listed, servers)?.let { "${it}GB" },
            "24/7".takeIf { raw != "EXTERNAL" && name != "Ultimate" && listed?.staticInfo?.alwaysOnline == true }
        )

        return if (details.isEmpty()) name else "$name (${details.joinToString(", ")})"
    }

    /**
     * The RAM of a plan in GB. Servers missing from the server list use the RAM of a listed server on the same plan,
     * counting monthly and yearly plans as the same.
     */
    private fun ram(server: Server, listed: ListedServer?, servers: List<ListedServer>): Int? {
        val plan = server.activeServerPlan?.removePrefix("YEARLY ")
        val ram = listed?.staticInfo?.planRam
            ?: plan?.let {
                servers.firstNotNullOfOrNull { other ->
                    other.staticInfo?.takeIf { info -> info.serverPlan?.removePrefix("YEARLY ") == plan }?.planRam
                }
            }

        return ram?.let { it / 1024 }
    }

    private class ItemIcon(val thumbnail: ByteArray, val color: Int?)

    private val itemIcons = ConcurrentHashMap<String, ItemIcon>()
    private val itemEmojis = ConcurrentHashMap<String, ApplicationEmoji>()
    private val emojiRetryAt = ConcurrentHashMap<String, Long>()
    private val iconRetryAt = ConcurrentHashMap<String, Long>()
    private val emojiLock = Mutex()

    @Volatile
    private var emojisLoaded = false

    private fun favicon(server: Server): BufferedImage? {
        val data = server.serverListFavicon?.substringAfter("base64,", "")?.takeIf { it.isNotEmpty() } ?: return null
        val bytes = runCatching { Base64.getDecoder().decode(data) }.getOrNull() ?: return null
        return Images.decode(bytes, MAX_IMAGE_SIZE)
    }

    private suspend fun downloadItemIcon(name: String): BufferedImage? {
        val response = runCatching { Minehut.httpClient.get("${Minehut.ICON_URL}/$name.png") }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
            ?.takeIf { it.status.isSuccess() } ?: return null

        return Images.decode(response.readRawBytes(), MAX_IMAGE_SIZE)
    }

    /**
     * Gets the thumbnail and accent color for a server's item icon, such as OAK_SIGN. Failed downloads are retried
     * after an hour.
     */
    private suspend fun itemIcon(name: String): ItemIcon? {
        itemIcons[name]?.let { return it }
        if (System.currentTimeMillis() < (iconRetryAt[name] ?: 0)) return null

        val icon = downloadItemIcon(name)?.let { ItemIcon(thumbnail(it), Colors.dominant(it)) }
        if (icon != null) itemIcons[name] = icon else iconRetryAt[name] = System.currentTimeMillis() + ICON_RETRY
        return icon
    }

    private fun thumbnail(icon: BufferedImage): ByteArray {
        val margin = (ICON_CANVAS - ICON_SIZE) / 2
        return Images.square(ICON_CANVAS) { Images.drawFitted(this, icon, margin, margin, ICON_SIZE) }
    }

    /**
     * Gets the application emoji of a server's item icon, creating it the first time it's needed. Failures are
     * retried after an hour.
     */
    private suspend fun itemEmoji(jda: JDA, name: String): ApplicationEmoji? {
        val emojiName = "mh_${name.replace(EMOJI_INVALID, "")}".take(32)
        itemEmojis[emojiName]?.let { return it }
        if (System.currentTimeMillis() < (emojiRetryAt[emojiName] ?: 0)) return null

        return runCatching {
            emojiLock.withLock {
                if (!emojisLoaded) {
                    jda.retrieveApplicationEmojis().await().forEach { itemEmojis[it.name] = it }
                    emojisLoaded = true
                }

                itemEmojis[emojiName] ?: downloadItemIcon(name)?.let { icon ->
                    jda.createApplicationEmoji(emojiName, Icon.from(Images.square(EMOJI_SIZE) {
                        Images.drawFitted(this, icon, 0, 0, EMOJI_SIZE)
                    })).await().also { itemEmojis[emojiName] = it }
                }
            }
        }.onFailure {
            if (it is CancellationException) throw it
            logger.warn("Failed to create the {} emoji", emojiName, it)
        }.getOrNull().also {
            if (it == null) emojiRetryAt[emojiName] = System.currentTimeMillis() + EMOJI_RETRY
        }
    }

    private fun period(minutes: Int): String = when {
        minutes >= 60 * 24 * 7 -> "Last 7 days"
        minutes >= 60 * 48 -> "Last ${minutes / (60 * 24)} days"
        minutes >= 60 * 2 -> "Last ${minutes / 60} hours"
        else -> "Last hour"
    }

    private fun networkName(name: String, current: Boolean): String {
        val text = MarkdownSanitizer.escape(name)
        return if (current) "__**$text**__" else text
    }

    /**
     * Lists the proxy and up to [MAX_NETWORK_SERVERS] sub servers of the network a server belongs to. A sub server
     * only finds its proxy while the proxy is in the server list.
     */
    private suspend fun network(server: Server, servers: List<ListedServer>): String? {
        val parent = if (server.connectedServers.isNullOrEmpty()) {
            servers.firstOrNull { server.id in it.staticInfo?.connectedServers.orEmpty() } ?: return null
        } else null

        val proxy = parent?.name ?: server.name ?: return null
        val ids = parent?.staticInfo?.connectedServers ?: server.connectedServers.orEmpty()

        val names = coroutineScope {
            ids.take(MAX_NETWORK_SERVERS).map { id ->
                async { (servers.firstOrNull { it.staticInfo?.id == id }?.name ?: Minehut.serverName(id))?.let { id to it } }
            }.awaitAll()
        }

        val subServers = names.filterNotNull().map { (id, name) -> networkName(name, id == server.id) }
            .plus(listOfNotNull((ids.size - MAX_NETWORK_SERVERS).takeIf { it > 0 }?.let { "${it.formatted()} more" }))

        val network = networkName(proxy, parent == null)
        return if (subServers.isEmpty()) network else "$network (${subServers.joinToString(", ")})"
    }

    private fun activityLine(emoji: String, stats: PlayerHistory.Stats): String {
        return "$emoji **${period(stats.minutes)}:** ${stats.average.formatted()} avg · ${stats.peak.formatted()} peak"
    }

    /**
     * Shows a motd as an ANSI code block when it can't be rendered, or as plain text when the ANSI version doesn't
     * fit in the space left on the card
     */
    private fun motdFallback(motd: String, text: String, space: Int): TextDisplay {
        val ansi = runCatching { MOTDResolver.toAnsi(motd.replace("`", "'")) }.getOrNull()
        if (ansi != null && ansi.length + 11 <= space) return TextDisplay.of("```ansi\n$ansi```")

        val plain = text.replace("`", "'").take((space - 7).coerceAtLeast(0)).dropLastWhile { it.isHighSurrogate() }
        return TextDisplay.of("```\n$plain```")
    }

    suspend fun buildServerCard(guild: Guild, server: Server): Container {
        val motd = MOTDResolver.toComponent(server.motd.orEmpty())
        val motdText = MOTDResolver.clean(motd)

        val motdImage = if (motdText.isBlank()) null else runCatching {
            MediaGallery.of(
                MediaGalleryItem.fromFile(FileUpload.fromData(MOTDRenderer.render(motd), "motd.png"))
                    .withDescription(motdText.take(MediaGalleryItem.MAX_DESCRIPTION_LENGTH).dropLastWhile { it.isHighSurrogate() })
            )
        }
        val servers = Minehut.servers()
        val listed = servers.firstOrNull { it.staticInfo?.id == server.id }

        val name = server.name ?: "Unknown"
        val link = "https://minehut.com/servers/${name.lowercase()}"
        val players = server.playerCount ?: 0

        val status = when {
            server.suspended == true -> "⚠️ **Suspended**"
            server.online == true -> "${EmojiResolver.find(guild, "yes", EmojiResolver.checkmark())!!.formatted} **Online** with " +
                "${players.formatted()} ${if (players == 1) "player" else "players"}"
            else -> "${EmojiResolver.find(guild, "no", EmojiResolver.crossmark())!!.formatted} **Offline**"
        }
        val since = server.lastOnline
            ?.takeIf { server.suspended != true && server.online != true }
            ?.let { "-# Last online ${it.toTime()}" }

        val network = network(server, servers)
        val notJoinable = "🚫 Not directly joinable"
            .takeIf { listed?.connectable == false && network == null && server.suspended != true }

        val owner = listed?.author?.let { MarkdownSanitizer.escape(it) }
        val ownerRank = listed?.authorRank
            ?.takeIf { listed.author != null && it != "DEFAULT" }
            ?.let { Minehut.rankName(it) ?: it.replace("_", " ").titlecase() }

        val external = server.serverPlan == "EXTERNAL"

        val categoryNames = Minehut.categoryNames()
        val categories = server.categories.orEmpty()
            .map { categoryNames[it] ?: it.titlecase() }
            .joinToString(", ")
            .ifEmpty { null }

        val iconName = server.icon ?: "OAK_SIGN"
        val emoji = itemEmoji(guild.jda, iconName)
        val favicon = favicon(server)
        val faviconColor = favicon?.let { Colors.dominant(it) }
        val item = if (favicon == null || faviconColor == null) itemIcon(iconName) else null
        val accent = faviconColor ?: item?.color ?: Colors.dominant(motd) ?: DEFAULT_ACCENT

        val thumbnail = (favicon?.let { thumbnail(it) } ?: item?.thumbnail)
            ?.let { Thumbnail.fromFile(FileUpload.fromData(it, "icon.png")) }
            ?: Thumbnail.fromUrl("${Minehut.ICON_URL}/$iconName.png")

        val header = listOfNotNull(
            "## ${emoji?.let { "${it.formatted} " } ?: ""}[${MarkdownSanitizer.escape(name)}]($link)",
            status,
            since,
            notJoinable
        ).joinToString("\n")

        val count = listed?.playerData?.playerCount?.takeIf { it > 0 }
        val rank = count?.let { 1 + servers.count { (it.playerData?.playerCount ?: 0) > count } }

        // The category the server ranks best in, among servers that share it
        val categoryRank = count?.let { listedPlayers ->
            server.categories.orEmpty().map { category ->
                category to 1 + servers.count {
                    category in it.allCategories.orEmpty() && (it.playerData?.playerCount ?: 0) > listedPlayers
                }
            }.minByOrNull { it.second }
        }?.let { (category, position) -> " · #${position.formatted()} in ${categoryNames[category] ?: category.titlecase()}" } ?: ""

        val emptySince = listed?.playerData
            ?.takeIf { it.playerCount == 0 && players == 0 }
            ?.timeNoPlayers?.takeIf { it > 0 }

        val day = server.id?.let { PlayerHistory.stats(it, 24) }
        val week = server.id?.let { PlayerHistory.stats(it, 24 * 7) }
            ?.takeIf { it.minutes > (day?.minutes ?: 0) && it.peak > 0 }
        val busiest = server.id?.let { PlayerHistory.busiestHour(it) }
            ?.let { System.currentTimeMillis() / 1000 / 86400 * 86400 + it * 3600L }

        val activity = listOfNotNull(
            "-# **PLAYER ACTIVITY**",
            rank?.let { "🏆 **Rank:** #${it.formatted()} of ${servers.size.formatted()}$categoryRank" },
            emptySince?.let { "💤 **Empty Since:** ${it.toTime()}" },
            day?.takeIf { it.peak > 0 }?.let { activityLine("📅", it) },
            week?.let { activityLine("🗓️", it) },
            busiest?.let { "⏰ **Busiest Hours:** <t:$it:t> – <t:${it + 3600}:t>" },
            "👥 **Total Joins:** ${server.joins?.formatted() ?: "0"}"
        ).joinToString("\n")

        val details = listOfNotNull(
            "-# **SERVER DETAILS**",
            owner?.let { "👑 **Owner:** $it${ownerRank?.let { rank -> " ($rank)" } ?: ""}" },
            "🎂 **Created:** ${server.creation?.let { "<t:${it / 1000}:D>" } ?: "Unknown"}",
            "💎 **Plan:** ${planName(server, listed, servers)}",
            server.serverVersionType?.takeIf { !external }?.let { "⚙️ **Server Type:** ${it.titlecase()}" },
            network?.let { "🌐 **Server Network:** $it" },
            listed?.versionMin?.let { "🏷️ **Minimum Version:** $it+" },
            categories?.let { "📁 **Categories:** $it" }
        ).joinToString("\n")

        val footer = "-# Server ID: ${server.id ?: "Unknown"}"
        val motdSection = motdImage?.getOrElse {
            logger.warn("Failed to render the motd of {}", server.name, it)
            val space = Message.MAX_CONTENT_LENGTH_COMPONENT_V2 - header.length - activity.length - details.length - footer.length
            motdFallback(server.motd.orEmpty(), motdText, space)
        }

        return Container.of(listOfNotNull(
            Section.of(thumbnail, TextDisplay.of(header)),
            motdSection,
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of(activity),
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of(details),
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of(footer)
        )).withAccentColor(accent)
    }

    override fun getData(): CommandData {
        return Command("server", "Get information about a server") {
            setContexts(InteractionContextType.GUILD)
            addOption(OptionType.STRING, "server", "The server to get information about", true, true)
        }
    }

    override suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val query = event.focusedOption.value
        return Minehut.cachedServers().asSequence()
            .mapNotNull { it.name }
            .filter { it.contains(query, ignoreCase = true) }
            .take(OptionData.MAX_CHOICES)
            .map { Command.Choice(it, it) }
            .toList()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val serverId = event.getOption("server")?.asString ?: error("Server not provided")

        val guild = event.guild ?: return

        // The Minehut API can take longer than Discord's 3 second window to respond
        event.deferReply(true).await()

        val data = server(serverId) ?: run {
            event.hook.editOriginalEmbeds(EmbedFactory.error("Failed to find the server", guild).build()).queue()
            return
        }

        event.hook.editOriginalComponents(buildServerCard(guild, data)).useComponentsV2().await()
    }

}

package me.santio.minehututils.resolvers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.logger.GuildLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.automod.AutoModResponse
import net.dv8tion.jda.api.entities.automod.AutoModRule
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * Provides an easy way to run messages and queries through auto mod, this will try it's best to mimic the exact
 * functionality as automod does however perfect functionality is not guaranteed.
 */
object AutoModResolver {

    private val logger = LoggerFactory.getLogger(AutoModResolver::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mentionRegex = Regex("<@!?\\d{17,20}>")

    /**
     * Parses a query and checks if it passes all auto mod rules
     * @param guild The guild containing the auto mod rules
     * @param member The member who sent the query
     * @param interaction The interaction that triggered the query
     * @param query The query to evaluate
     * @return A completable future which will return whether the query passes, if the bot is missing permissions to
     * access the auto mod rules (MANAGE_GUILD), then we'll default to the query passing to avoid any functionality breaking.
     */
    fun parse(guild: Guild, query: String, member: Member, interaction: Interaction): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        try {
            guild.retrieveAutoModRules().queue({ rules ->
                for (rule in rules.filter { it.isEnabled }) {
                    var passes = rule.passes(query, interaction.guildChannel, member)

                    val mentions = mentionRegex.findAll(query).count()
                    if (rule.mentionLimit > 0 && mentions > rule.mentionLimit) passes = false

                    if (!passes) {
                        for (action in rule.actions) {
                            // A failed action (such as missing permissions) must not change the verdict
                            runCatching {
                                when (action.type) {
                                    AutoModResponse.Type.SEND_ALERT_MESSAGE -> coroutineScope.launch(exceptionHandler) {
                                        sendLog(query, guild, interaction, rule)
                                    }
                                    AutoModResponse.Type.TIMEOUT -> {
                                        action.timeoutDuration?.let {
                                            member.timeoutFor(it).queue(null) { error ->
                                                logger.warn("Failed to time out member {} in guild {}: {}", member.id, guild.id, error.toString())
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                            }.onFailure { logger.warn("Failed to run the auto mod action {}: {}", action.type, it.toString()) }
                        }

                        future.complete(false)
                        return@queue
                    }
                }

                future.complete(true)
            }, {
                future.complete(true)
            })
        } catch (e: InsufficientPermissionException) {
            // No permissions
            future.complete(true)
        }

        return future
    }

    /**
     * Parses multiple queries and checks if it all pass each auto mod rules
     * @param guild The guild containing the auto mod rules
     * @param member The member who sent the query
     * @param interaction The interaction that triggered the query
     * @param queries The queries to evaluate
     * @return A completable future which will return whether the query passes, if the bot is missing permissions to
     * access the auto mod rules (MANAGE_GUILD), then we'll default to the query passing to avoid any functionality breaking.
     */
    fun parse(
        guild: Guild,
        member: Member,
        interaction: Interaction,
        vararg queries: String
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        val remaining = AtomicInteger(queries.size)

        for (query in queries) {
            parse(guild, query, member, interaction).whenComplete { passes, _ ->
                if (passes == false) {
                    future.complete(false)
                    return@whenComplete
                }

                if (remaining.decrementAndGet() == 0) {
                    future.complete(true)
                }
            }
        }

        return future
    }

    private fun AutoModRule.passes(query: String, channel: GuildChannel, member: Member): Boolean {
        if (channel in this.exemptChannels) return true
        if (member.roles.any { it in this.exemptRoles }) return true

        // Discord uses Rust regex syntax, translate named groups and skip any other pattern Java can't compile
        val regex = this.filteredRegex.mapNotNull {
            runCatching { Regex(it.replace("(?P<", "(?<"), RegexOption.IGNORE_CASE) }.getOrNull()
        }
        val blockedWords = this.filteredKeywords
        val allowedWords = this.allowlist

        val words = query.split(" ").map { it.lowercase().trim() }
        for (word in words) {
            if (word in blockedWords && word !in allowedWords) {
                return false
            }
        }

        return regex
            .map { it.findAll(query) }
            .map {
                it.filter { m -> m.value !in allowedWords }.count() > 0
            }
            .all { !it }
    }

    private suspend fun sendLog(query: String, guild: Guild, interaction: Interaction, rule: AutoModRule) {
        GuildLogger.of(guild).log(
            "Message was caught failing to pass auto moderation rules",
            ":identification_card: User: ${interaction.member?.asMention} *(${interaction.user.name} - ${interaction.user.id})*",
            ":pencil: Query: `${MarkdownSanitizer.escape(query)}`",
            ":scroll: Rule Broken: ${rule.name}"
        ).withContext(interaction).titled("Auto Moderation").post()
    }

}

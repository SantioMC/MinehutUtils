package me.santio.minehututils.skript

import com.google.gson.annotations.SerializedName
import dev.minn.jda.ktx.messages.EmbedBuilder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

data class SkriptSyntax(
    val id: Int,
    val creator: String,
    val title: String,
    val description: String,
    @SerializedName("syntax_pattern")
    val syntaxPattern: String,
    @SerializedName("compatible_addon_version")
    val compatibleAddonVersion: String,
    @SerializedName("compatible_minecraft_version")
    val compatibleMinecraftVersion: String,
    @SerializedName("syntax_type")
    val syntaxType: String,
    @SerializedName("get_syntax_type_css_class")
    val syntaxTypeCssClass: String,
    @SerializedName("required_plugins")
    val requiredPlugins: List<SyntaxRequiredPlugin>,
    val addon: String,
    @SerializedName("type_usage")
    val typeUsage: String,
    @SerializedName("return_type")
    val returnType: String,
    @SerializedName("event_values")
    val eventValues: String,
    @SerializedName("json_id")
    val jsonId: String,
    @SerializedName("event_cancellable")
    val eventCancellable: Boolean,
    val link: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val entries: String
) {
    fun send(interaction: SlashCommandInteractionEvent) {
        val embed = EmbedBuilder()
        embed.setColor(0x488aff)
        embed.setTitle(title)
        embed.setDescription(description)
        embed.setUrl(link)
        embed.addField("Syntax", syntaxPattern, true)
        if (addon != "Skript") {
            embed.addField("Addon", addon, true)
        }
        interaction.replyEmbeds(embed.build()).queue()
    }
}

data class SyntaxRequiredPlugin(
    val name: String,
    val link: String
)

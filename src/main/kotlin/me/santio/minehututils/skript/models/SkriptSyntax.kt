package me.santio.minehututils.skript.models

import com.google.gson.annotations.SerializedName

data class SkriptSyntax(
    val id: Long,
    val creator: String,
    var title: String,
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
)

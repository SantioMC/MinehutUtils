package me.santio.minehututils.skript.models

import com.google.gson.annotations.SerializedName

data class SkriptExample(
    val id: Long,
    @SerializedName("example_author")
    val exampleAuthor: String,
    @SerializedName("syntax_element")
    val syntaxElement: Long,
    @SerializedName("example_name")
    val exampleName: String,
    @SerializedName("example_code")
    val exampleCode: String,
    val score: Long,
    @SerializedName("official_example")
    val officialExample: Boolean
)

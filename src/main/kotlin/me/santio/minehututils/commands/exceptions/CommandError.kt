package me.santio.minehututils.commands.exceptions

internal class CommandError(
    override val message: String,
    val ephemeral: Boolean
) : Exception()

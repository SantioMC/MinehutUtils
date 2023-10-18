package me.santio.minehututils.adapters

import me.santio.coffee.common.adapter.ArgumentAdapter
import me.santio.coffee.common.adapter.ContextData
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.api.ServerModel

object ServerAdapter: ArgumentAdapter<ServerModel>() {

    override val type: Class<ServerModel> = ServerModel::class.java
    override val error: String = "Failed to find server '%arg%'"

    override fun adapt(arg: String, context: ContextData): ServerModel? {
        return Minehut.server(arg)
    }

    override fun suggest(arg: String): List<String> {
        val servers = Minehut.servers()?.servers ?: emptyList()

        if (arg.isEmpty() && servers.isEmpty()) return listOf("Loading servers...")
        return servers.map { it.name }
    }

}
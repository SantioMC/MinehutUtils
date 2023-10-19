package me.santio.minehututils.adapters

import me.santio.coffee.common.adapter.ArgumentAdapter
import me.santio.coffee.common.adapter.ContextData
import me.santio.minehututils.resolvers.DurationResolver
import java.time.Duration

object DurationAdapter: ArgumentAdapter<Duration>() {

    override val type: Class<Duration> = Duration::class.java
    override val error: String = "Invalid duration entered '%arg%'"

    override fun adapt(arg: String, context: ContextData): Duration? {
        return DurationResolver.from(arg)
    }

}
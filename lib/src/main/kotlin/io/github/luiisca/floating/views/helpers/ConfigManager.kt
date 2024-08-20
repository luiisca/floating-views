package io.github.luiisca.floating.views.helpers

import io.github.luiisca.floating.views.FloatingViewsConfig
import java.util.UUID

object ConfigManager {
    private val configs = mutableMapOf<String, FloatingViewsConfig>()

    fun addConfig(config: FloatingViewsConfig): String {
        val id = UUID.randomUUID().toString()
        configs[id] = config

        return id
    }

    fun getConfig(id: String): FloatingViewsConfig? = configs[id]

    fun removeConfig(id: String) {
        configs.remove(id)
    }
}
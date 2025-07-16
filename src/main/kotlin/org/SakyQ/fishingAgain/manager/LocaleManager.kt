package org.SakyQ.fishingAgain.manager

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class LocaleManager(private val plugin: JavaPlugin) {
    private val locales = mutableMapOf<String, YamlConfiguration>()

    init {
        loadLocales()
    }

    private fun loadLocales() {
        val localeFolder = File(plugin.dataFolder, "locales")
        if (!localeFolder.exists()) {
            localeFolder.mkdirs()
        }

        for (locale in arrayOf("en")) {
            val localeFile = File(localeFolder, "messages_$locale.yml")
            if (!localeFile.exists()) {
                plugin.saveResource("locales/messages_$locale.yml", false)
            }
            locales[locale] = YamlConfiguration.loadConfiguration(localeFile)
        }
    }

    private fun translateColors(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }

    fun getMessage(player: Player, key: String, vararg args: Any): String {
        val localeCode = player.locale.split("_")[0]
        val locale = locales[localeCode] ?: locales["en"]!!

        var message = locale.getString(key, "Message not found: $key")!!

        args.forEachIndexed { index, arg ->
            message = message.replace("{$index}", arg.toString())
        }

        return translateColors(message)
    }
}
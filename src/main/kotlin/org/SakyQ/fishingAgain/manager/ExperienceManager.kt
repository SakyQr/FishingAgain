package org.SakyQ.fishingAgain.manager

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

data class FishingLevel(
    val level: Int,
    val experience: Int,
    val experienceToNext: Int
)

class ExperienceManager(private val plugin: JavaPlugin) {
    private val playerData = mutableMapOf<UUID, FishingLevel>()
    private lateinit var dataFile: File
    private lateinit var dataConfig: YamlConfiguration

    init {
        setupDataFile()
        loadPlayerData()
    }

    private fun setupDataFile() {
        dataFile = File(plugin.dataFolder, "player_data.yml")
        if (!dataFile.exists()) {
            dataFile.createNewFile()
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
    }

    private fun loadPlayerData() {
        for (key in dataConfig.getKeys(false)) {
            try {
                val uuid = UUID.fromString(key)
                val level = dataConfig.getInt("$key.level", 1)
                val experience = dataConfig.getInt("$key.experience", 0)
                val experienceToNext = calculateExperienceForNextLevel(level) - experience

                playerData[uuid] = FishingLevel(level, experience, experienceToNext)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load player data for $key: ${e.message}")
            }
        }
        plugin.logger.info("Loaded fishing data for ${playerData.size} players")
    }

    fun savePlayerData() {
        for ((uuid, fishingLevel) in playerData) {
            val key = uuid.toString()
            dataConfig.set("$key.level", fishingLevel.level)
            dataConfig.set("$key.experience", fishingLevel.experience)
        }

        try {
            dataConfig.save(dataFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save player data: ${e.message}")
        }
    }

    fun getPlayerLevel(player: Player): FishingLevel {
        return playerData.getOrPut(player.uniqueId) {
            FishingLevel(1, 0, calculateExperienceForNextLevel(1))
        }
    }

    fun giveExperience(player: Player, amount: Int) {
        val currentLevel = getPlayerLevel(player)
        var newExperience = currentLevel.experience + amount
        var newLevel = currentLevel.level

        // Check for level ups
        while (newExperience >= calculateExperienceForNextLevel(newLevel)) {
            newExperience -= calculateExperienceForNextLevel(newLevel)
            newLevel++

            // Notify player of level up
            onLevelUp(player, newLevel)
        }

        val experienceToNext = calculateExperienceForNextLevel(newLevel) - newExperience
        val updatedLevel = FishingLevel(newLevel, newExperience, experienceToNext)

        playerData[player.uniqueId] = updatedLevel

        // Give vanilla experience too
        val vanillaExp = plugin.config.getInt("fishing.vanilla-experience-per-catch", 1)
        if (vanillaExp > 0) {
            player.giveExp(vanillaExp)
        }

        // Show experience gain message
        val localeManager = LocaleManager(plugin)
        player.sendMessage(localeManager.getMessage(
            player, "experience_gained",
            amount, updatedLevel.level, updatedLevel.experience, updatedLevel.experienceToNext
        ))
    }

    private fun onLevelUp(player: Player, newLevel: Int) {
        val localeManager = LocaleManager(plugin)

        // Send level up message
        player.sendMessage(localeManager.getMessage(player, "level_up", newLevel))

        // Play sound
        player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)

        // Give level up rewards
        val rewardsSection = plugin.config.getConfigurationSection("fishing.level-rewards.$newLevel")
        if (rewardsSection != null) {
            // Give item rewards
            val items = rewardsSection.getStringList("items")
            for (itemString in items) {
                try {
                    val parts = itemString.split(":")
                    val material = org.bukkit.Material.valueOf(parts[0].uppercase())
                    val amount = if (parts.size > 1) parts[1].toInt() else 1

                    player.inventory.addItem(org.bukkit.inventory.ItemStack(material, amount))
                } catch (e: Exception) {
                    plugin.logger.warning("Invalid reward item: $itemString")
                }
            }

            // Give command rewards
            val commands = rewardsSection.getStringList("commands")
            for (command in commands) {
                val processedCommand = command.replace("{player}", player.name)
                plugin.server.dispatchCommand(plugin.server.consoleSender, processedCommand)
            }
        }
    }

    private fun calculateExperienceForNextLevel(level: Int): Int {
        // Experience formula: level^2 * 50 + level * 25
        return level * level * 50 + level * 25
    }

    fun getTopPlayers(limit: Int = 10): List<Pair<String, FishingLevel>> {
        return playerData.entries
            .sortedWith(compareByDescending<Map.Entry<UUID, FishingLevel>> { it.value.level }
                .thenByDescending { it.value.experience })
            .take(limit)
            .mapNotNull { entry ->
                val player = plugin.server.getOfflinePlayer(entry.key)
                val name = player.name ?: "Unknown"
                name to entry.value
            }
    }

    fun onDisable() {
        savePlayerData()
    }
}
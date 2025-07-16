package org.SakyQ.fishingAgain.manager

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class FishRecord(
    val playerName: String,
    val playerUUID: String,
    val fishName: String,
    val fishType: String,
    val length: Double,
    val weight: Double,
    val lengthUnit: String,
    val weightUnit: String,
    val timestamp: String,
    val difficulty: String
)

data class Discovery(
    val playerName: String,
    val playerUUID: String,
    val fishName: String,
    val fishType: String,
    val timestamp: String,
    val difficulty: String
)

class FishingRecordsManager(private val plugin: JavaPlugin) {

    private val recordsFile = File(plugin.dataFolder, "fishing_records.yml")
    private val discoveriesFile = File(plugin.dataFolder, "discoveries.yml")
    private lateinit var recordsConfig: YamlConfiguration
    private lateinit var discoveriesConfig: YamlConfiguration

    private val fishRecords = mutableMapOf<String, FishRecord>() // fishType -> biggest record
    private val playerDiscoveries = mutableMapOf<UUID, MutableSet<String>>() // playerUUID -> discovered fish types

    init {
        setupFiles()
        loadRecords()
        loadDiscoveries()
    }

    private fun setupFiles() {
        if (!recordsFile.exists()) {
            recordsFile.createNewFile()
        }
        if (!discoveriesFile.exists()) {
            discoveriesFile.createNewFile()
        }

        recordsConfig = YamlConfiguration.loadConfiguration(recordsFile)
        discoveriesConfig = YamlConfiguration.loadConfiguration(discoveriesFile)
    }

    private fun loadRecords() {
        val recordsSection = recordsConfig.getConfigurationSection("records")
        if (recordsSection != null) {
            for (fishType in recordsSection.getKeys(false)) {
                val section = recordsSection.getConfigurationSection(fishType) ?: continue

                val record = FishRecord(
                    playerName = section.getString("player-name", "Unknown")!!,
                    playerUUID = section.getString("player-uuid", "")!!,
                    fishName = section.getString("fish-name", fishType)!!,
                    fishType = fishType,
                    length = section.getDouble("length", 0.0),
                    weight = section.getDouble("weight", 0.0),
                    lengthUnit = section.getString("length-unit", "cm")!!,
                    weightUnit = section.getString("weight-unit", "kg")!!,
                    timestamp = section.getString("timestamp", "")!!,
                    difficulty = section.getString("difficulty", "easy")!!
                )

                fishRecords[fishType] = record
            }
        }
        plugin.logger.info("Loaded ${fishRecords.size} fishing records")
    }

    private fun loadDiscoveries() {
        val discoveriesSection = discoveriesConfig.getConfigurationSection("discoveries")
        if (discoveriesSection != null) {
            for (playerUUID in discoveriesSection.getKeys(false)) {
                try {
                    val uuid = UUID.fromString(playerUUID)
                    val fishList = discoveriesSection.getStringList(playerUUID)
                    playerDiscoveries[uuid] = fishList.toMutableSet()
                } catch (e: Exception) {
                    plugin.logger.warning("Invalid UUID in discoveries: $playerUUID")
                }
            }
        }
        plugin.logger.info("Loaded discoveries for ${playerDiscoveries.size} players")
    }

    fun recordCatch(player: Player, fishItem: ItemStack, fishStats: FishStats, difficulty: String, fishStatsManager: FishStatsManager) {
        val fishType = identifyFishType(fishItem)
        val fishName = fishItem.itemMeta?.displayName ?: fishItem.type.name
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // Check for first discovery
        val playerId = player.uniqueId
        val playerDiscoveredFish = playerDiscoveries.getOrPut(playerId) { mutableSetOf() }

        val isFirstDiscovery = !playerDiscoveredFish.contains(fishType)
        if (isFirstDiscovery) {
            playerDiscoveredFish.add(fishType)
            broadcastDiscovery(player, fishName, difficulty)
            saveDiscoveries()
        }

        // Get metric values for consistent record comparison
        val meta = fishItem.itemMeta
        val lengthCm = meta?.persistentDataContainer?.get(
            org.bukkit.NamespacedKey(plugin, "fish_length_cm"),
            org.bukkit.persistence.PersistentDataType.DOUBLE
        ) ?: convertToMetric(fishStats.length, fishStats.lengthUnit)

        val weightKg = meta?.persistentDataContainer?.get(
            org.bukkit.NamespacedKey(plugin, "fish_weight_kg"),
            org.bukkit.persistence.PersistentDataType.DOUBLE
        ) ?: convertWeightToKg(fishStats.weight, fishStats.weightUnit)

        // Check for new record using metric values
        val currentRecord = fishRecords[fishType]
        val currentRecordLengthCm = currentRecord?.let {
            convertToMetric(it.length, it.lengthUnit)
        } ?: 0.0

        val isNewRecord = lengthCm > currentRecordLengthCm

        if (isNewRecord) {
            val newRecord = FishRecord(
                playerName = player.name,
                playerUUID = player.uniqueId.toString(),
                fishName = fishName,
                fishType = fishType,
                length = fishStats.length,
                weight = fishStats.weight,
                lengthUnit = fishStats.lengthUnit,
                weightUnit = fishStats.weightUnit,
                timestamp = currentTime,
                difficulty = difficulty
            )

            fishRecords[fishType] = newRecord
            broadcastNewRecord(player, newRecord, currentRecord != null)
            saveRecords()
        }

        // Special broadcasts for rare/large fish
        if (difficulty.lowercase() in listOf("legendary", "mythical", "godlike", "divine")) {
            broadcastRareCatch(player, fishName, fishStats, difficulty)
        }
    }

    private fun convertToMetric(length: Double, unit: String): Double {
        return when (unit.lowercase()) {
            "in", "inches" -> length * 2.54
            else -> length // Assume cm
        }
    }

    private fun convertWeightToKg(weight: Double, unit: String): Double {
        return when (unit.lowercase()) {
            "lbs", "pounds" -> weight / 2.20462
            "g", "grams" -> weight / 1000.0
            "oz", "ounces" -> weight / 35.274
            else -> weight // Assume kg
        }
    }

    private fun broadcastDiscovery(player: Player, fishName: String, difficulty: String) {
        val cleanFishName = fishName.replace("§[0-9a-fk-or]".toRegex(), "")
        val difficultyColor = when (difficulty.lowercase()) {
            "legendary" -> "§5"
            "mythical" -> "§d"
            "godlike" -> "§4"
            "divine" -> "§f"
            else -> "§6"
        }

        Bukkit.broadcastMessage("$difficultyColor🎣 FIRST DISCOVERY! ${player.name} discovered a $cleanFishName!")

        // Play sound to all players
        Bukkit.getOnlinePlayers().forEach { p ->
            p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f)
        }
    }

    private fun broadcastNewRecord(player: Player, record: FishRecord, wasExistingRecord: Boolean) {
        val cleanFishName = record.fishName.replace("§[0-9a-fk-or]".toRegex(), "")
        val lengthDisplay = String.format("%.1f", record.length)
        val weightDisplay = if (record.weightUnit == "g" || record.weightUnit == "oz") {
            String.format("%.0f", record.weight)
        } else {
            String.format("%.2f", record.weight)
        }

        val difficultyColor = when (record.difficulty.lowercase()) {
            "legendary" -> "§5"
            "mythical" -> "§d"
            "godlike" -> "§4"
            "divine" -> "§f"
            else -> "§6"
        }

        val recordType = if (wasExistingRecord) "BROKE THE RECORD" else "SET A NEW RECORD"
        Bukkit.broadcastMessage("$difficultyColor🏆 $recordType! ${player.name} caught a $cleanFishName that is $lengthDisplay ${record.lengthUnit} long!")

        // Play sound to all players
        val sound = if (wasExistingRecord) Sound.ENTITY_PLAYER_LEVELUP else Sound.UI_TOAST_CHALLENGE_COMPLETE
        Bukkit.getOnlinePlayers().forEach { p ->
            p.playSound(p.location, sound, 1.0f, 0.8f)
        }
    }

    private fun broadcastRareCatch(player: Player, fishName: String, fishStats: FishStats, difficulty: String) {
        val cleanFishName = fishName.replace("§[0-9a-fk-or]".toRegex(), "")
        val lengthDisplay = String.format("%.1f", fishStats.length)

        val difficultyColor = when (difficulty.lowercase()) {
            "legendary" -> "§5"
            "mythical" -> "§d"
            "godlike" -> "§4"
            "divine" -> "§f"
            else -> "§6"
        }

        val message = when (difficulty.lowercase()) {
            "legendary" -> "$difficultyColor⭐ LEGENDARY CATCH! ${player.name} landed a $cleanFishName ($lengthDisplay ${fishStats.lengthUnit})!"
            "mythical" -> "$difficultyColor✦ MYTHICAL CATCH! ${player.name} caught a $cleanFishName ($lengthDisplay ${fishStats.lengthUnit})!"
            "godlike" -> "$difficultyColor⬢ GODLIKE CATCH! ${player.name} mastered a $cleanFishName ($lengthDisplay ${fishStats.lengthUnit})!"
            "divine" -> "$difficultyColor✦ DIVINE CATCH! ${player.name} achieved the impossible with a $cleanFishName ($lengthDisplay ${fishStats.lengthUnit})!"
            else -> return
        }

        Bukkit.broadcastMessage(message)

        // Play special sound
        val sound = when (difficulty.lowercase()) {
            "legendary" -> Sound.ENTITY_ENDER_DRAGON_DEATH
            "mythical" -> Sound.ENTITY_WITHER_SPAWN
            "godlike", "divine" -> Sound.ENTITY_LIGHTNING_BOLT_THUNDER
            else -> Sound.ENTITY_PLAYER_LEVELUP
        }

        Bukkit.getOnlinePlayers().forEach { p ->
            p.playSound(p.location, sound, 0.5f, 1.0f)
        }
    }

    private fun identifyFishType(fishItem: ItemStack): String {
        val displayName = fishItem.itemMeta?.displayName
        if (displayName != null) {
            val cleanName = displayName.replace("§[0-9a-fk-or]".toRegex(), "").lowercase()
            // Return a simplified fish type for record keeping with improved matching
            return when {
                cleanName.contains("sardine") -> "sardine"
                cleanName.contains("herring") -> "herring"
                cleanName.contains("mackerel") -> "mackerel"
                cleanName.contains("sea bass") || cleanName.contains("bass") -> "sea_bass"
                cleanName.contains("red mullet") || cleanName.contains("mullet") -> "red_mullet"
                cleanName.contains("flatfish") || cleanName.contains("flounder") -> "flatfish"
                cleanName.contains("bluefin tuna") || cleanName.contains("tuna") -> "bluefin_tuna"
                cleanName.contains("swordfish") -> "swordfish"
                cleanName.contains("mahi-mahi") || cleanName.contains("mahi") -> "mahi_mahi"
                cleanName.contains("giant squid") || cleanName.contains("squid") -> "giant_squid"
                cleanName.contains("great white shark") || cleanName.contains("shark") -> "great_white_shark"
                cleanName.contains("whale bone") || cleanName.contains("whale") -> "whale_bone"
                cleanName.contains("compass") -> "ship_compass"
                cleanName.contains("naval chart") || cleanName.contains("chart") -> "naval_chart"
                cleanName.contains("ship's bell") || cleanName.contains("bell") -> "ship_bell"
                cleanName.contains("cannonball") -> "cannonball"
                cleanName.contains("anchor") -> "ship_anchor"
                cleanName.contains("doubloon") -> "spanish_doubloon"
                cleanName.contains("lantern") -> "captain_lantern"
                cleanName.contains("naval") && cleanName.contains("sword") -> "naval_sword"
                cleanName.contains("wheel") -> "ship_wheel"
                cleanName.contains("message") || cleanName.contains("bottle") -> "message_bottle"
                cleanName.contains("crown") -> "royal_crown"
                cleanName.contains("uniform") -> "admiral_uniform"
                cleanName.contains("log") -> "captain_log"
                else -> {
                    // Fallback to material type
                    fishItem.type.name.lowercase()
                }
            }
        }
        return fishItem.type.name.lowercase()
    }

    fun getPlayerDiscoveries(playerId: UUID): Set<String> {
        return playerDiscoveries[playerId] ?: emptySet()
    }

    fun getRecord(fishType: String): FishRecord? {
        return fishRecords[fishType]
    }

    fun getAllRecords(): Map<String, FishRecord> {
        return fishRecords.toMap()
    }

    fun getTopPlayers(limit: Int = 10): List<Pair<String, Int>> {
        // Count discoveries per player
        val playerDiscoveryCount = mutableMapOf<String, Int>()

        playerDiscoveries.forEach { (uuid, discoveries) ->
            val player = Bukkit.getOfflinePlayer(uuid)
            val playerName = player.name ?: "Unknown"
            playerDiscoveryCount[playerName] = discoveries.size
        }

        return playerDiscoveryCount.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun getPlayerStats(playerId: UUID): Map<String, Any> {
        val discoveries = playerDiscoveries[playerId] ?: emptySet()
        val records = fishRecords.values.filter { it.playerUUID == playerId.toString() }

        return mapOf(
            "discoveries" to discoveries.size,
            "records_held" to records.size,
            "latest_discovery" to (discoveries.lastOrNull() ?: "None"),
            "rarest_catch" to getRarestCatch(discoveries)
        )
    }

    private fun getRarestCatch(discoveries: Set<String>): String {
        val rarityOrder = listOf(
            "royal_crown", "admiral_uniform", "captain_log", // Legendary
            "naval_sword", "ship_wheel", "message_bottle", // Rare Treasure
            "ship_anchor", "spanish_doubloon", "captain_lantern", // Valuable Treasure
            "ship_compass", "naval_chart", "ship_bell", "cannonball", // Treasure
            "giant_squid", "great_white_shark", "whale_bone", // Epic
            "bluefin_tuna", "swordfish", "mahi_mahi", // Rare
            "sea_bass", "red_mullet", "flatfish", // Uncommon
            "sardine", "herring", "mackerel" // Common
        )

        return discoveries.firstOrNull { it in rarityOrder } ?: "None"
    }

    private fun saveRecords() {
        for ((fishType, record) in fishRecords) {
            val path = "records.$fishType"
            recordsConfig.set("$path.player-name", record.playerName)
            recordsConfig.set("$path.player-uuid", record.playerUUID)
            recordsConfig.set("$path.fish-name", record.fishName)
            recordsConfig.set("$path.length", record.length)
            recordsConfig.set("$path.weight", record.weight)
            recordsConfig.set("$path.length-unit", record.lengthUnit)
            recordsConfig.set("$path.weight-unit", record.weightUnit)
            recordsConfig.set("$path.timestamp", record.timestamp)
            recordsConfig.set("$path.difficulty", record.difficulty)
        }

        try {
            recordsConfig.save(recordsFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save fishing records: ${e.message}")
        }
    }

    private fun saveDiscoveries() {
        for ((playerId, discoveries) in playerDiscoveries) {
            discoveriesConfig.set("discoveries.$playerId", discoveries.toList())
        }

        try {
            discoveriesConfig.save(discoveriesFile)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save discoveries: ${e.message}")
        }
    }

    fun onDisable() {
        saveRecords()
        saveDiscoveries()
    }
}
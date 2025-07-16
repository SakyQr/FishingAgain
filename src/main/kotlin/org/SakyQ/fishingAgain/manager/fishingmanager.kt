package org.SakyQ.fishingAgain.manager

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

data class FishingDifficulty(
    val name: String,
    val successZoneSize: Double, // 0.1 to 1.0 (percentage of bar)
    val speed: Int, // cursor movement speed
    val experienceMultiplier: Double,
    val timeLimit: Int = 100, // ticks before auto-fail (20 ticks = 1 second)
    val cursorAcceleration: Boolean = false // whether cursor accelerates over time
)

data class CustomFishItem(
    val material: Material,
    val displayName: String,
    val lore: List<String>,
    val difficulty: FishingDifficulty,
    val rarity: Double // 0.0 to 1.0 (chance to get this item)
)

class FishingManager(private val plugin: JavaPlugin) {
    private val difficulties = mutableMapOf(
        "easy" to FishingDifficulty("Easy", 0.3, 1, 1.0, 120, false),
        "medium" to FishingDifficulty("Medium", 0.2, 2, 1.5, 100, false),
        "hard" to FishingDifficulty("Hard", 0.15, 3, 2.0, 80, true),
        "legendary" to FishingDifficulty("Legendary", 0.1, 4, 3.0, 60, true),
        "mythical" to FishingDifficulty("Mythical", 0.08, 5, 4.0, 50, true),
        "godlike" to FishingDifficulty("Godlike", 0.05, 6, 5.0, 40, true)
    )

    private val customItems = mutableListOf<CustomFishItem>()

    init {
        loadCustomItems()
    }

    private fun loadCustomItems() {
        val config = plugin.config

        // Load difficulty settings from config first
        loadDifficultiesFromConfig()

        // Load custom items from config
        val itemsSection = config.getConfigurationSection("fishing.custom-items")
        if (itemsSection != null) {
            for (key in itemsSection.getKeys(false)) {
                val itemSection = itemsSection.getConfigurationSection(key) ?: continue

                try {
                    val material = Material.valueOf(itemSection.getString("material", "COD")!!.uppercase())
                    val displayName = itemSection.getString("display-name", key)!!
                    val lore = itemSection.getStringList("lore")
                    val difficultyName = itemSection.getString("difficulty", "easy")!!.lowercase()
                    val rarity = itemSection.getDouble("rarity", 0.1)

                    val difficulty = difficulties[difficultyName] ?: difficulties["easy"]!!

                    customItems.add(
                        CustomFishItem(
                            material = material,
                            displayName = displayName,
                            lore = lore,
                            difficulty = difficulty,
                            rarity = rarity
                        )
                    )
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to load custom fishing item '$key': ${e.message}")
                }
            }
        }

        // Add default items if none configured
        if (customItems.isEmpty()) {
            addDefaultItems()
        }

        plugin.logger.info("Loaded ${customItems.size} custom fishing items")
    }

    private fun loadDifficultiesFromConfig() {
        val difficultySection = plugin.config.getConfigurationSection("fishing.difficulties")
        if (difficultySection != null) {
            val customDifficulties = mutableMapOf<String, FishingDifficulty>()

            for (key in difficultySection.getKeys(false)) {
                val section = difficultySection.getConfigurationSection(key) ?: continue

                try {
                    val name = section.getString("name", key)!!
                    val successZoneSize = section.getDouble("success-zone-size", 0.2)
                    val speed = section.getInt("speed", 2)
                    val experienceMultiplier = section.getDouble("experience-multiplier", 1.0)
                    val timeLimit = section.getInt("time-limit", 100)
                    val cursorAcceleration = section.getBoolean("cursor-acceleration", false)

                    customDifficulties[key.lowercase()] = FishingDifficulty(
                        name, successZoneSize, speed, experienceMultiplier, timeLimit, cursorAcceleration
                    )
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to load difficulty '$key': ${e.message}")
                }
            }

            // Override default difficulties with config ones
            if (customDifficulties.isNotEmpty()) {
                (difficulties as MutableMap).putAll(customDifficulties)
            }
        }
    }

    private fun addDefaultItems() {
        customItems.addAll(listOf(
            // Common Fish (Easy difficulty)
            CustomFishItem(
                Material.COD, "§fSardine",
                listOf("§7A small, silvery fish", "§7Common in coastal waters"),
                difficulties["easy"]!!, 0.25
            ),
            CustomFishItem(
                Material.SALMON, "§fHerring",
                listOf("§7A schooling fish", "§7Popular with fishermen"),
                difficulties["easy"]!!, 0.22
            ),
            CustomFishItem(
                Material.COD, "§fMackerel",
                listOf("§7A fast-swimming fish", "§7Known for its blue-green stripes"),
                difficulties["easy"]!!, 0.20
            ),

            // Uncommon Fish (Medium difficulty)
            CustomFishItem(
                Material.COD, "§eSea Bass",
                listOf("§7A popular game fish", "§7Fights hard when hooked"),
                difficulties["medium"]!!, 0.15
            ),
            CustomFishItem(
                Material.TROPICAL_FISH, "§eRed Mullet",
                listOf("§7Prized for its delicate flavor", "§7Found near rocky bottoms"),
                difficulties["medium"]!!, 0.12
            ),
            CustomFishItem(
                Material.COD, "§eFlatfish",
                listOf("§7A bottom-dwelling fish", "§7Masters of camouflage"),
                difficulties["medium"]!!, 0.10
            ),

            // Rare Fish (Hard difficulty)
            CustomFishItem(
                Material.COD, "§6Bluefin Tuna",
                listOf("§7A massive oceanic predator", "§6Highly prized by anglers", "§6Worth a fortune!"),
                difficulties["hard"]!!, 0.08
            ),
            CustomFishItem(
                Material.COD, "§6Swordfish",
                listOf("§7Armed with a deadly bill", "§6Legendary fighter", "§6Extremely valuable!"),
                difficulties["hard"]!!, 0.06
            ),
            CustomFishItem(
                Material.TROPICAL_FISH, "§6Mahi-Mahi",
                listOf("§7Beautiful golden fish", "§6Known for spectacular jumps", "§6Prized catch!"),
                difficulties["hard"]!!, 0.05
            ),

            // Epic Fish (Legendary difficulty)
            CustomFishItem(
                Material.NAUTILUS_SHELL, "§5Giant Squid Tentacle",
                listOf("§7A piece of the legendary kraken", "§5Only the bravest dare to catch", "§5Ancient terror of the depths"),
                difficulties["legendary"]!!, 0.03
            ),
            CustomFishItem(
                Material.COD, "§5Great White Shark",
                listOf("§7Apex predator of the oceans", "§5Requires incredible skill", "§5Dangerous and magnificent"),
                difficulties["legendary"]!!, 0.025
            ),
            CustomFishItem(
                Material.HEART_OF_THE_SEA, "§5Whale Bone",
                listOf("§7Remnant of an ocean giant", "§5Blessed by the sea gods", "§5Holds ancient power"),
                difficulties["legendary"]!!, 0.02
            ),

            // Treasure Items
            CustomFishItem(
                Material.COMPASS, "§eLost Ship's Compass",
                listOf("§7A navigator's most precious tool", "§ePoints to unknown treasures"),
                difficulties["medium"]!!, 0.04
            ),
            CustomFishItem(
                Material.MAP, "§eNaval Chart",
                listOf("§7Ancient maritime routes", "§eMarks secret passages"),
                difficulties["medium"]!!, 0.035
            ),
            CustomFishItem(
                Material.BELL, "§eShip's Bell",
                listOf("§7Once called sailors to duty", "§eEchoes with maritime history"),
                difficulties["medium"]!!, 0.03
            ),
            CustomFishItem(
                Material.IRON_BLOCK, "§7Cannonball",
                listOf("§7Heavy ammunition from naval battles", "§7Forged in times of war"),
                difficulties["hard"]!!, 0.025
            ),
            CustomFishItem(
                Material.ANVIL, "§7Ship's Anchor",
                listOf("§7Massive iron anchor", "§7Once held great vessels", "§6Extremely heavy and valuable"),
                difficulties["hard"]!!, 0.02
            ),
            CustomFishItem(
                Material.GOLD_NUGGET, "§6Spanish Doubloon",
                listOf("§7Pirate treasure from the golden age", "§6Pure Spanish gold", "§6Worth a king's ransom!"),
                difficulties["hard"]!!, 0.015
            ),
            CustomFishItem(
                Material.LANTERN, "§eCaptain's Lantern",
                listOf("§7Guided ships through stormy nights", "§eStill glows with inner light"),
                difficulties["medium"]!!, 0.03
            ),
            CustomFishItem(
                Material.IRON_SWORD, "§7Naval Officer's Sword",
                listOf("§7Blade of honor and duty", "§7Served in countless battles", "§6Officer's ceremonial weapon"),
                difficulties["hard"]!!, 0.018
            ),
            CustomFishItem(
                Material.SHIELD, "§eShip's Wheel",
                listOf("§7Steered vessels across vast oceans", "§eHolds the spirit of adventure"),
                difficulties["medium"]!!, 0.025
            ),
            CustomFishItem(
                Material.GLASS_BOTTLE, "§bMessage in a Bottle",
                listOf("§7Contains an urgent plea for help", "§bSealed decades ago", "§bMysterious and intriguing"),
                difficulties["medium"]!!, 0.02
            ),

            // Legendary Treasures (Mythical/Godlike difficulty)
            CustomFishItem(
                Material.GOLDEN_HELMET, "§6Royal Crown",
                listOf("§7Crown of a long-lost maritime kingdom", "§6Adorned with precious gems", "§dFit for oceanic royalty", "§4§lExtremely rare!"),
                difficulties["mythical"]!!, 0.008
            ),
            CustomFishItem(
                Material.LEATHER_CHESTPLATE, "§bAdmiral's Uniform",
                listOf("§7Uniform of the greatest naval commander", "§bDecorated with medals of honor", "§dLegendary leadership", "§4§lMuseum quality!"),
                difficulties["mythical"]!!, 0.007
            ),
            CustomFishItem(
                Material.BOOK, "§eCaptain's Log",
                listOf("§7Personal diary of a legendary explorer", "§eRecords of uncharted waters", "§dSecrets of the seven seas", "§4§lPriceless knowledge!"),
                difficulties["godlike"]!!, 0.005
            )
        ))
    }

    fun getCustomItem(originalItem: ItemStack): ItemStack? {
        // Check if we should replace with custom item
        val roll = Random.nextDouble()
        var cumulativeChance = 0.0

        // Sort by rarity (rarest first) to prioritize rare catches
        val sortedItems = customItems.sortedBy { it.rarity }

        for (customItem in sortedItems) {
            cumulativeChance += customItem.rarity
            if (roll <= cumulativeChance) {
                return createCustomItemStack(customItem)
            }
        }

        return null // Use original item
    }

    private fun createCustomItemStack(customItem: CustomFishItem): ItemStack {
        val itemStack = ItemStack(customItem.material)
        val meta: ItemMeta = itemStack.itemMeta!!

        meta.setDisplayName(customItem.displayName)
        if (customItem.lore.isNotEmpty()) {
            meta.lore = customItem.lore
        }

        // Add fishing metadata to mark as legitimate catch
        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "fishing_catch"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "true"
        )

        itemStack.itemMeta = meta
        return itemStack
    }

    fun getFishingDifficulty(item: ItemStack): FishingDifficulty {
        // Check if this is a custom item
        val itemMeta = item.itemMeta
        if (itemMeta?.hasDisplayName() == true) {
            val displayName = itemMeta.displayName
            val customItem = customItems.find { it.displayName == displayName }
            if (customItem != null) {
                return customItem.difficulty
            }

            // Check by display name content for difficulty hints
            val cleanName = displayName.replace("§[0-9a-fk-or]".toRegex(), "").lowercase()
            return when {
                cleanName.contains("royal") || cleanName.contains("admiral") || cleanName.contains("captain's log") -> difficulties["godlike"]!!
                cleanName.contains("crown") || cleanName.contains("uniform") || cleanName.contains("legendary") -> difficulties["mythical"]!!
                cleanName.contains("giant squid") || cleanName.contains("shark") || cleanName.contains("whale") -> difficulties["legendary"]!!
                cleanName.contains("tuna") || cleanName.contains("swordfish") || cleanName.contains("anchor") || cleanName.contains("doubloon") -> difficulties["hard"]!!
                cleanName.contains("sea bass") || cleanName.contains("mullet") || cleanName.contains("compass") || cleanName.contains("chart") -> difficulties["medium"]!!
                else -> difficulties["easy"]!!
            }
        }

        // Default difficulty based on vanilla items
        return when (item.type) {
            Material.COD -> difficulties["easy"]!!
            Material.SALMON -> difficulties["medium"]!!
            Material.TROPICAL_FISH -> difficulties["medium"]!!
            Material.PUFFERFISH -> difficulties["hard"]!!
            Material.NAUTILUS_SHELL -> difficulties["legendary"]!!
            Material.HEART_OF_THE_SEA -> difficulties["mythical"]!!
            Material.NETHER_STAR -> difficulties["godlike"]!!
            else -> difficulties["easy"]!!
        }
    }

    fun reloadCustomItems() {
        customItems.clear()
        difficulties.clear()
        difficulties.putAll(mapOf(
            "easy" to FishingDifficulty("Easy", 0.3, 1, 1.0, 120, false),
            "medium" to FishingDifficulty("Medium", 0.2, 2, 1.5, 100, false),
            "hard" to FishingDifficulty("Hard", 0.15, 3, 2.0, 80, true),
            "legendary" to FishingDifficulty("Legendary", 0.1, 4, 3.0, 60, true),
            "mythical" to FishingDifficulty("Mythical", 0.08, 5, 4.0, 50, true),
            "godlike" to FishingDifficulty("Godlike", 0.05, 6, 5.0, 40, true)
        ))
        loadCustomItems()
    }
}
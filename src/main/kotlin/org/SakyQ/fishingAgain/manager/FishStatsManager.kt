package org.SakyQ.fishingAgain.manager

import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

data class FishStats(
    val length: Double,    // in cm or inches
    val weight: Double,    // in kg or pounds
    val lengthUnit: String = "cm",
    val weightUnit: String = "kg"
)

data class FishSpecies(
    val name: String,
    val minLength: Double,
    val maxLength: Double,
    val minWeight: Double,
    val maxWeight: Double,
    val lengthWeightRatio: Double = 0.1, // kg per cm ratio
    val qualityModifier: Double = 1.0     // affects price based on size
)

class FishStatsManager(private val plugin: JavaPlugin) {

    private val fishSpecies = mutableMapOf<String, FishSpecies>()
    private var useMetricSystem = true
    private var lengthUnit = "cm"
    private var weightUnit = "kg"
    private var smallWeightUnit = "g"

    init {
        loadConfiguration()
        loadFishSpecies()
    }

    private fun loadConfiguration() {
        val config = plugin.config

        // Load measurement system preferences
        useMetricSystem = config.getBoolean("fish-stats.use-metric-system", true)

        if (useMetricSystem) {
            lengthUnit = config.getString("fish-stats.metric.length-unit", "cm") ?: "cm"
            weightUnit = config.getString("fish-stats.metric.weight-unit", "kg") ?: "kg"
            smallWeightUnit = config.getString("fish-stats.metric.small-weight-unit", "g") ?: "g"
        } else {
            lengthUnit = config.getString("fish-stats.imperial.length-unit", "in") ?: "in"
            weightUnit = config.getString("fish-stats.imperial.weight-unit", "lbs") ?: "lbs"
            smallWeightUnit = config.getString("fish-stats.imperial.small-weight-unit", "oz") ?: "oz"
        }
    }

    private fun loadFishSpecies() {
        // Load species data from config or set defaults
        val speciesSection = plugin.config.getConfigurationSection("fish-species")

        if (speciesSection != null) {
            for (speciesKey in speciesSection.getKeys(false)) {
                val section = speciesSection.getConfigurationSection(speciesKey) ?: continue

                val species = FishSpecies(
                    name = section.getString("name", speciesKey) ?: speciesKey,
                    minLength = section.getDouble("min-length", 5.0),
                    maxLength = section.getDouble("max-length", 20.0),
                    minWeight = section.getDouble("min-weight", 0.1),
                    maxWeight = section.getDouble("max-weight", 1.0),
                    lengthWeightRatio = section.getDouble("length-weight-ratio", 0.1),
                    qualityModifier = section.getDouble("quality-modifier", 1.0)
                )

                fishSpecies[speciesKey] = species
            }
        } else {
            // Load default fish species
            loadDefaultSpecies()
        }

        plugin.logger.info("Loaded ${fishSpecies.size} fish species with size/weight data")
    }

    private fun loadDefaultSpecies() {
        fishSpecies.putAll(mapOf(
            // Small fish
            "sardine" to FishSpecies("Sardine", 8.0, 15.0, 0.02, 0.08, 0.005),
            "herring" to FishSpecies("Herring", 12.0, 25.0, 0.05, 0.15, 0.006),
            "mackerel" to FishSpecies("Mackerel", 15.0, 35.0, 0.1, 0.5, 0.014),

            // Medium fish
            "sea_bass" to FishSpecies("Sea Bass", 25.0, 60.0, 0.5, 3.0, 0.05),
            "red_mullet" to FishSpecies("Red Mullet", 15.0, 40.0, 0.2, 1.2, 0.03),
            "flatfish" to FishSpecies("Flatfish", 20.0, 50.0, 0.3, 2.0, 0.04),

            // Large fish
            "bluefin_tuna" to FishSpecies("Bluefin Tuna", 120.0, 300.0, 15.0, 200.0, 0.67),
            "swordfish" to FishSpecies("Swordfish", 150.0, 400.0, 20.0, 300.0, 0.75),
            "mahi_mahi" to FishSpecies("Mahi-Mahi", 60.0, 150.0, 5.0, 30.0, 0.2),

            // Special catches
            "giant_squid" to FishSpecies("Giant Squid", 200.0, 1000.0, 50.0, 500.0, 0.5),
            "great_white_shark" to FishSpecies("Great White Shark", 200.0, 600.0, 100.0, 1000.0, 1.67),
            "whale_bone" to FishSpecies("Whale Bone", 50.0, 200.0, 5.0, 50.0, 0.25),

            // Treasures (fixed sizes)
            "ship_compass" to FishSpecies("Ship's Compass", 10.0, 15.0, 0.5, 1.0, 0.05),
            "naval_chart" to FishSpecies("Naval Chart", 20.0, 30.0, 0.1, 0.3, 0.01),
            "ship_bell" to FishSpecies("Ship's Bell", 15.0, 25.0, 2.0, 8.0, 0.3),
            "cannonball" to FishSpecies("Cannonball", 8.0, 12.0, 3.0, 8.0, 0.6),
            "ship_anchor" to FishSpecies("Ship's Anchor", 100.0, 200.0, 50.0, 200.0, 1.0),
            "spanish_doubloon" to FishSpecies("Spanish Doubloon", 2.0, 3.0, 0.02, 0.05, 0.02),
            "captain_lantern" to FishSpecies("Captain's Lantern", 20.0, 35.0, 1.0, 3.0, 0.08),
            "naval_sword" to FishSpecies("Naval Sword", 80.0, 120.0, 1.5, 4.0, 0.03),
            "ship_wheel" to FishSpecies("Ship's Wheel", 60.0, 100.0, 10.0, 25.0, 0.2),
            "message_bottle" to FishSpecies("Message in a Bottle", 15.0, 25.0, 0.2, 0.8, 0.03),
            "royal_crown" to FishSpecies("Royal Crown", 20.0, 30.0, 2.0, 5.0, 0.15),
            "admiral_uniform" to FishSpecies("Admiral's Uniform", 50.0, 80.0, 1.0, 3.0, 0.04),
            "captain_log" to FishSpecies("Captain's Log", 25.0, 35.0, 0.5, 1.5, 0.04),

            // Vanilla materials (for compatibility)
            "COD" to FishSpecies("Cod", 30.0, 80.0, 1.0, 8.0, 0.1),
            "SALMON" to FishSpecies("Salmon", 40.0, 120.0, 2.0, 15.0, 0.125),
            "TROPICAL_FISH" to FishSpecies("Tropical Fish", 5.0, 20.0, 0.01, 0.2, 0.01),
            "PUFFERFISH" to FishSpecies("Pufferfish", 10.0, 35.0, 0.05, 1.0, 0.029)
        ))
    }

    fun generateFishStats(fishKey: String, itemStack: ItemStack): ItemStack {
        val species = fishSpecies[fishKey] ?: fishSpecies[identifyFishSpecies(itemStack) ?: "COD"] ?: return itemStack

        // Generate realistic length within species range
        val lengthCm = Random.nextDouble(species.minLength, species.maxLength)

        // Generate weight based on length with some natural variation
        val baseWeightKg = lengthCm * species.lengthWeightRatio
        val weightVariation = Random.nextDouble(0.8, 1.2) // ±20% variation
        val weightKg = (baseWeightKg * weightVariation).coerceIn(species.minWeight, species.maxWeight)

        // Convert to display units if needed
        val displayLength = if (useMetricSystem) lengthCm else cmToInches(lengthCm)
        val displayWeight = if (useMetricSystem) {
            if (weightKg < 1.0) weightKg * 1000 else weightKg // Convert to grams if less than 1kg
        } else {
            kgToPounds(weightKg)
        }

        val finalWeightUnit = if (useMetricSystem) {
            if (weightKg < 1.0) smallWeightUnit else weightUnit
        } else {
            if (displayWeight < 1.0) smallWeightUnit else weightUnit
        }

        // Apply stats to item
        return applyStatsToItem(itemStack, FishStats(displayLength, displayWeight, lengthUnit, finalWeightUnit), species, lengthCm, weightKg)
    }

    private fun applyStatsToItem(itemStack: ItemStack, stats: FishStats, species: FishSpecies, lengthCm: Double, weightKg: Double): ItemStack {
        val meta = itemStack.itemMeta ?: return itemStack
        val currentLore = meta.lore?.toMutableList() ?: mutableListOf()

        // Remove existing fish stats if present
        currentLore.removeAll { line ->
            val cleanLine = line.replace("§[0-9a-fk-or]".toRegex(), "")
            cleanLine.contains("Length:") || cleanLine.contains("Weight:") || cleanLine.contains("Grade:") ||
                    cleanLine.contains("Record Catch!") || cleanLine.contains("Excellent specimen!")
        }

        // Determine size category using metric values for consistency
        val lengthPercentile = (lengthCm - species.minLength) / (species.maxLength - species.minLength)
        val sizeCategory = when {
            lengthPercentile >= 0.9 -> "§6§lTrophy Size!"
            lengthPercentile >= 0.7 -> "§eLarge"
            lengthPercentile >= 0.3 -> "§7Average"
            else -> "§8Small"
        }

        // Add spacing before stats
        if (currentLore.isNotEmpty() && currentLore.last().isNotEmpty()) {
            currentLore.add("")
        }

        // Format weight display
        val weightDisplay = formatWeight(stats.weight, stats.weightUnit)

        // Add size stats
        currentLore.add("§b⚖ Length: §f${String.format("%.1f", stats.length)} ${stats.lengthUnit}")
        currentLore.add("§b⚖ Weight: §f$weightDisplay")
        currentLore.add("§b⚖ Grade: $sizeCategory")

        // Add quality indicator for trophy fish
        if (lengthPercentile >= 0.9) {
            currentLore.add("§6✦ Record Catch! ✦")
        } else if (lengthPercentile >= 0.8) {
            currentLore.add("§eExcellent specimen!")
        }

        // Store raw metric values in persistent data for consistent calculations
        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "fish_length_cm"),
            org.bukkit.persistence.PersistentDataType.DOUBLE,
            lengthCm
        )
        meta.persistentDataContainer.set(
            org.bukkit.NamespacedKey(plugin, "fish_weight_kg"),
            org.bukkit.persistence.PersistentDataType.DOUBLE,
            weightKg
        )

        meta.lore = currentLore
        itemStack.itemMeta = meta

        return itemStack
    }

    private fun formatWeight(weight: Double, unit: String): String {
        return when (unit) {
            "g" -> "${String.format("%.0f", weight)} g"
            "oz" -> "${String.format("%.1f", weight)} oz"
            "kg" -> "${String.format("%.2f", weight)} kg"
            "lbs" -> "${String.format("%.2f", weight)} lbs"
            else -> "${String.format("%.2f", weight)} $unit"
        }
    }

    private fun cmToInches(cm: Double): Double = cm / 2.54
    private fun kgToPounds(kg: Double): Double = kg * 2.20462
    private fun inchesToCm(inches: Double): Double = inches * 2.54
    private fun poundsToKg(pounds: Double): Double = pounds / 2.20462

    fun getFishStats(itemStack: ItemStack): FishStats? {
        val lore = itemStack.itemMeta?.lore ?: return null

        var length: Double? = null
        var weight: Double? = null
        var lengthUnit = "cm"
        var weightUnit = "kg"

        for (line in lore) {
            val cleanLine = line.replace("§[0-9a-fk-or]".toRegex(), "")

            if (cleanLine.contains("Length:")) {
                val lengthPart = cleanLine.substringAfter("Length:").trim()
                val parts = lengthPart.split(" ")
                if (parts.size >= 2) {
                    length = parts[0].toDoubleOrNull()
                    lengthUnit = parts[1]
                }
            }

            if (cleanLine.contains("Weight:")) {
                val weightPart = cleanLine.substringAfter("Weight:").trim()
                val parts = weightPart.split(" ")
                if (parts.size >= 2) {
                    weight = parts[0].toDoubleOrNull()
                    weightUnit = parts[1]
                }
            }
        }

        return if (length != null && weight != null) {
            FishStats(length, weight, lengthUnit, weightUnit)
        } else null
    }

    // EXPLOIT FIX: Reduced size bonus multipliers to prevent crazy pricing
    fun calculateSizeBonus(itemStack: ItemStack, basePrice: Double): Double {
        val meta = itemStack.itemMeta ?: return basePrice

        // Use stored metric values for consistent calculations
        val lengthCm = meta.persistentDataContainer.get(
            org.bukkit.NamespacedKey(plugin, "fish_length_cm"),
            org.bukkit.persistence.PersistentDataType.DOUBLE
        )
        val weightKg = meta.persistentDataContainer.get(
            org.bukkit.NamespacedKey(plugin, "fish_weight_kg"),
            org.bukkit.persistence.PersistentDataType.DOUBLE
        )

        if (lengthCm == null || weightKg == null) {
            // Fallback to lore parsing for backwards compatibility
            val stats = getFishStats(itemStack) ?: return basePrice
            val fishKey = identifyFishSpecies(itemStack) ?: return basePrice
            val species = fishSpecies[fishKey] ?: return basePrice

            // Convert display units back to metric for calculations
            val metricLength = if (stats.lengthUnit == "in") inchesToCm(stats.length) else stats.length
            val metricWeight = if (stats.weightUnit == "lbs") poundsToKg(stats.weight)
            else if (stats.weightUnit == "g") stats.weight / 1000
            else stats.weight

            return calculateSizeBonusFromMetric(metricLength, species, basePrice)
        }

        val fishKey = identifyFishSpecies(itemStack) ?: return basePrice
        val species = fishSpecies[fishKey] ?: return basePrice

        return calculateSizeBonusFromMetric(lengthCm, species, basePrice)
    }

    // EXPLOIT FIX: Much smaller multipliers to prevent economy breaking
    private fun calculateSizeBonusFromMetric(lengthCm: Double, species: FishSpecies, basePrice: Double): Double {
        // Calculate size percentile using metric values
        val lengthPercentile = ((lengthCm - species.minLength) / (species.maxLength - species.minLength)).coerceIn(0.0, 1.0)

        // REDUCED size multipliers - maximum 50% bonus instead of 300%
        val sizeMultiplier = when {
            lengthPercentile >= 0.95 -> 1.5   // 50% bonus max (was 300%)
            lengthPercentile >= 0.9 -> 1.3    // 30% bonus (was 250%)
            lengthPercentile >= 0.8 -> 1.2    // 20% bonus (was 200%)
            lengthPercentile >= 0.6 -> 1.1    // 10% bonus (was 150%)
            lengthPercentile >= 0.4 -> 1.0    // Normal price
            lengthPercentile >= 0.2 -> 0.9    // 10% penalty (was 80%)
            else -> 0.8                       // 20% penalty (was 60%)
        }

        return basePrice * sizeMultiplier * species.qualityModifier
    }

    fun identifyFishSpecies(itemStack: ItemStack): String? {
        val displayName = itemStack.itemMeta?.displayName

        if (displayName != null) {
            val cleanName = displayName.replace("§[0-9a-fk-or]".toRegex(), "").lowercase()
            // Check custom fish first with improved matching
            return when {
                cleanName.contains("sardine") -> "sardine"
                cleanName.contains("herring") -> "herring"
                cleanName.contains("mackerel") -> "mackerel"
                cleanName.contains("sea bass") -> "sea_bass"
                cleanName.contains("red mullet") -> "red_mullet"
                cleanName.contains("flatfish") -> "flatfish"
                cleanName.contains("bluefin tuna") -> "bluefin_tuna"
                cleanName.contains("swordfish") -> "swordfish"
                cleanName.contains("mahi-mahi") -> "mahi_mahi"
                cleanName.contains("giant squid") -> "giant_squid"
                cleanName.contains("great white shark") -> "great_white_shark"
                cleanName.contains("whale bone") -> "whale_bone"
                cleanName.contains("compass") -> "ship_compass"
                cleanName.contains("naval chart") -> "naval_chart"
                cleanName.contains("ship's bell") -> "ship_bell"
                cleanName.contains("cannonball") -> "cannonball"
                cleanName.contains("anchor") -> "ship_anchor"
                cleanName.contains("doubloon") -> "spanish_doubloon"
                cleanName.contains("lantern") -> "captain_lantern"
                cleanName.contains("naval") && cleanName.contains("sword") -> "naval_sword"
                cleanName.contains("wheel") -> "ship_wheel"
                cleanName.contains("message") -> "message_bottle"
                cleanName.contains("crown") -> "royal_crown"
                cleanName.contains("uniform") -> "admiral_uniform"
                cleanName.contains("log") -> "captain_log"
                else -> itemStack.type.name
            }
        }

        // Fallback to material type
        return itemStack.type.name
    }

    fun getSpeciesInfo(fishKey: String): FishSpecies? {
        return fishSpecies[fishKey]
    }

    fun reloadConfiguration() {
        fishSpecies.clear()
        loadConfiguration()
        loadFishSpecies()
    }
}
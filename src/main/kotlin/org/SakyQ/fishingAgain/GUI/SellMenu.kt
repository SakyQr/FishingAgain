package org.SakyQ.fishingAgain.GUI

import net.milkbowl.vault.economy.Economy
import org.SakyQ.fishingAgain.manager.FishStatsManager
import org.SakyQ.fishingAgain.manager.LocaleManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.random.Random

class SellMenu(
    private val plugin: JavaPlugin,
    private val economy: Economy?,
    private val localeManager: LocaleManager,
    private val fishStatsManager: FishStatsManager
) : Listener {

    private val sellMenus = mutableMapOf<UUID, Inventory>()
    private val playersInTransaction = mutableSetOf<UUID>() // Prevent double-selling
    private val itemPriceCache = mutableMapOf<String, Double>() // Cache prices per item hash
    private var sellMenuTitle = "§6§lFish Market - Sell Your Catch"
    private var menuSize = 54
    private var sellSlots = 45
    private val fishPrices = mutableMapOf<String, FishPrice>()

    data class FishPrice(
        val basePrice: Double,
        val priceVariation: Double = 0.1,
        val displayName: String = ""
    )

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        loadConfiguration()
    }

    fun reloadConfiguration() {
        loadConfiguration()
        itemPriceCache.clear() // Clear cache on reload
    }

    private fun loadConfiguration() {
        val config = plugin.config

        // Load menu settings
        sellMenuTitle = config.getString("sell-menu.title", "§6§lFish Market - Sell Your Catch")!!
        menuSize = config.getInt("sell-menu.size", 54).coerceIn(9, 54)
        sellSlots = when (menuSize) {
            9 -> 6
            18 -> 15
            27 -> 24
            36 -> 33
            45 -> 42
            54 -> 45
            else -> 45
        }

        // Clear and reload fish prices
        fishPrices.clear()

        // Load material-based prices from "sell-menu.prices.materials" section
        val materialPricesSection = config.getConfigurationSection("sell-menu.prices.materials")
        if (materialPricesSection != null) {
            for (materialName in materialPricesSection.getKeys(false)) {
                val section = materialPricesSection.getConfigurationSection(materialName) ?: continue
                val basePrice = section.getDouble("base-price", 0.10)
                val variation = section.getDouble("price-variation", 0.05)

                fishPrices[materialName.uppercase()] = FishPrice(basePrice, variation)
            }
        }

        // Load custom fish prices from "sell-menu.prices.custom-fish" section
        val customPricesSection = config.getConfigurationSection("sell-menu.prices.custom-fish")
        if (customPricesSection != null) {
            for (fishKey in customPricesSection.getKeys(false)) {
                val section = customPricesSection.getConfigurationSection(fishKey) ?: continue
                val displayName = section.getString("display-name", "")!!
                val basePrice = section.getDouble("base-price", 2.0)
                val variation = section.getDouble("price-variation", 0.10)

                fishPrices["CUSTOM_$fishKey"] = FishPrice(basePrice, variation, displayName)
            }
        }

        // Add default prices if none configured
        if (fishPrices.isEmpty()) {
            addDefaultPrices()
        }

        plugin.logger.info("Loaded ${fishPrices.size} fish prices for sell menu")
    }

    private fun addDefaultPrices() {
        fishPrices.putAll(mapOf(
            // All vanilla materials = 0.10 coins (exploit protection)
            "COD" to FishPrice(0.10, 0.05),
            "SALMON" to FishPrice(0.10, 0.05),
            "TROPICAL_FISH" to FishPrice(0.10, 0.05),
            "PUFFERFISH" to FishPrice(0.10, 0.05),
            "NAUTILUS_SHELL" to FishPrice(0.10, 0.05),
            "HEART_OF_THE_SEA" to FishPrice(0.10, 0.05),
            "PRISMARINE_SHARD" to FishPrice(0.10, 0.05),

            // Custom fish by display name - 2-5 coins range
            "CUSTOM_sardine" to FishPrice(2.0, 0.10, "Sardine"),
            "CUSTOM_mackerel" to FishPrice(2.2, 0.10, "Mackerel"),
            "CUSTOM_herring" to FishPrice(2.5, 0.10, "Herring"),
            "CUSTOM_sea_bass" to FishPrice(3.0, 0.08, "Sea Bass"),
            "CUSTOM_red_mullet" to FishPrice(2.8, 0.08, "Red Mullet"),
            "CUSTOM_flounder" to FishPrice(2.7, 0.08, "Flatfish"),
            "CUSTOM_tuna" to FishPrice(4.5, 0.06, "Bluefin Tuna"),
            "CUSTOM_swordfish" to FishPrice(5.0, 0.06, "Swordfish"),
            "CUSTOM_mahi_mahi" to FishPrice(4.0, 0.06, "Mahi-Mahi"),
            "CUSTOM_giant_squid" to FishPrice(4.8, 0.05, "Giant Squid Tentacle"),
            "CUSTOM_shark" to FishPrice(5.0, 0.05, "Great White Shark"),
            "CUSTOM_whale_fragment" to FishPrice(5.0, 0.05, "Whale Bone"),

            // Treasures
            "CUSTOM_ship_compass" to FishPrice(3.5, 0.08, "Lost Ship's Compass"),
            "CUSTOM_ship_map" to FishPrice(4.0, 0.08, "Naval Chart"),
            "CUSTOM_ship_bell" to FishPrice(3.2, 0.08, "Ship's Bell"),
            "CUSTOM_cannon_ball" to FishPrice(2.5, 0.10, "Cannonball"),
            "CUSTOM_ship_anchor" to FishPrice(4.5, 0.06, "Ship's Anchor"),
            "CUSTOM_treasure_coin" to FishPrice(3.8, 0.08, "Spanish Doubloon"),
            "CUSTOM_ship_lantern" to FishPrice(3.3, 0.08, "Captain's Lantern"),
            "CUSTOM_naval_sword" to FishPrice(4.2, 0.06, "Naval Officer's Sword"),
            "CUSTOM_ship_wheel" to FishPrice(4.8, 0.06, "Ship's Wheel"),
            "CUSTOM_message_bottle" to FishPrice(2.8, 0.10, "Message in a Bottle"),
            "CUSTOM_royal_crown" to FishPrice(5.0, 0.05, "Royal Crown"),
            "CUSTOM_admiral_uniform" to FishPrice(4.5, 0.06, "Admiral's Coat"),
            "CUSTOM_ship_log" to FishPrice(3.0, 0.08, "Captain's Log")
        ))
    }

    fun openSellMenu(player: Player) {
        // EXPLOIT FIX: Check permission
        if (!player.hasPermission("fishingagain.sell")) {
            player.sendMessage("§cYou don't have permission to sell fish!")
            return
        }

        if (economy == null) {
            player.sendMessage(config.getString("sell-menu.messages.economy-disabled", "§cEconomy system not available!")!!)
            return
        }

        val inventory = Bukkit.createInventory(null, menuSize, sellMenuTitle)
        setupSellMenuItems(inventory)
        sellMenus[player.uniqueId] = inventory
        player.openInventory(inventory)

        val openMessage = config.getString("sell-menu.messages.menu-opened", "§6§lFish Market opened! §7Place your fish to sell them.")!!
        player.sendMessage(openMessage)

        val openSound = config.getString("sell-menu.sounds.menu-open", "BLOCK_CHEST_OPEN")!!
        try {
            player.playSound(player.location, Sound.valueOf(openSound), 1.0f, 1.0f)
        } catch (e: Exception) {
            player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f)
        }
    }

    private val config get() = plugin.config

    private fun setupSellMenuItems(inventory: Inventory) {
        val uiSlots = getUISlots()

        // Sell button
        val sellButtonSlot = config.getInt("sell-menu.ui-items.sell-button.slot", menuSize - 1)
        val sellButtonMaterial = config.getString("sell-menu.ui-items.sell-button.material", "EMERALD")!!
        val sellButton = ItemStack(Material.valueOf(sellButtonMaterial))
        val sellMeta = sellButton.itemMeta!!
        sellMeta.setDisplayName(config.getString("sell-menu.ui-items.sell-button.name", "§a§lSELL ALL FISH")!!)
        sellMeta.lore = config.getStringList("sell-menu.ui-items.sell-button.lore")
        sellButton.itemMeta = sellMeta
        inventory.setItem(sellButtonSlot, sellButton)

        // Info button
        val infoButtonSlot = config.getInt("sell-menu.ui-items.info-button.slot", menuSize - 9)
        val infoButtonMaterial = config.getString("sell-menu.ui-items.info-button.material", "BOOK")!!
        val infoButton = ItemStack(Material.valueOf(infoButtonMaterial))
        val infoMeta = infoButton.itemMeta!!
        infoMeta.setDisplayName(config.getString("sell-menu.ui-items.info-button.name", "§b§lFISH MARKET INFO")!!)

        val infoLore = config.getStringList("sell-menu.ui-items.info-button.lore").toMutableList()
        // Add dynamic price info
        if (config.getBoolean("sell-menu.ui-items.info-button.show-prices", true)) {
            infoLore.add("")
            infoLore.add("§7Current Fish Prices:")
            infoLore.add("§7• Vanilla Fish: §c0.10 coins")
            infoLore.add("§7• Custom Fish: §a2-5 coins")
            infoLore.add("§7• Rarity affects price!")
        }

        infoMeta.lore = infoLore
        infoButton.itemMeta = infoMeta
        inventory.setItem(infoButtonSlot, infoButton)

        // Close button
        val closeButtonSlot = config.getInt("sell-menu.ui-items.close-button.slot", menuSize - 5)
        val closeButtonMaterial = config.getString("sell-menu.ui-items.close-button.material", "BARRIER")!!
        val closeButton = ItemStack(Material.valueOf(closeButtonMaterial))
        val closeMeta = closeButton.itemMeta!!
        closeMeta.setDisplayName(config.getString("sell-menu.ui-items.close-button.name", "§c§lCLOSE MENU")!!)
        closeMeta.lore = config.getStringList("sell-menu.ui-items.close-button.lore")
        closeButton.itemMeta = closeMeta
        inventory.setItem(closeButtonSlot, closeButton)

        // Decorative items
        if (config.getBoolean("sell-menu.ui-items.decoration.enabled", true)) {
            val decorationMaterial = config.getString("sell-menu.ui-items.decoration.material", "BLUE_STAINED_GLASS_PANE")!!
            val decorationSlots = config.getIntegerList("sell-menu.ui-items.decoration.slots")

            val decoration = ItemStack(Material.valueOf(decorationMaterial))
            val decorationMeta = decoration.itemMeta!!
            decorationMeta.setDisplayName(config.getString("sell-menu.ui-items.decoration.name", " ")!!)
            decoration.itemMeta = decorationMeta

            for (slot in decorationSlots) {
                if (slot < menuSize && inventory.getItem(slot) == null) {
                    inventory.setItem(slot, decoration)
                }
            }
        }
    }

    private fun getUISlots(): Set<Int> {
        val uiSlots = mutableSetOf<Int>()
        uiSlots.add(config.getInt("sell-menu.ui-items.sell-button.slot", menuSize - 1))
        uiSlots.add(config.getInt("sell-menu.ui-items.info-button.slot", menuSize - 9))
        uiSlots.add(config.getInt("sell-menu.ui-items.close-button.slot", menuSize - 5))
        uiSlots.addAll(config.getIntegerList("sell-menu.ui-items.decoration.slots"))
        return uiSlots
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.inventory

        if (inventory.viewers.contains(player) && event.view.title == sellMenuTitle) {
            val clickedSlot = event.slot
            val uiSlots = getUISlots()

            if (clickedSlot in uiSlots) {
                event.isCancelled = true

                when (clickedSlot) {
                    config.getInt("sell-menu.ui-items.sell-button.slot", menuSize - 1) -> {
                        sellAllFish(player, inventory)
                    }
                    config.getInt("sell-menu.ui-items.close-button.slot", menuSize - 5) -> {
                        player.closeInventory()
                    }
                }
            } else if (clickedSlot < sellSlots) {
                // Allow normal inventory interaction for selling slots
                updateSellPreview(player, inventory)
            } else {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        if (event.view.title == sellMenuTitle) {
            sellMenus.remove(player.uniqueId)

            // Return any remaining items to player
            val inventory = event.inventory
            for (i in 0 until sellSlots) {
                val item = inventory.getItem(i)
                if (item != null && item.type != Material.AIR) {
                    player.inventory.addItem(item).values.forEach { leftover ->
                        player.world.dropItemNaturally(player.location, leftover)
                    }
                }
            }
        }
    }

    private fun updateSellPreview(player: Player, inventory: Inventory) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val totalValue = calculateTotalValue(inventory)
            val fishCount = countFish(inventory)

            val sellButtonSlot = config.getInt("sell-menu.ui-items.sell-button.slot", menuSize - 1)
            val sellButtonMaterial = config.getString("sell-menu.ui-items.sell-button.material", "EMERALD")!!
            val sellButton = ItemStack(Material.valueOf(sellButtonMaterial))
            val sellMeta = sellButton.itemMeta!!
            sellMeta.setDisplayName(config.getString("sell-menu.ui-items.sell-button.name", "§a§lSELL ALL FISH")!!)

            val lore = config.getStringList("sell-menu.ui-items.sell-button.lore").toMutableList()
            lore.add("")
            lore.add("§7Fish in market: §e$fishCount")
            lore.add("§7Total value: §a${String.format("%.2f", totalValue)}")
            lore.add("")
            lore.add(if (fishCount > 0) "§e§lClick to sell!" else "§c§lNo fish to sell!")

            sellMeta.lore = lore
            sellButton.itemMeta = sellMeta
            inventory.setItem(sellButtonSlot, sellButton)
        }, 1L)
    }

    private fun sellAllFish(player: Player, inventory: Inventory) {
        if (economy == null) {
            player.sendMessage(config.getString("sell-menu.messages.economy-disabled", "§cEconomy system not available!")!!)
            return
        }

        if (playersInTransaction.contains(player.uniqueId)) {
            return // Prevent double-selling
        }

        playersInTransaction.add(player.uniqueId)

        val fishItems = mutableListOf<ItemStack>()
        var totalValue = 0.0

        // Collect all fish items
        for (i in 0 until sellSlots) {
            val item = inventory.getItem(i)
            if (item != null && isFish(item)) {
                fishItems.add(item.clone())
                totalValue += getFishValue(item)
                inventory.setItem(i, null)
            }
        }

        playersInTransaction.remove(player.uniqueId)

        if (fishItems.isEmpty()) {
            player.sendMessage(config.getString("sell-menu.messages.no-fish", "§c§lNo fish to sell!")!!)
            val failSound = config.getString("sell-menu.sounds.sell-fail", "ENTITY_VILLAGER_NO")!!
            try {
                player.playSound(player.location, Sound.valueOf(failSound), 1.0f, 1.0f)
            } catch (e: Exception) {
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
            return
        }

        // Give money to player
        economy.depositPlayer(player, totalValue)

        // Success messages
        val successMessages = config.getStringList("sell-menu.messages.sale-success")
        for (message in successMessages) {
            val processedMessage = message
                .replace("{count}", fishItems.size.toString())
                .replace("{amount}", String.format("%.2f", totalValue))
                .replace("{balance}", String.format("%.2f", economy.getBalance(player)))
            player.sendMessage(processedMessage)
        }

        // Success sounds
        val successSounds = config.getStringList("sell-menu.sounds.sell-success")
        for (soundString in successSounds) {
            val parts = soundString.split(":")
            val soundName = parts[0]
            val volume = if (parts.size > 1) parts[1].toFloatOrNull() ?: 1.0f else 1.0f
            val pitch = if (parts.size > 2) parts[2].toFloatOrNull() ?: 1.0f else 1.0f

            try {
                player.playSound(player.location, Sound.valueOf(soundName), volume, pitch)
            } catch (e: Exception) {
                plugin.logger.warning("Invalid sound: $soundName")
            }
        }

        // Update sell button
        updateSellPreview(player, inventory)

        // Log the transaction
        if (config.getBoolean("sell-menu.logging.enabled", true)) {
            plugin.logger.info("${player.name} sold ${fishItems.size} fish for ${String.format("%.2f", totalValue)}")
        }
    }

    private fun calculateTotalValue(inventory: Inventory): Double {
        var total = 0.0
        for (i in 0 until sellSlots) {
            val item = inventory.getItem(i)
            if (item != null && isFish(item)) {
                total += getFishValue(item)
            }
        }
        return total
    }

    private fun countFish(inventory: Inventory): Int {
        var count = 0
        for (i in 0 until sellSlots) {
            val item = inventory.getItem(i)
            if (item != null && isFish(item)) {
                count += item.amount
            }
        }
        return count
    }

    // EXPLOIT FIX: Only allow items with fishing metadata
    private fun isFish(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false

        // MUST have fishing metadata to be sellable
        val fishingKey = org.bukkit.NamespacedKey(plugin, "fishing_catch")
        return meta.persistentDataContainer.has(fishingKey, org.bukkit.persistence.PersistentDataType.STRING)
    }

    private fun generateConsistentPrice(item: ItemStack, baseValue: Double): Double {
        // Create a hash from the item's stats for consistent pricing
        val meta = item.itemMeta
        val statsHash = if (meta != null) {
            val lengthCm = meta.persistentDataContainer.get(
                org.bukkit.NamespacedKey(plugin, "fish_length_cm"),
                org.bukkit.persistence.PersistentDataType.DOUBLE
            )
            val weightKg = meta.persistentDataContainer.get(
                org.bukkit.NamespacedKey(plugin, "fish_weight_kg"),
                org.bukkit.persistence.PersistentDataType.DOUBLE
            )

            if (lengthCm != null && weightKg != null) {
                "${lengthCm.hashCode()}_${weightKg.hashCode()}"
            } else {
                // Fallback to lore hash for backwards compatibility
                meta.lore?.joinToString("") ?: item.type.name
            }
        } else {
            item.type.name
        }

        // Use cached price if available
        val cachedPrice = itemPriceCache[statsHash]
        if (cachedPrice != null) {
            return cachedPrice
        }

        // Apply size bonus (REDUCED to prevent exploits)
        val bonusValue = fishStatsManager.calculateSizeBonus(item, baseValue)

        // Apply price variation based on fish stats (consistent for same fish)
        val variation = fishPrices.values.firstOrNull()?.priceVariation ?: 0.1
        val hashBasedSeed = statsHash.hashCode().toLong()
        val random = Random(hashBasedSeed) // Use hash as seed for consistency
        val randomMultiplier = (1.0 - variation) + (random.nextDouble() * variation * 2)

        val finalPrice = bonusValue * randomMultiplier

        // Cache the price
        itemPriceCache[statsHash] = finalPrice

        return finalPrice
    }

    // EXPLOIT FIX: Custom fish ALWAYS get custom prices, never material prices
    private fun getFishValue(item: ItemStack): Double {
        var baseValue = 0.10 // Default to lowest vanilla price

        val displayName = item.itemMeta?.displayName
        var foundCustomFish = false

        // Check custom fish first (by display name) - PRIORITY
        if (displayName != null) {
            val cleanDisplayName = displayName.replace("§[0-9a-fk-or]".toRegex(), "").lowercase()

            for ((key, price) in fishPrices) {
                if (key.startsWith("CUSTOM_") && price.displayName.isNotEmpty()) {
                    val cleanPriceName = price.displayName.replace("§[0-9a-fk-or]".toRegex(), "").lowercase()
                    if (cleanDisplayName.contains(cleanPriceName) ||
                        cleanPriceName.split(" ").any { cleanDisplayName.contains(it) }) {
                        baseValue = price.basePrice
                        foundCustomFish = true
                        break
                    }
                }
            }
        }

        // ONLY use material price if NO custom fish was found
        if (!foundCustomFish) {
            val materialPrice = fishPrices[item.type.name]
            if (materialPrice != null) {
                baseValue = materialPrice.basePrice
            }
        }

        // Generate consistent price with LIMITED size bonus
        val finalPrice = generateConsistentPrice(item, baseValue)

        return finalPrice * item.amount
    }
}
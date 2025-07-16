package org.SakyQ.fishingAgain.GUI

import org.SakyQ.fishingAgain.manager.FishRecord
import org.SakyQ.fishingAgain.manager.FishingManager
import org.SakyQ.fishingAgain.manager.FishingRecordsManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class DiscoveryGUI(
    private val plugin: JavaPlugin,
    private val recordsManager: FishingRecordsManager,
    private val fishingManager: FishingManager
) : Listener {

    private val discoveryMenus = mutableMapOf<UUID, Inventory>()
    private val menuTitle = "§6§lFish Discovery Collection"
    private var currentPage = mutableMapOf<UUID, Int>()

    // Complete fish data with proper materials and display names
    private val allFishData = listOf(
        // Common Fish
        FishData("sardine", Material.COD, "§fSardine", "§aCommon", "Found in shallow coastal waters"),
        FishData("herring", Material.SALMON, "§fHerring", "§aCommon", "Schooling fish in temperate waters"),
        FishData("mackerel", Material.COD, "§fMackerel", "§aCommon", "Fast swimmers with distinctive stripes"),

        // Uncommon Fish
        FishData("sea_bass", Material.COD, "§eSea Bass", "§2Uncommon", "Popular game fish in deeper waters"),
        FishData("red_mullet", Material.TROPICAL_FISH, "§eRed Mullet", "§2Uncommon", "Prized for delicate flavor"),
        FishData("flatfish", Material.COD, "§eFlatfish", "§2Uncommon", "Masters of bottom camouflage"),

        // Rare Fish
        FishData("bluefin_tuna", Material.COD, "§6Bluefin Tuna", "§6Rare", "Massive oceanic predator"),
        FishData("swordfish", Material.COD, "§6Swordfish", "§6Rare", "Armed with a deadly bill"),
        FishData("mahi_mahi", Material.TROPICAL_FISH, "§6Mahi-Mahi", "§6Rare", "Golden fish known for spectacular jumps"),

        // Epic Fish
        FishData("giant_squid", Material.NAUTILUS_SHELL, "§5Giant Squid Tentacle", "§5Epic", "Piece of the legendary kraken"),
        FishData("great_white_shark", Material.COD, "§5Great White Shark", "§5Epic", "Apex predator of the oceans"),
        FishData("whale_bone", Material.HEART_OF_THE_SEA, "§5Whale Bone", "§5Epic", "Remnant of an ocean giant"),

        // Treasure Items
        FishData("ship_compass", Material.COMPASS, "§eLost Ship's Compass", "§eTreasure", "Points to unknown treasures"),
        FishData("naval_chart", Material.MAP, "§eNaval Chart", "§eTreasure", "Ancient maritime routes"),
        FishData("ship_bell", Material.BELL, "§eShip's Bell", "§eTreasure", "Once called sailors to duty"),
        FishData("cannonball", Material.IRON_BLOCK, "§7Cannonball", "§eTreasure", "Heavy naval ammunition"),
        FishData("ship_anchor", Material.ANVIL, "§7Ship's Anchor", "§6Valuable Treasure", "Massive iron anchor from great vessels"),
        FishData("spanish_doubloon", Material.GOLD_NUGGET, "§6Spanish Doubloon", "§6Valuable Treasure", "Pure Spanish gold from pirates"),
        FishData("captain_lantern", Material.LANTERN, "§eCaptain's Lantern", "§6Valuable Treasure", "Guided ships through storms"),
        FishData("naval_sword", Material.IRON_SWORD, "§7Naval Officer's Sword", "§bRare Treasure", "Blade of honor and duty"),
        FishData("ship_wheel", Material.SHIELD, "§eShip's Wheel", "§bRare Treasure", "Steered vessels across vast oceans"),
        FishData("message_bottle", Material.GLASS_BOTTLE, "§bMessage in a Bottle", "§bRare Treasure", "Contains an urgent plea for help"),

        // Legendary Treasures
        FishData("royal_crown", Material.GOLDEN_HELMET, "§6Royal Crown", "§dLegendary Treasure", "Crown of a maritime kingdom"),
        FishData("admiral_uniform", Material.LEATHER_CHESTPLATE, "§bAdmiral's Uniform", "§dLegendary Treasure", "Uniform of the greatest commander"),
        FishData("captain_log", Material.BOOK, "§eCaptain's Log", "§dLegendary Treasure", "Diary of a legendary explorer")
    )

    data class FishData(
        val fishType: String,
        val material: Material,
        val displayName: String,
        val rarity: String,
        val description: String
    )

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun openDiscoveryMenu(player: Player) {
        currentPage[player.uniqueId] = 0
        val inventory = Bukkit.createInventory(null, 54, menuTitle)
        setupDiscoveryMenu(player, inventory, 0)
        discoveryMenus[player.uniqueId] = inventory
        player.openInventory(inventory)

        // Play opening sound
        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f)
    }

    private fun setupDiscoveryMenu(player: Player, inventory: Inventory, page: Int) {
        // Clear inventory
        inventory.clear()

        val playerDiscoveries = recordsManager.getPlayerDiscoveries(player.uniqueId)
        val allRecords = recordsManager.getAllRecords()

        val itemsPerPage = 28 // 4 rows of 7 items
        val startIndex = page * itemsPerPage
        val endIndex = (startIndex + itemsPerPage).coerceAtMost(allFishData.size)
        val totalPages = (allFishData.size + itemsPerPage - 1) / itemsPerPage

        // Add fish items for current page
        var slot = 10 // Start from row 2, column 2
        var rowCount = 0

        for (i in startIndex until endIndex) {
            if (rowCount >= 4) break // Max 4 rows

            val fishData = allFishData[i]
            val isDiscovered = playerDiscoveries.contains(fishData.fishType)
            val record = allRecords[fishData.fishType]

            val item = createDiscoveryItem(fishData, isDiscovered, record)
            inventory.setItem(slot, item)

            slot++
            // Skip to next row after 7 items
            if ((slot + 1) % 9 == 0) {
                slot += 2
                rowCount++
            }
        }

        // Add navigation and UI elements
        setupUIElements(inventory, player, playerDiscoveries.size, allFishData.size, page, totalPages)
    }

    private fun createDiscoveryItem(fishData: FishData, isDiscovered: Boolean, record: FishRecord?): ItemStack {
        val item = if (isDiscovered) {
            ItemStack(fishData.material)
        } else {
            ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        }

        val meta = item.itemMeta!!

        if (isDiscovered) {
            // Show discovered fish info
            meta.setDisplayName("§a✓ ${fishData.displayName}")

            val lore = mutableListOf<String>()
            lore.add("§7Status: §aDiscovered!")
            lore.add("§7Rarity: ${fishData.rarity}")
            lore.add("§7${fishData.description}")
            lore.add("")

            if (record != null) {
                lore.add("§6§lCurrent Record:")
                lore.add("§7Holder: §f${record.playerName}")
                lore.add("§7Size: §f${String.format("%.1f", record.length)} ${record.lengthUnit}")

                val weightDisplay = if (record.weightUnit == "g" || record.weightUnit == "oz") {
                    String.format("%.0f", record.weight)
                } else {
                    String.format("%.2f", record.weight)
                }
                lore.add("§7Weight: §f$weightDisplay ${record.weightUnit}")
                lore.add("§7Date: §f${record.timestamp}")
                lore.add("§7Difficulty: §f${record.difficulty}")
            } else {
                lore.add("§7No records yet!")
                lore.add("§7Be the first to set a record!")
            }

            // Add value estimation
            lore.add("")
            val valueRange = getEstimatedValue(fishData.rarity)
            lore.add("§7Estimated Value: §6$valueRange")

            meta.lore = lore

        } else {
            // Show undiscovered fish as mystery
            meta.setDisplayName("§8§l??? Unknown Creature")
            val mysteryLore = mutableListOf<String>()
            mysteryLore.add("§7Status: §cNot Discovered")
            mysteryLore.add("§7Rarity: §8???")
            mysteryLore.add("")
            mysteryLore.add("§7Go fishing to discover")
            mysteryLore.add("§7this mysterious creature!")
            mysteryLore.add("")
            mysteryLore.add("§8Hint: ${fishData.description}")

            meta.lore = mysteryLore
        }

        item.itemMeta = meta
        return item
    }

    private fun getEstimatedValue(rarity: String): String {
        return when {
            rarity.contains("Common") -> "10-30 coins"
            rarity.contains("Uncommon") -> "25-60 coins"
            rarity.contains("Rare") -> "80-200 coins"
            rarity.contains("Epic") -> "150-400 coins"
            rarity.contains("Treasure") && !rarity.contains("Legendary") -> "50-150 coins"
            rarity.contains("Valuable Treasure") -> "100-300 coins"
            rarity.contains("Rare Treasure") -> "200-500 coins"
            rarity.contains("Legendary") -> "400-1000+ coins"
            else -> "Unknown"
        }
    }

    private fun setupUIElements(inventory: Inventory, player: Player, discovered: Int, total: Int, currentPage: Int, totalPages: Int) {
        // Progress info (center bottom)
        val progressItem = ItemStack(Material.BOOK)
        val progressMeta = progressItem.itemMeta!!
        progressMeta.setDisplayName("§6§lYour Discovery Progress")
        val percentage = if (total > 0) (discovered.toDouble() / total * 100).toInt() else 0
        progressMeta.lore = listOf(
            "§7Discovered: §a$discovered§7/§e$total",
            "§7Progress: §6$percentage%",
            "§7Page: §e${currentPage + 1}§7/§e$totalPages",
            "",
            "§7Keep fishing to discover more!",
            "",
            "§aDiscovery Rewards:",
            "§7• Experience bonuses",
            "§7• Unlock rare fish",
            "§7• Bragging rights!",
            "",
            "§7Your Rank: ${getPlayerRanking(percentage)}"
        )
        progressItem.itemMeta = progressMeta
        inventory.setItem(49, progressItem)

        // Previous page button
        if (currentPage > 0) {
            val prevItem = ItemStack(Material.ARROW)
            val prevMeta = prevItem.itemMeta!!
            prevMeta.setDisplayName("§e§lPrevious Page")
            prevMeta.lore = listOf("§7Click to go to page §e$currentPage")
            prevItem.itemMeta = prevMeta
            inventory.setItem(45, prevItem)
        } else {
            val fillerItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
            val fillerMeta = fillerItem.itemMeta!!
            fillerMeta.setDisplayName(" ")
            fillerItem.itemMeta = fillerMeta
            inventory.setItem(45, fillerItem)
        }

        // Next page button
        if (currentPage < totalPages - 1) {
            val nextItem = ItemStack(Material.ARROW)
            val nextMeta = nextItem.itemMeta!!
            nextMeta.setDisplayName("§e§lNext Page")
            nextMeta.lore = listOf("§7Click to go to page §e${currentPage + 2}")
            nextItem.itemMeta = nextMeta
            inventory.setItem(53, nextItem)
        } else {
            val fillerItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
            val fillerMeta = fillerItem.itemMeta!!
            fillerMeta.setDisplayName(" ")
            fillerItem.itemMeta = fillerMeta
            inventory.setItem(53, fillerItem)
        }

        // Records button
        val recordsItem = ItemStack(Material.GOLDEN_HELMET)
        val recordsMeta = recordsItem.itemMeta!!
        recordsMeta.setDisplayName("§6§lFishing Records")
        recordsMeta.lore = listOf(
            "§7View all current fishing records",
            "§7and see who holds the biggest catches!",
            "",
            "§eClick to view your records"
        )
        recordsItem.itemMeta = recordsMeta
        inventory.setItem(46, recordsItem)

        // Statistics button
        val statsItem = ItemStack(Material.WRITABLE_BOOK)
        val statsMeta = statsItem.itemMeta!!
        statsMeta.setDisplayName("§b§lYour Statistics")

        val playerRecordCount = recordsManager.getAllRecords().values.count { it.playerUUID == player.uniqueId.toString() }
        statsMeta.lore = listOf(
            "§7Your fishing achievements:",
            "§7Discoveries: §a$discovered§7/§e$total",
            "§7Records held: §6$playerRecordCount",
            "§7Progress: §6$percentage%",
            "",
            "§eClick for detailed stats"
        )
        statsItem.itemMeta = statsMeta
        inventory.setItem(47, statsItem)

        // Close button
        val closeItem = ItemStack(Material.BARRIER)
        val closeMeta = closeItem.itemMeta!!
        closeMeta.setDisplayName("§c§lClose Discovery")
        closeMeta.lore = listOf("§7Click to close this menu")
        closeItem.itemMeta = closeMeta
        inventory.setItem(52, closeItem)

        // Sorting button
        val sortItem = ItemStack(Material.HOPPER)
        val sortMeta = sortItem.itemMeta!!
        sortMeta.setDisplayName("§d§lSort Options")
        sortMeta.lore = listOf(
            "§7Current: §eBy Rarity",
            "",
            "§7Available sorts:",
            "§7• By Rarity (default)",
            "§7• By Discovery Status",
            "§7• Alphabetical",
            "",
            "§eClick to change sorting"
        )
        sortItem.itemMeta = sortMeta
        inventory.setItem(48, sortItem)

        // Decorative glass panes
        val glass = ItemStack(Material.BLUE_STAINED_GLASS_PANE)
        val glassMeta = glass.itemMeta!!
        glassMeta.setDisplayName(" ")
        glass.itemMeta = glassMeta

        // Fill empty UI slots with glass
        val uiSlots = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 50, 51)
        for (slot in uiSlots) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, glass)
            }
        }
    }

    private fun getPlayerRanking(percentage: Int): String {
        return when {
            percentage >= 90 -> "§6Master Explorer"
            percentage >= 70 -> "§eExperienced Angler"
            percentage >= 50 -> "§aSkilled Fisher"
            percentage >= 30 -> "§7Dedicated Angler"
            percentage >= 10 -> "§8Novice Explorer"
            else -> "§8Beginner"
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        if (event.view.title == menuTitle) {
            event.isCancelled = true

            val clickedItem = event.currentItem ?: return
            val playerPage = currentPage[player.uniqueId] ?: 0

            when (event.slot) {
                45 -> {
                    // Previous page
                    if (playerPage > 0) {
                        val newPage = playerPage - 1
                        currentPage[player.uniqueId] = newPage
                        setupDiscoveryMenu(player, event.inventory, newPage)
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f)
                    }
                }
                53 -> {
                    // Next page
                    val totalPages = (allFishData.size + 27) / 28
                    if (playerPage < totalPages - 1) {
                        val newPage = playerPage + 1
                        currentPage[player.uniqueId] = newPage
                        setupDiscoveryMenu(player, event.inventory, newPage)
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1.2f)
                    }
                }
                52 -> {
                    // Close button
                    player.closeInventory()
                    player.playSound(player.location, Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f)
                }
                49 -> {
                    // Progress button - show progress details
                    showProgressDetails(player)
                }
                46 -> {
                    // Records button - show player's records
                    showPlayerRecords(player)
                }
                47 -> {
                    // Statistics button - show detailed statistics
                    showDetailedStatistics(player)
                }
                48 -> {
                    // Sort button - future implementation
                    player.sendMessage("§6Sorting options coming soon!")
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_TRADE, 0.7f, 1.0f)
                }
                else -> {
                    // Fish discovery slots
                    if (clickedItem.hasItemMeta()) {
                        val displayName = clickedItem.itemMeta?.displayName ?: ""
                        if (displayName.contains("✓")) {
                            player.sendMessage("§aYou have discovered this creature! Check your records for more details.")
                        } else if (displayName.contains("???")) {
                            player.sendMessage("§7This creature remains a mystery... Keep fishing to discover it!")
                            val lore = clickedItem.itemMeta?.lore
                            if (lore != null && lore.size > 6) {
                                player.sendMessage(lore[6]) // Show hint
                            }
                        }
                        player.playSound(player.location, Sound.ENTITY_FISH_SWIM, 0.5f, 1.5f)
                    }
                }
            }
        }
    }

    private fun showProgressDetails(player: Player) {
        val discoveries = recordsManager.getPlayerDiscoveries(player.uniqueId)
        val percentage = if (allFishData.isNotEmpty()) (discoveries.size.toDouble() / allFishData.size * 100).toInt() else 0

        player.sendMessage("§6=== Your Discovery Progress ===")
        player.sendMessage("§7Total discoveries: §a${discoveries.size}§7/§e${allFishData.size} §7(§6$percentage%§7)")
        player.sendMessage("§7Rank: ${getPlayerRanking(percentage)}")

        if (discoveries.isNotEmpty()) {
            player.sendMessage("§7Recent discoveries:")
            discoveries.take(5).forEach { fishType ->
                val fishData = allFishData.find { it.fishType == fishType }
                val displayName = fishData?.displayName?.replace("§[0-9a-fk-or]".toRegex(), "") ?: fishType
                player.sendMessage("§7• §f$displayName")
            }
            if (discoveries.size > 5) {
                player.sendMessage("§7... and ${discoveries.size - 5} more!")
            }
        } else {
            player.sendMessage("§7No discoveries yet. Start fishing to begin your collection!")
        }

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f)
    }

    private fun showPlayerRecords(player: Player) {
        val allRecords = recordsManager.getAllRecords()
        val playerRecords = allRecords.filter { it.value.playerUUID == player.uniqueId.toString() }

        if (playerRecords.isEmpty()) {
            player.sendMessage("§7You don't hold any fishing records yet.")
            player.sendMessage("§7Keep fishing larger specimens to set new records!")
        } else {
            player.sendMessage("§6=== Your Fishing Records ===")
            playerRecords.forEach { (fishType, record) ->
                val fishData = allFishData.find { it.fishType == fishType }
                val fishName = fishData?.displayName?.replace("§[0-9a-fk-or]".toRegex(), "") ?: fishType
                player.sendMessage("§7• §f$fishName: §6${String.format("%.1f", record.length)} ${record.lengthUnit}")
            }
        }

        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f)
    }

    private fun showDetailedStatistics(player: Player) {
        val discoveries = recordsManager.getPlayerDiscoveries(player.uniqueId)
        val allRecords = recordsManager.getAllRecords()
        val playerRecords = allRecords.filter { it.value.playerUUID == player.uniqueId.toString() }

        // Count discoveries by rarity
        val rarityCount = mutableMapOf<String, Int>()
        discoveries.forEach { fishType ->
            val fishData = allFishData.find { it.fishType == fishType }
            if (fishData != null) {
                val cleanRarity = fishData.rarity.replace("§[0-9a-fk-or]".toRegex(), "")
                rarityCount[cleanRarity] = rarityCount.getOrDefault(cleanRarity, 0) + 1
            }
        }

        player.sendMessage("§b=== Detailed Statistics ===")
        player.sendMessage("§7Total discoveries: §a${discoveries.size}")
        player.sendMessage("§7Records held: §6${playerRecords.size}")
        player.sendMessage("")
        player.sendMessage("§7Discoveries by rarity:")

        val rarityOrder = listOf("Common", "Uncommon", "Rare", "Epic", "Treasure", "Valuable Treasure", "Rare Treasure", "Legendary Treasure")
        for (rarity in rarityOrder) {
            val count = rarityCount[rarity] ?: 0
            if (count > 0) {
                val color = when (rarity) {
                    "Common" -> "§a"
                    "Uncommon" -> "§2"
                    "Rare" -> "§6"
                    "Epic" -> "§5"
                    "Treasure" -> "§e"
                    "Valuable Treasure" -> "§6"
                    "Rare Treasure" -> "§b"
                    "Legendary Treasure" -> "§d"
                    else -> "§7"
                }
                player.sendMessage("§7• $color$rarity: §f$count")
            }
        }

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f)
    }
}
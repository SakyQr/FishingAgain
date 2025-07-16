package org.SakyQ.fishingAgain

import net.milkbowl.vault.economy.Economy
import org.SakyQ.fishingAgain.GUI.DiscoveryGUI
import org.SakyQ.fishingAgain.GUI.SellMenu
import org.SakyQ.fishingAgain.manager.*
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class FishingAgain : JavaPlugin(), Listener {
    private val configVersion = 1.0
    private lateinit var localeManager: LocaleManager
    private lateinit var fishingManager: FishingManager
    private lateinit var experienceManager: ExperienceManager
    private lateinit var fishingCommands: FishingCommands
    private lateinit var sellMenu: SellMenu
    private lateinit var fishStatsManager: FishStatsManager
    private lateinit var recordsManager: FishingRecordsManager
    private lateinit var discoveryGUI: DiscoveryGUI
    private var economy: Economy? = null
    private val fishingStatus = mutableSetOf<UUID>()
    private val skillCheckTasks = mutableMapOf<UUID, SkillCheckTask>()

    override fun onEnable() {
        server.logger.info("[FishingAgain] Hello, world!")

        // Setup Vault economy
        if (!setupEconomy()) {
            logger.warning("Vault economy not found! Sell menu will be disabled.")
        }

        // Initialize managers
        localeManager = LocaleManager(this)
        fishingManager = FishingManager(this)
        experienceManager = ExperienceManager(this)
        fishStatsManager = FishStatsManager(this)
        recordsManager = FishingRecordsManager(this)
        discoveryGUI = DiscoveryGUI(this, recordsManager, fishingManager)
        sellMenu = SellMenu(this, economy, localeManager, fishStatsManager)
        fishingCommands = FishingCommands(this, localeManager, experienceManager, sellMenu, discoveryGUI)

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this)

        // Register commands
        getCommand("fishing")?.setExecutor(fishingCommands)
        getCommand("fishing")?.tabCompleter = fishingCommands

        // Setup config
        saveDefaultConfig()
        updateConfig()

        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }

        val rsp: RegisteredServiceProvider<Economy>? = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }

        economy = rsp.provider
        return economy != null
    }


    private fun updateConfig() {
        val config: FileConfiguration = config
        val currentConfigVersion = config.getDouble("config-version", 1.0)

        if (currentConfigVersion < configVersion) {
            logger.info("Updated config up to $configVersion")
            config.set("config-version", configVersion)
            saveConfig()
        }
    }

    override fun onDisable() {
        server.logger.info("[FishingAgain] Bye, world!")
        // Save player data before shutdown, but only if initialized
        if (::experienceManager.isInitialized) {
            experienceManager.onDisable()
        }
    }

    @EventHandler
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player
        val playerId = player.uniqueId

        when (event.state) {
            PlayerFishEvent.State.CAUGHT_FISH -> {
                event.isCancelled = true
                val caught = event.caught
                if (caught is Item) {
                    val caughtItem = caught.itemStack
                    if (!skillCheckTasks.containsKey(playerId)) {
                        if (!fishingStatus.contains(playerId)) {
                            fishingStatus.add(playerId)
                            startSkillCheck(player, caughtItem)
                        }
                    }
                }
            }
            PlayerFishEvent.State.BITE -> {
                if (fishingStatus.contains(playerId)) {
                    fishingStatus.remove(playerId)
                }
            }
            else -> {}
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action.name.contains("RIGHT")) {
            val task = skillCheckTasks[player.uniqueId]
            task?.checkInput()
        }
    }

    private fun startSkillCheck(player: Player, caughtItem: ItemStack) {
        // Get custom item or use default
        val finalItem = fishingManager.getCustomItem(caughtItem) ?: caughtItem
        val difficulty = fishingManager.getFishingDifficulty(finalItem)

        val task = SkillCheckTask(
            this,
            player,
            finalItem,
            localeManager,
            experienceManager,
            difficulty,  // Fixed: difficulty comes before fishStatsManager
            fishStatsManager  // Fixed: fishStatsManager comes last
        )
        skillCheckTasks[player.uniqueId] = task
        task.runTaskTimer(this, 0L, 1L)
    }

    fun removeSkillCheckTask(playerId: UUID) {
        skillCheckTasks.remove(playerId)
    }

    fun getRecordsManager(): FishingRecordsManager {
        return recordsManager
    }

    fun reloadSellMenu() {
        if (::sellMenu.isInitialized) {
            sellMenu.reloadConfiguration()
        }
    }
}
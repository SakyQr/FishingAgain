package org.SakyQ.fishingAgain

import org.SakyQ.fishingAgain.manager.ExperienceManager
import org.SakyQ.fishingAgain.manager.FishStatsManager
import org.SakyQ.fishingAgain.manager.FishingDifficulty
import org.SakyQ.fishingAgain.manager.LocaleManager
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class SkillCheckTask(
    private val plugin: FishingAgain,
    private val player: Player,
    private val caughtItem: ItemStack,
    private val localeManager: LocaleManager,
    private val experienceManager: ExperienceManager,
    private val difficulty: FishingDifficulty,
    private val fishStatsManager: FishStatsManager
) : BukkitRunnable() {

    private var cursorPos = 0
    private var direction = 1
    private val barLength = 50
    private val successStart: Int
    private val successEnd: Int
    private var isActive = true
    private var currentSpeed = difficulty.speed
    private var ticksElapsed = 0
    private val maxTicks = difficulty.timeLimit
    private var hasStartedMoving = false

    init {
        val successZoneLength = (barLength * difficulty.successZoneSize).toInt().coerceAtLeast(3)
        successStart = Random.nextInt(0, (barLength - successZoneLength).coerceAtLeast(1))
        successEnd = (successStart + successZoneLength - 1).coerceAtMost(barLength - 1)

        // Send initial warning message for difficult fish
        sendDifficultyWarning()

        // Give player a moment to prepare
        player.sendMessage("§e⚡ Get ready! Right-click when the cursor is in the §a§lGREEN§e zone!")
    }

    private fun sendDifficultyWarning() {
        when (difficulty.name.lowercase()) {
            "easy" -> player.sendMessage("§a🎣 A gentle fish has taken your bait...")
            "medium" -> player.sendMessage("§e🎣 This fish has some fight in it!")
            "hard" -> player.sendMessage("§6⚠ This fish is putting up a strong fight!")
            "legendary" -> {
                player.sendMessage("§5⚠ §lA LEGENDARY CREATURE RESISTS YOUR LINE!")
                player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f)
            }
            "mythical" -> {
                player.sendMessage("§d⚠ §lSOMETHING MYTHICAL HAS TAKEN THE BAIT!")
                player.playSound(player.location, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.2f)
            }
            "godlike" -> {
                player.sendMessage("§4⚠ §l§nDIVINE POWER AWAKENS!§r §4⚠")
                player.sendMessage("§4§l⬢ THE GODS THEMSELVES CHALLENGE YOU! ⬢")
                player.playSound(player.location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.5f)
            }
        }
    }

    override fun run() {
        if (!isActive) return

        ticksElapsed++

        // Give player 1 second to prepare before starting
        if (ticksElapsed <= 20) {
            val countdown = (21 - ticksElapsed) / 20.0
            val difficultyColor = getDifficultyColor()
            player.sendActionBar("$difficultyColor${difficulty.name} §7- Starting in §e${String.format("%.1f", countdown)}s")
            return
        }

        if (!hasStartedMoving) {
            hasStartedMoving = true
            player.sendMessage("§e⚡ GO! Right-click to catch!")
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 2.0f)
        }

        // Check time limit (accounting for preparation time)
        if (ticksElapsed >= maxTicks + 20) {
            timeOut()
            return
        }

        // Apply acceleration for harder difficulties
        if (difficulty.cursorAcceleration && ticksElapsed > 40) {
            val accelerationFactor = 1 + ((ticksElapsed - 40) / 100.0) // Gradually increase speed after 2 seconds
            currentSpeed = (difficulty.speed * accelerationFactor).toInt().coerceAtMost(10)
        }

        // Move cursor based on current speed - much simpler and faster
        cursorPos += direction * currentSpeed

        // Bounce off edges
        if (cursorPos >= barLength - 1) {
            cursorPos = barLength - 1
            direction = -1
        } else if (cursorPos <= 0) {
            cursorPos = 0
            direction = 1
        }

        // Build the visual bar
        val bar = buildActionBar()

        // Calculate time remaining
        val totalTime = maxTicks / 20.0
        val timeElapsed = (ticksElapsed - 20) / 20.0
        val timeLeft = (totalTime - timeElapsed).coerceAtLeast(0.0)

        val difficultyColor = getDifficultyColor()
        val speedIndicator = if (difficulty.cursorAcceleration && currentSpeed > difficulty.speed) " §c⚡" else ""
        val accuracyHint = if (cursorPos in successStart..successEnd) " §a§l[PERFECT!]" else ""

        player.sendActionBar("$difficultyColor${difficulty.name}$speedIndicator §7[${String.format("%.1f", timeLeft)}s] $bar$accuracyHint")
    }

    private fun buildActionBar(): String {
        val bar = StringBuilder()

        for (i in 0 until barLength) {
            when {
                i == cursorPos -> {
                    // Cursor with different styles based on difficulty
                    val cursor = when (difficulty.name.lowercase()) {
                        "easy" -> "§f▼"
                        "medium" -> "§e▼"
                        "hard" -> "§6⚡"
                        "legendary" -> "§5★"
                        "mythical" -> "§d♦"
                        "godlike" -> "§4◆"
                        else -> "§f▼"
                    }
                    bar.append(cursor)
                }
                i in successStart..successEnd -> {
                    // Success zone - different colors based on difficulty
                    val successColor = when (difficulty.name.lowercase()) {
                        "easy", "medium" -> "§a"
                        "hard" -> "§2"
                        "legendary" -> "§a"
                        "mythical" -> "§a"
                        "godlike" -> "§a"
                        else -> "§a"
                    }
                    bar.append("${successColor}█")
                }
                else -> {
                    // Failure zone
                    bar.append("§c█")
                }
            }
        }

        return bar.toString()
    }

    private fun getDifficultyColor(): String {
        return when (difficulty.name.lowercase()) {
            "easy" -> "§a"
            "medium" -> "§e"
            "hard" -> "§6"
            "legendary" -> "§5"
            "mythical" -> "§d"
            "godlike" -> "§4"
            else -> "§f"
        }
    }

    private fun timeOut() {
        if (!isActive) return

        isActive = false
        cancel()

        // Time out failure with appropriate messages
        val failSound = when (difficulty.name.lowercase()) {
            "legendary", "mythical", "godlike" -> Sound.ENTITY_WITHER_DEATH
            else -> Sound.BLOCK_ANVIL_LAND
        }

        player.playSound(player.location, failSound, 0.5f, 0.5f)

        val timeoutMessage = when (difficulty.name.lowercase()) {
            "legendary" -> "§5⏰ The legendary creature escaped into the depths..."
            "mythical" -> "§d⏰ The mythical being vanished in a shimmer of light..."
            "godlike" -> "§4⏰ §lThe divine essence returned to the cosmos..."
            else -> {
                val fishName = caughtItem.itemMeta?.displayName?.replace("§[0-9a-fk-or]".toRegex(), "")
                    ?: caughtItem.type.name.lowercase().replace('_', ' ')
                "§c⏰ Time's up! The $fishName got away!"
            }
        }

        player.sendMessage(timeoutMessage)
        player.sendActionBar("§c§lFAILED! §c- Time ran out")

        plugin.removeSkillCheckTask(player.uniqueId)
    }

    fun checkInput() {
        if (!isActive) return
        if (!hasStartedMoving) {
            player.sendMessage("§c⚠ Wait for the fishing challenge to start!")
            return
        }

        // Prevent spam clicking
        isActive = false
        cancel()

        val isSuccess = cursorPos in successStart..successEnd

        if (isSuccess) {
            handleSuccess()
        } else {
            handleFailure()
        }

        // Always remove from tracking
        plugin.removeSkillCheckTask(player.uniqueId)
    }

    private fun handleSuccess() {
        // Play success sound based on difficulty
        val successSound = when (difficulty.name.lowercase()) {
            "easy" -> Sound.ENTITY_PLAYER_LEVELUP
            "medium" -> Sound.UI_TOAST_CHALLENGE_COMPLETE
            "hard" -> Sound.ENTITY_ENDER_DRAGON_DEATH
            "legendary" -> Sound.ENTITY_LIGHTNING_BOLT_IMPACT
            "mythical" -> Sound.ENTITY_WITHER_SPAWN
            "godlike" -> Sound.ENTITY_LIGHTNING_BOLT_THUNDER
            else -> Sound.ENTITY_PLAYER_LEVELUP
        }

        player.playSound(player.location, successSound, 1.0f, 1.0f)

        // Generate fish with proper stats
        val fishKey = identifyFishKey(caughtItem)
        val finalItemWithStats = if (fishKey != null) {
            fishStatsManager.generateFishStats(fishKey, caughtItem)
        } else {
            // Fallback: still generate stats using material type
            fishStatsManager.generateFishStats(caughtItem.type.name, caughtItem)
        }

        // Add fishing metadata to mark it as a legitimate catch
        val finalMeta = finalItemWithStats.itemMeta
        if (finalMeta != null) {
            finalMeta.persistentDataContainer.set(
                org.bukkit.NamespacedKey(plugin, "fishing_catch"),
                org.bukkit.persistence.PersistentDataType.STRING,
                "true"
            )

            // Store catch difficulty
            finalMeta.persistentDataContainer.set(
                org.bukkit.NamespacedKey(plugin, "catch_difficulty"),
                org.bukkit.persistence.PersistentDataType.STRING,
                difficulty.name
            )

            finalItemWithStats.itemMeta = finalMeta
        }

        // Display success messages
        val fishName = finalItemWithStats.itemMeta?.displayName ?: finalItemWithStats.type.name
        val successMessage = when (difficulty.name.lowercase()) {
            "easy" -> "§a🎣 Nice catch! You landed a $fishName!"
            "medium" -> "§e🎣 Great job! You caught a $fishName!"
            "hard" -> "§6🎣 Excellent! You fought hard for that $fishName!"
            "legendary" -> "§5⭐ LEGENDARY CATCH! §5You've conquered a $fishName!"
            "mythical" -> "§d✦ MYTHICAL ACHIEVEMENT! §dThe seas bow to your skill with this $fishName!"
            "godlike" -> "§4§l⬢ GODLIKE MASTERY! ⬢ §4§lYou have achieved the impossible with this $fishName!"
            else -> "§a🎣 You caught a $fishName!"
        }

        player.sendMessage(successMessage)
        player.sendActionBar("§a§lSUCCESS! §a- Perfect timing!")

        // Get and display fish stats
        val fishStats = fishStatsManager.getFishStats(finalItemWithStats)
        if (fishStats != null) {
            val lengthDisplay = String.format("%.1f", fishStats.length)
            val weightDisplay = if (fishStats.weightUnit == "g" || fishStats.weightUnit == "oz") {
                String.format("%.0f", fishStats.weight)
            } else {
                String.format("%.2f", fishStats.weight)
            }

            player.sendMessage("§b⚖ Size: §f$lengthDisplay ${fishStats.lengthUnit}, §f$weightDisplay ${fishStats.weightUnit}")

            // Show size quality from lore
            val lore = finalItemWithStats.itemMeta?.lore
            if (lore != null) {
                for (line in lore) {
                    if (line.contains("Grade:")) {
                        player.sendMessage("§b⚖ $line")
                        break
                    }
                }
            }

            // Record the catch for records and discoveries
            plugin.getRecordsManager().recordCatch(player, finalItemWithStats, fishStats, difficulty.name, fishStatsManager)
        }

        // Add item to inventory
        val leftoverItems = player.inventory.addItem(finalItemWithStats)
        if (leftoverItems.isNotEmpty()) {
            // Drop items that don't fit
            leftoverItems.values.forEach { leftover ->
                player.world.dropItemNaturally(player.location, leftover)
            }
            player.sendMessage("§6Some items were dropped because your inventory is full!")
        }

        // Give experience based on difficulty
        val baseExp = plugin.config.getInt("fishing.base-experience", 10)
        val expReward = (baseExp * difficulty.experienceMultiplier).toInt()
        experienceManager.giveExperience(player, expReward)

        // Special rewards for high difficulty catches
        giveSpecialRewards(difficulty.name)
    }

    private fun handleFailure() {
        // Play failure sound
        val failSound = when (difficulty.name.lowercase()) {
            "legendary", "mythical", "godlike" -> Sound.ENTITY_WITHER_HURT
            else -> Sound.ENTITY_VILLAGER_NO
        }

        player.playSound(player.location, failSound, 1.0f, 1.0f)

        val failMessage = when (difficulty.name.lowercase()) {
            "easy" -> "§c🎣 The fish got away! Try to time it better next time."
            "medium" -> "§c🎣 So close! The fish slipped off the hook."
            "hard" -> "§c🎣 The fish fought back and escaped!"
            "legendary" -> "§5💫 The legendary creature slipped away into the depths..."
            "mythical" -> "§d✨ The mythical being vanished in a shimmer of light..."
            "godlike" -> "§4§l⚡ The divine essence returned to the cosmos..."
            else -> "§c🎣 The fish escaped!"
        }

        player.sendMessage(failMessage)
        player.sendActionBar("§c§lMISSED! §c- Try again next time")

        // Give small consolation experience for higher difficulties
        if (difficulty.experienceMultiplier >= 2.0) {
            val consolationExp = (plugin.config.getInt("fishing.base-experience", 10) * 0.2).toInt()
            if (consolationExp > 0) {
                experienceManager.giveExperience(player, consolationExp)
                player.sendMessage("§7You gained §a$consolationExp EXP §7for the attempt!")
            }
        }
    }

    private fun giveSpecialRewards(difficulty: String) {
        when (difficulty.lowercase()) {
            "legendary" -> {
                // Small chance for bonus items
                if (Random.nextDouble() < 0.1) {
                    val bonusItem = ItemStack(org.bukkit.Material.EXPERIENCE_BOTTLE, Random.nextInt(1, 4))
                    player.inventory.addItem(bonusItem)
                    player.sendMessage("§5✨ Bonus reward: Experience bottles!")
                }
            }
            "mythical" -> {
                // Better bonus chance
                if (Random.nextDouble() < 0.15) {
                    val bonusItem = ItemStack(org.bukkit.Material.EMERALD, Random.nextInt(1, 3))
                    player.inventory.addItem(bonusItem)
                    player.sendMessage("§d✨ Bonus reward: Emeralds!")
                }
            }
            "godlike" -> {
                // Guaranteed bonus
                val bonusItem = ItemStack(org.bukkit.Material.DIAMOND, Random.nextInt(1, 3))
                player.inventory.addItem(bonusItem)
                player.sendMessage("§4✨ Divine reward: Diamonds!")
            }
        }
    }

    private fun identifyFishKey(itemStack: ItemStack): String? {
        val displayName = itemStack.itemMeta?.displayName

        if (displayName != null) {
            // Extract fish key from display name with comprehensive matching
            val cleanName = displayName.replace("§[0-9a-fk-or]".toRegex(), "").lowercase()
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
                else -> null
            }
        }

        // Fallback to material type
        return itemStack.type.name
    }
}
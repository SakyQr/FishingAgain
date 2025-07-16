package org.SakyQ.fishingAgain

import org.SakyQ.fishingAgain.GUI.DiscoveryGUI
import org.SakyQ.fishingAgain.GUI.SellMenu
import org.SakyQ.fishingAgain.manager.ExperienceManager
import org.SakyQ.fishingAgain.manager.LocaleManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class FishingCommands(
    private val plugin: FishingAgain,
    private val localeManager: LocaleManager,
    private val experienceManager: ExperienceManager,
    private val sellMenu: SellMenu,
    private val discoveryGUI: DiscoveryGUI
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("fishing", ignoreCase = true)) {
            when (args.getOrNull(0)?.lowercase()) {
                "stats", "level" -> {
                    if (sender !is Player) {
                        sender.sendMessage("§cThis command can only be used by players!")
                        return true
                    }
                    showStats(sender)
                }
                "top", "leaderboard" -> {
                    showLeaderboard(sender)
                }
                "sell", "market" -> {
                    if (sender !is Player) {
                        sender.sendMessage("§cThis command can only be used by players!")
                        return true
                    }
                    sellMenu.openSellMenu(sender)
                }
                "discover", "discovery", "collection" -> {
                    if (sender !is Player) {
                        sender.sendMessage("§cThis command can only be used by players!")
                        return true
                    }
                    discoveryGUI.openDiscoveryMenu(sender)
                }
                "reload" -> {
                    if (!sender.hasPermission("fishingagain.reload")) {
                        sender.sendMessage("§cYou don't have permission to use this command!")
                        return true
                    }
                    reloadPlugin(sender)
                }
                "help", null -> {
                    showHelp(sender)
                }
                else -> {
                    sender.sendMessage("§cUnknown subcommand. Use /fishing help for available commands.")
                }
            }
            return true
        }
        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (command.name.equals("fishing", ignoreCase = true)) {
            if (args.size == 1) {
                val subcommands = mutableListOf("stats", "top", "sell", "discover", "help")
                if (sender.hasPermission("fishingagain.reload")) {
                    subcommands.add("reload")
                }
                return subcommands.filter { it.startsWith(args[0].lowercase()) }
            }
        }
        return emptyList()
    }

    private fun showStats(player: Player) {
        val fishingLevel = experienceManager.getPlayerLevel(player)

        player.sendMessage(localeManager.getMessage(player, "fishing_stats"))
        player.sendMessage(localeManager.getMessage(player, "fishing_level", fishingLevel.level))
        player.sendMessage(localeManager.getMessage(player, "fishing_experience", fishingLevel.experience, fishingLevel.experienceToNext))

        // Show progress bar
        val totalExpForLevel = fishingLevel.experience + fishingLevel.experienceToNext
        val progress = (fishingLevel.experience.toDouble() / totalExpForLevel * 20).toInt()
        val progressBar = StringBuilder("§7[")
        for (i in 0 until 20) {
            if (i < progress) {
                progressBar.append("§a█")
            } else {
                progressBar.append("§8█")
            }
        }
        progressBar.append("§7]")
        player.sendMessage(progressBar.toString())
    }

    private fun showLeaderboard(sender: CommandSender) {
        val topPlayers = experienceManager.getTopPlayers(10)

        if (sender is Player) {
            sender.sendMessage(localeManager.getMessage(sender, "fishing_top_header"))
        } else {
            sender.sendMessage("§6=== Top Fishers ===")
        }

        topPlayers.forEachIndexed { index, (playerName, fishingLevel) ->
            val message = if (sender is Player) {
                localeManager.getMessage(sender, "fishing_top_entry", index + 1, playerName, fishingLevel.level)
            } else {
                "§7${index + 1}. §6$playerName §7- Level ${fishingLevel.level}"
            }
            sender.sendMessage(message)
        }

        if (topPlayers.isEmpty()) {
            sender.sendMessage("§7No fishing data available yet.")
        }
    }

    private fun reloadPlugin(sender: CommandSender) {
        try {
            plugin.reloadConfig()
            plugin.reloadSellMenu()
            experienceManager.savePlayerData()
            sender.sendMessage("§aFishingAgain plugin reloaded successfully!")
        } catch (e: Exception) {
            sender.sendMessage("§cFailed to reload plugin: ${e.message}")
            plugin.logger.severe("Failed to reload plugin: ${e.message}")
        }
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§6=== FishingAgain Commands ===")
        sender.sendMessage("§7/fishing stats §8- §fShow your fishing level and experience")
        sender.sendMessage("§7/fishing top §8- §fShow the top fishers leaderboard")
        sender.sendMessage("§7/fishing sell §8- §fOpen the fish market to sell your catch")
        sender.sendMessage("§7/fishing discover §8- §fView your fish discovery collection")
        sender.sendMessage("§7/fishing help §8- §fShow this help message")

        if (sender.hasPermission("fishingagain.reload")) {
            sender.sendMessage("§7/fishing reload §8- §fReload the plugin configuration")
        }
    }
}
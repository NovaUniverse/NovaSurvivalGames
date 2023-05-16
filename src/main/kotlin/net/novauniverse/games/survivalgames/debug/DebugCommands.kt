package net.novauniverse.games.survivalgames.debug

import net.md_5.bungee.api.ChatColor
import net.novauniverse.games.survivalgames.SurvivalGamesPlugin.Companion.getInstance
import net.zeeraa.novacore.spigot.command.AllowedSenders
import net.zeeraa.novacore.spigot.debug.DebugCommandRegistrator
import net.zeeraa.novacore.spigot.debug.DebugTrigger
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault


class DebugCommands {
    init {
        DebugCommandRegistrator.getInstance().addDebugTrigger(object : DebugTrigger {
            override fun onExecute(sender: CommandSender, commandLabel: String, args: Array<String>) {
                if (getInstance().game!!.hasActiveMap()) {
                    getInstance().game?.setCages(true)
                    sender.sendMessage("${ChatColor.GREEN}ok")
                } else {
                    sender.sendMessage("${ChatColor.RED}No map")
                }
            }

            override fun getPermissionDefault(): PermissionDefault {
                return PermissionDefault.OP
            }

            override fun getPermission(): String {
                return "novauniverse.debug.survivalgames.spawncages"
            }

            override fun getName(): String {
                return "spawncages"
            }

            override fun getAllowedSenders(): AllowedSenders {
                return AllowedSenders.ALL
            }
        })
        DebugCommandRegistrator.getInstance().addDebugTrigger(object : DebugTrigger {
            override fun onExecute(sender: CommandSender, commandLabel: String, args: Array<String>) {
                if (getInstance().game!!.hasActiveMap()) {
                    getInstance().game?.setCages(false)
                    sender.sendMessage("${ChatColor.GREEN}ok")
                } else {
                    sender.sendMessage("${ChatColor.RED}No map")
                }
            }

            override fun getPermissionDefault(): PermissionDefault {
                return PermissionDefault.OP
            }

            override fun getPermission(): String {
                return "novauniverse.debug.survivalgames.despawncages"
            }

            override fun getName(): String {
                return "despawncages"
            }

            override fun getAllowedSenders(): AllowedSenders {
                return AllowedSenders.ALL
            }
        })
    }
}
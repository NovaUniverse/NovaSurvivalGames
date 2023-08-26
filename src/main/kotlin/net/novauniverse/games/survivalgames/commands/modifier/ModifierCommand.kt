package net.novauniverse.games.survivalgames.commands.modifier

import net.novauniverse.games.survivalgames.modifier.ModifierGUI.Companion.openModifierGUI
import net.zeeraa.novacore.spigot.command.AllowedSenders
import net.zeeraa.novacore.spigot.command.NovaCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.bukkit.plugin.Plugin

class ModifierCommand(owner: Plugin?) : NovaCommand("sgmodifiers", owner) {
    init {
        aliases = generateAliasList("modifiers")
        allowedSenders = AllowedSenders.PLAYERS
        permissionDefaultValue = PermissionDefault.OP
        permission = "survivalgames.modifier.select"
        isEmptyTabMode = true
        isFilterAutocomplete = true
        description = "Opens modifier menu"
        usage = "/sgmodifiers"
        addHelpSubCommand()
    }

    override fun execute(sender: CommandSender, command: String, args: Array<String>): Boolean {
        openModifierGUI(sender as Player)
        return true
    }
}

package net.novauniverse.games.survivalgames.modifier.selector

import net.md_5.bungee.api.ChatColor
import net.novauniverse.games.survivalgames.modifier.ModifierGUI
import net.zeeraa.novacore.spigot.module.modules.customitems.CustomItem
import net.zeeraa.novacore.spigot.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class ModifierSelectorItem: CustomItem() {
    override fun createItemStack(player: Player?): ItemStack {
        val builder = ItemBuilder(Material.NETHER_STAR)

        builder.setName("${ChatColor.GOLD.toString() + ChatColor.BOLD.toString()}Select modifiers")
        builder.addLore("${ChatColor.WHITE}Right click to select game modifiers")

        return builder.build()
    }

    override fun onPlayerDropItem(event: PlayerDropItemEvent) {
        event.isCancelled = true
    }

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        if(event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true
            if(event.player.hasPermission("survivalgames.modifier.select")) {
                ModifierGUI.openModifierGUI(event.player)
            } else {
                event.player.sendMessage("${ChatColor.RED}You dont have permission to select modifiers")
            }
        }
    }
}
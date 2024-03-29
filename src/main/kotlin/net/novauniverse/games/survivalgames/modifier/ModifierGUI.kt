package net.novauniverse.games.survivalgames.modifier

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound
import net.zeeraa.novacore.spigot.module.modules.gui.GUIAction
import net.zeeraa.novacore.spigot.module.modules.gui.holders.GUIReadOnlyHolder
import net.zeeraa.novacore.spigot.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.DyeColor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.util.concurrent.atomic.AtomicInteger

class ModifierGUI {
    companion object {
        @JvmStatic
        val SelectedModifiers = ArrayList<Modifier>()

        @JvmStatic
        fun openModifierGUI(player: Player) {
            val holder = GUIReadOnlyHolder()
            val inventory = Bukkit.createInventory(holder, 6*9, "Modifiers")

            val index = AtomicInteger(0)

            val background = ItemBuilder(ColoredBlockType.GLASS_PANE, DyeColor.WHITE)
            background.setName(" ");
            background.setAmount(1)
            for(i in 0 until inventory.size) {
                inventory.setItem(i, background.build())
            }

            SurvivalGamesPlugin.getInstance().game?.modifiers?.forEach { modifier ->
                val builder = modifier.getIconAsItemBuilder()

                builder.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                if(SelectedModifiers.contains(modifier)) {
                    builder.addEnchant(Enchantment.DURABILITY, 1)
                    builder.addEmptyLoreLine()
                    builder.addLore("${ChatColor.GREEN}Active")
                }

                inventory.setItem(index.get(), builder.build())
                holder.addClickCallback(index.get()) { _, _, _, _, _, _ ->
                    if (SelectedModifiers.contains(modifier)) {
                        SelectedModifiers.remove(modifier)
                        Bukkit.broadcast("${ChatColor.RED}${player.name} disabled modifier ${modifier.getDisplayName()}",  "survivalgames.modifier.select")
                    } else {
                        SelectedModifiers.add(modifier)
                        Bukkit.broadcast("${ChatColor.GREEN}${player.name} enabled modifier ${modifier.getDisplayName()}",  "survivalgames.modifier.select")
                    }
                    VersionIndependentSound.NOTE_PLING.play(player)
                    openModifierGUI(player)
                    return@addClickCallback GUIAction.CANCEL_INTERACTION
                }

                index.addAndGet(1)
            }

            player.openInventory(inventory)
        }
    }
}
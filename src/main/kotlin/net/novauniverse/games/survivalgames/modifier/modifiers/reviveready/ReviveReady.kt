package net.novauniverse.games.survivalgames.modifier.modifiers.reviveready

import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.spigot.module.modules.customitems.CustomItemManager
import net.novauniversee.novacore.gameengine.plus.customitem.revivecrystal.ReviveCrystalItem
import org.bukkit.Material

class ReviveReady : Modifier("SurvivalGames.ReviveReady") {
    override fun getDisplayName(): String {
        return "Revive Ready"
    }

    override fun getIconMaterial(): Material {
        return Material.NETHER_STAR
    }

    override fun getShortDescription(): String {
        return "All players gets a revive crystal once the game starts"
    }

    override fun onGameStart() {
        getGame().onlinePlayers.forEach { it.inventory.addItem(CustomItemManager.getInstance().getCustomItemStack(ReviveCrystalItem::class.java, it)) }
    }
}
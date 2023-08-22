package net.novauniverse.games.survivalgames.modifier.modifiers

import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.spigot.utils.PlayerUtils
import org.bukkit.Bukkit
import org.bukkit.Material

class SingleHeart : Modifier("SingleHeart")  {
    override fun getDisplayName(): String {
        return "Single Heart"
    }

    override fun getIconMaterial(): Material {
        return Material.REDSTONE;
    }

    override fun getShortDescription(): String {
        return "Every player will have a single heart"
    }

    override fun onGameStart() {
        Bukkit.getServer().onlinePlayers.forEach { PlayerUtils.setMaxHealth(it, 2.0) }
    }
}
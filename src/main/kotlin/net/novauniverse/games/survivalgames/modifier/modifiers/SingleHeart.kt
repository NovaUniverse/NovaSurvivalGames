package net.novauniverse.games.survivalgames.modifier.modifiers

import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodules.graceperiod.graceperiod.event.GracePeriodBeginEvent
import net.zeeraa.novacore.spigot.utils.PlayerUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class SingleHeart : Modifier("SingleHeart") {
    override fun getDisplayName(): String {
        return "Single Heart"
    }

    override fun getIconMaterial(): Material {
        return Material.REDSTONE
    }

    override fun getShortDescription(): String {
        return "Every player will have a single heart"
    }

    override fun onGameStart() {
        Bukkit.getServer().onlinePlayers.forEach { PlayerUtils.setMaxHealth(it, 2.0) }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onGracePeriodBegin(e: GracePeriodBeginEvent) {
        Log.trace("SingleHeart", e.javaClass.name)
        val gracePeriodTime = 60 * 3
        if (e.time < gracePeriodTime) {
            e.time = gracePeriodTime // Increase grace period to 3 minutes if it's less than 3 minutes
            Log.info("SingleHeart", "Increased grace period to $gracePeriodTime seconds")
        }
    }
}
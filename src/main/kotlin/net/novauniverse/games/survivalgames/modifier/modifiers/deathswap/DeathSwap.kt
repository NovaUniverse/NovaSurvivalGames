package net.novauniverse.games.survivalgames.modifier.modifiers.deathswap

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin
import net.novauniverse.games.survivalgames.modifier.Modifier
import net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider.DeathSwapProvider
import net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider.providers.DefaultSwapProvider
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.commons.tasks.Task
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound
import net.zeeraa.novacore.spigot.tasks.SimpleTask
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player

class DeathSwap : Modifier("DeathSwap") {
    private var timeLeft = 0;
    private var task: Task? = null;

    override fun getDisplayName(): String {
        return "DeathSwap"
    }

    override fun getIconMaterial(): Material {
        return Material.ENDER_PEARL
    }

    override fun getShortDescription(): String {
        return "Players will randomly switch locations during the game"
    }

    override fun onLoad() {
        timeLeft = SWAP_DELAY
        task = SimpleTask({
            if(timeLeft == SWAP_WARNING) {
                VersionIndependentSound.NOTE_PLING.broadcast()
                Bukkit.getServer().broadcastMessage("${ChatColor.RED.toString() + ChatColor.BOLD.toString()}Swapping in $timeLeft seconds")
            }

            if (timeLeft <= 0) {
                swapPlayers()
                timeLeft = SWAP_DELAY
            }

            timeLeft--;
        }, 20L)
    }

    fun swapPlayers() {
        try {
            SwapProvider.swap()
        } catch(e: Exception) {
            Log.error("DeathSwap", "Failed to swap players ${e.javaClass.name} ${e.message}")
        }
    }

    override fun onGameStart() {
        Task.tryStartTask(task)
        SwapProvider.onGameStart()
    }

    override fun onGameEnd() {
        Task.tryStopTask(task)
        SwapProvider.onGameEnd()
    }

    companion object {
        val SWAP_DELAY = 60 // 1 minute in seconds
        val SWAP_WARNING = 5 // 5 seconds

        var SwapProvider: DeathSwapProvider = DefaultSwapProvider(false)
    }
}
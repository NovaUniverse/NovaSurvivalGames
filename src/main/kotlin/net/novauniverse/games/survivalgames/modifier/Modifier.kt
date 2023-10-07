package net.novauniverse.games.survivalgames.modifier

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin
import net.novauniverse.games.survivalgames.game.SurvivalGames
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.spigot.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.Listener

abstract class Modifier(val name: String) : Listener {
    @get:JvmName("isEnabled")
    var enabled: Boolean = false
        private set

    init {
        Log.debug("Modifier", "Modifier ${name} init")
    }

    abstract fun getDisplayName(): String

    abstract fun getIconMaterial(): Material

    abstract fun getShortDescription(): String?

    fun getIconAsItemBuilder(): ItemBuilder {
        val builder = ItemBuilder(getIconMaterial())
        builder.setName(getDisplayName())
        val description = getShortDescription()

        description.let { builder.addLore(description) }

        modifyIcon(builder)

        return builder
    }

    fun getGame(): SurvivalGames {
        return SurvivalGamesPlugin.getInstance().game!!
    }

    open protected fun modifyIcon(itemBuilder: ItemBuilder) {}

    fun enable() {
        if(enabled) {
            return
        }

        Log.debug("Modifier", "Enabling modifier ${this.getDisplayName()}")

        enabled = true
        Bukkit.getServer().pluginManager.registerEvents(this, SurvivalGamesPlugin.getInstance())
        this.onEnable()
    }

    /**
     * Called when the modifier is loaded initially
     */
    open fun onLoad() {}

    open fun onEnable() {}

    /**
     * Called if the modifier is enabled in the game start function
     */
    open fun onGameStart() {}

    /**
     * Called if the modifier is enabled when the GameBeginEvent is sent
     */
    open fun onGameBegin() {}


    /**
     * Called if the modifier is enabled in the game end function
     */
    open fun onGameEnd() {}
}
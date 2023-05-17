package net.novauniverse.games.survivalgames.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class SurvivalgamesCountdownEvent(val timeLeft: Int): Event() {
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}
package net.novauniverse.games.survivalgames.map.mapmodules.countdown

import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule
import org.json.JSONObject

class SurvivalGamesCountdownConfig(json: JSONObject) : MapModule(json) {
    val initialCountdownValue: Int = json.optInt("time", 20)
}
package net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocation

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.spigot.abstraction.enums.ColoredBlockType
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModule
import net.zeeraa.novacore.spigot.utils.materialwrapper.WrappedBukkitMaterial
import net.zeeraa.novacore.spigot.utils.materialwrapper.WrappedColoredMaterial
import net.zeeraa.novacore.spigot.utils.materialwrapper.WrappedMaterial
import org.bukkit.DyeColor
import org.bukkit.Material
import org.json.JSONObject

class ExtendedSpawnLocationConfig(json: JSONObject) : MapModule(json) {
    var floorMaterial: WrappedMaterial? = null
        private set

    var keepAfterStart: Boolean = json.optBoolean("keep_after_start", false)
        private set

    var disabled: Boolean = json.optBoolean("disabled", false)
        private set

    init {
        if (json.has("floor_material")) {
            val materialString = json.getString("floor_material")
            try {
                if (materialString.startsWith("COLOREDBLOCK:")) {
                    val data = materialString.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = ColoredBlockType.valueOf(data[1])
                    val color = DyeColor.valueOf(data[2])
                    floorMaterial = WrappedColoredMaterial(color, type)
                } else {
                    floorMaterial = WrappedBukkitMaterial(Material.valueOf(materialString))
                }
            } catch (e: Exception) {
                Log.error("ExtendedSpawnLocationConfig", "Failed to parse material " + materialString + ". " + e.javaClass.name + " " + e.message)
            }
        } else {
            floorMaterial = SurvivalGamesPlugin.DEFAULT_EXTENDED_SPAWN_FLOOR_MATERIAL
        }
    }
}
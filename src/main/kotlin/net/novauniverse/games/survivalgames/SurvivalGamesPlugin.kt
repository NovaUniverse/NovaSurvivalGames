package net.novauniverse.games.survivalgames

import net.novauniverse.games.survivalgames.configuration.SurvivalGamesConfig
import net.novauniverse.games.survivalgames.debug.DebugCommands
import net.novauniverse.games.survivalgames.game.SurvivalGames
import net.novauniverse.games.survivalgames.map.mapmodules.countdown.SurvivalGamesCountdownConfig
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocation.ExtendedSpawnLocationConfig
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.commons.utils.JSONFileUtils
import net.zeeraa.novacore.spigot.NovaCore
import net.zeeraa.novacore.spigot.abstraction.events.VersionIndependentPlayerAchievementAwardedEvent
import net.zeeraa.novacore.spigot.gameengine.NovaCoreGameEngine
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodule.MapModuleManager
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.mapselector.selectors.RandomMapSelector
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.mapselector.selectors.guivoteselector.GUIMapVote
import net.zeeraa.novacore.spigot.gameengine.module.modules.gamelobby.GameLobby
import net.zeeraa.novacore.spigot.language.LanguageReader
import net.zeeraa.novacore.spigot.module.ModuleManager
import net.zeeraa.novacore.spigot.module.modules.compass.event.CompassTrackingEvent
import net.zeeraa.novacore.spigot.utils.materialwrapper.WrappedBukkitMaterial
import net.zeeraa.novacore.spigot.utils.materialwrapper.WrappedMaterial
import org.apache.commons.io.FileUtils
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.json.JSONObject
import java.io.File
import java.io.IOException

class SurvivalGamesPlugin : JavaPlugin(), Listener {
    var game: SurvivalGames? = null
        private set

    var survivalGamesConfig: SurvivalGamesConfig? = null
        private set

    override fun onEnable() {
        saveDefaultConfig()

        Metrics(this, 18495)

        survivalGamesConfig = SurvivalGamesConfig(config)

        var mapDataDirectoryNameOverride = "SurvivalGames"
        val overridesFile = File(dataFolder.path + File.separator + "overrides.json")
        if (overridesFile.exists()) {
            try {
                val overrides: JSONObject = JSONFileUtils.readJSONObjectFromFile(overridesFile)
                if (overrides.has("map_data_folder_name")) {
                    mapDataDirectoryNameOverride = overrides.getString("map_data_folder_name")
                    Log.info("SurvivalGames", "Map data folder name override set to: $mapDataDirectoryNameOverride")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.error("Failed to read overrides from file " + overridesFile.absolutePath)
            }
        }

        var mapFolder = File(dataFolder.path + File.separator + "Maps")
        var worldFolder = File(dataFolder.path + File.separator + "Worlds")
        var lootTableFolder = File(dataFolder.path + File.separator + "LootTables")

        if (NovaCoreGameEngine.getInstance().requestedGameDataDirectory != null) {
            mapFolder = File(NovaCoreGameEngine.getInstance().requestedGameDataDirectory.absolutePath + File.separator + mapDataDirectoryNameOverride + File.separator + "Maps");
            worldFolder = File(NovaCoreGameEngine.getInstance().requestedGameDataDirectory.absolutePath + File.separator + mapDataDirectoryNameOverride + File.separator + "Worlds");
            lootTableFolder = File(NovaCoreGameEngine.getInstance().requestedGameDataDirectory.absolutePath + File.separator + mapDataDirectoryNameOverride + File.separator + "LootTables");
        }

        Log.info("NovaSurvivalGames", "Loading language files...")
        try {
            LanguageReader.readFromJar(this.javaClass, "/lang/en-us.json")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        try {
            FileUtils.forceMkdir(dataFolder)
            FileUtils.forceMkdir(mapFolder)
            FileUtils.forceMkdir(worldFolder)
            FileUtils.forceMkdir(lootTableFolder)
        } catch (e1: IOException) {
            e1.printStackTrace()
            Log.fatal("SurvivalGames", "Failed to setup data directory")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        ModuleManager.enable(GameManager::class.java)
        if (!survivalGamesConfig!!.dontUseGameLobby) {
            ModuleManager.enable(GameLobby::class.java)
        }

        MapModuleManager.addMapModule("novauniverse.survivalgames.extendedspawnlocation.config", ExtendedSpawnLocationConfig::class.java)
        MapModuleManager.addMapModule("novauniverse.survivalgames.countdown.config", SurvivalGamesCountdownConfig::class.java)

        game = SurvivalGames(this)

        val gameManager = ModuleManager.getModule(GameManager::class.java)
        gameManager.isUseCombatTagging = survivalGamesConfig!!.combatTagging
        gameManager.loadGame(game)

        Bukkit.getServer().pluginManager.registerEvents(this, this)

        if (!survivalGamesConfig!!.dontUseGameLobby) {
            val mapSelector = GUIMapVote()
            gameManager.mapSelector = mapSelector
            Bukkit.getServer().pluginManager.registerEvents(mapSelector, this)
        } else {
            gameManager.mapSelector = RandomMapSelector()
        }

        NovaCore.getInstance().lootTableManager.loadAll(lootTableFolder)

        Log.info(name, "Scheduled loading maps from " + mapFolder.path)
        GameManager.getInstance().readMapsFromFolderDelayed(mapFolder, worldFolder)

        DebugCommands()
    }

    override fun onDisable() {
        HandlerList.unregisterAll(this as Plugin)
        Bukkit.getScheduler().cancelTasks(this)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onCompassTracking(e: CompassTrackingEvent) {
        var enabled = false
        if (GameManager.getInstance().isEnabled) {
            if (GameManager.getInstance().hasGame()) {
                if (GameManager.getInstance().activeGame.hasStarted()) {
                    enabled = true
                }
            }
        }
        e.isCancelled = !enabled
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onTheLongBoi(e: VersionIndependentPlayerAchievementAwardedEvent) {
        e.isCancelled = true
    }

    companion object {
        private var gameInstance: SurvivalGamesPlugin? = null

        val DEFAULT_EXTENDED_SPAWN_FLOOR_MATERIAL: WrappedMaterial = WrappedBukkitMaterial(Material.BARRIER)

        @JvmStatic
        fun getInstance(): SurvivalGamesPlugin {
            return gameInstance!!
        }
    }
}
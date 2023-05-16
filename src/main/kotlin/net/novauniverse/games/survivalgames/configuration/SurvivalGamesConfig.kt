package net.novauniverse.games.survivalgames.configuration

import org.bukkit.configuration.file.FileConfiguration

class SurvivalGamesConfig(configFile: FileConfiguration) {
    var allowReconnect = configFile.getBoolean("AllowReconnect")
    val combatTagging = configFile.getBoolean("CombatTagging")
    val playerEliminationDelay = configFile.getInt("PlayerEliminationDelay")
    var useExtendedSpawnLocations = configFile.getBoolean("ExtendedSpawnLocations")
    var disableEarlyBlockBreakCheck = configFile.getBoolean("DisableEarlyBlockBreakCheck")
    val dontUseGameLobby = configFile.getBoolean("DontUseGameLobby")
    var disableDefaultEndSound = configFile.getBoolean("DisableDefaultEndSound")
    var disableBuiltInCountdownSound = configFile.getBoolean("DisableBuiltInCountdownSound")
    var disableActionBar = configFile.getBoolean("DisableActionBar")
    var disableChatCountdown = configFile.getBoolean("DisableChatCountdown")
}
package net.novauniverse.games.survivalgames.configuration

import org.bukkit.configuration.file.FileConfiguration

class SurvivalGamesConfig(configFile: FileConfiguration) {
    @get:JvmName("isAllowReconnect")
    var allowReconnect = configFile.getBoolean("AllowReconnect")

    @get:JvmName("isUsecombatTaggings")
    val combatTagging = configFile.getBoolean("CombatTagging")
    val playerEliminationDelay = configFile.getInt("PlayerEliminationDelay")

    @get:JvmName("isUseExtendedSpawnLocations")
    var useExtendedSpawnLocations = configFile.getBoolean("ExtendedSpawnLocations")

    @get:JvmName("isDisableEarlyBlockBreakCheck")
    var disableEarlyBlockBreakCheck = configFile.getBoolean("DisableEarlyBlockBreakCheck")

    @get:JvmName("isDontUseGameLobby")
    val dontUseGameLobby = configFile.getBoolean("DontUseGameLobby")

    @get:JvmName("isDisableDefaultEndSound")
    var disableDefaultEndSound = configFile.getBoolean("DisableDefaultEndSound")

    @get:JvmName("isDisableBuiltInCountdownSound")
    var disableBuiltInCountdownSound = configFile.getBoolean("DisableBuiltInCountdownSound")

    @get:JvmName("isDisableActionBar")
    var disableActionBar = configFile.getBoolean("DisableActionBar")

    @get:JvmName("isDisableChatCountdown")
    var disableChatCountdown = configFile.getBoolean("DisableChatCountdown")
}
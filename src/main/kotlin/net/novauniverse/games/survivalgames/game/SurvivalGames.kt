package net.novauniverse.games.survivalgames.game

import net.novauniverse.games.survivalgames.SurvivalGamesPlugin
import net.novauniverse.games.survivalgames.SurvivalGamesPlugin.Companion.getInstance
import net.novauniverse.games.survivalgames.event.SurvivalgamesCountdownEvent
import net.novauniverse.games.survivalgames.map.mapmodules.countdown.SurvivalGamesCountdownConfig
import net.novauniverse.games.survivalgames.map.mapmodules.extendedspawnlocation.ExtendedSpawnLocationConfig
import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.spigot.NovaCore
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameEndReason
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.MapGame
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.elimination.PlayerQuitEliminationAction
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.events.GameBeginEvent
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodules.graceperiod.graceperiod.event.GracePeriodBeginEvent
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.triggers.DelayedGameTrigger
import net.zeeraa.novacore.spigot.language.LanguageManager
import net.zeeraa.novacore.spigot.teams.Team
import net.zeeraa.novacore.spigot.teams.TeamManager
import net.zeeraa.novacore.spigot.timers.BasicTimer
import net.zeeraa.novacore.spigot.utils.PlayerUtils
import net.zeeraa.novacore.spigot.utils.RandomFireworkEffect
import net.zeeraa.novacore.spigot.utils.materialwrapper.WrappedMaterial
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import java.util.*
import java.util.function.Consumer


class SurvivalGames(@SuppressWarnings("WeakerAccess") val plugin: SurvivalGamesPlugin) : MapGame(plugin), Listener {
    private var started = false

    private var ended = false

    private val usedStartLocation: HashMap<UUID, Location> = HashMap()
    private val teamSpawnLocation: HashMap<Team, Location> = HashMap()
    private val temporaryCageBlocks: HashMap<Location, Material> = HashMap()

    private var modifierBeginCalled: Boolean = false

    val modifiers: ArrayList<Modifier> = ArrayList()

    @get:JvmName("getCountdownTime")
    var countdownTime: Int = 20
        private set

    @get:JvmName("isCountdownOver")
    var countdownOver = false
        private set
    private var allowDamage = false

    @get:JvmName("isCountdownStarted")
    var countdownStarted = false
        private set

    @get:JvmName("isHasWorldBorder")
    var hasWorldBorder = false
        private set

    override fun getName(): String {
        return "survivalgames"
    }

    override fun getDisplayName(): String {
        return "SurvivalGames"
    }

    override fun getPlayerQuitEliminationAction(): PlayerQuitEliminationAction {
        return if (plugin.survivalGamesConfig!!.allowReconnect) PlayerQuitEliminationAction.DELAYED else PlayerQuitEliminationAction.INSTANT
    }

    override fun getPlayerEliminationDelay(): Int {
        return plugin.survivalGamesConfig?.playerEliminationDelay ?: 120
    }

    override fun eliminatePlayerOnDeath(p0: Player?): Boolean {
        return true
    }

    override fun isPVPEnabled(): Boolean {
        return countdownOver
    }

    override fun autoEndGame(): Boolean {
        return true
    }

    override fun hasStarted(): Boolean {
        return started
    }

    override fun hasEnded(): Boolean {
        return ended
    }

    override fun isFriendlyFireAllowed(): Boolean {
        return false
    }

    override fun canAttack(p0: LivingEntity?, p1: LivingEntity?): Boolean {
        return countdownOver
    }

    override fun tpToSpectator(player: Player) {
        PlayerUtils.resetMaxHealth(player)
        PlayerUtils.fullyHealPlayer(player)
        player.gameMode = GameMode.SPECTATOR
        if (hasActiveMap()) {
            player.teleport(activeMap.spectatorLocation)
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun tpToArena(player: Player) {
        if (hasActiveMap()) {
            if (plugin.survivalGamesConfig!!.useExtendedSpawnLocations && TeamManager.hasTeamManager()) {
                // Use new version
                val team: Team? = TeamManager.getTeamManager().getPlayerTeam(player)
                if (team != null) {
                    if (teamSpawnLocation.containsKey(team)) {
                        tpToArena(player, teamSpawnLocation[team]!!)
                        return
                    } else {
                        for (i in activeMap.starterLocations.indices) {
                            val location = activeMap.starterLocations[i]
                            if (teamSpawnLocation.containsValue(location)) {
                                // Already in use
                                continue
                            }
                            teamSpawnLocation[team] = location
                            tpToArena(player, location)
                            return
                        }
                    }
                }
            } else {
                // Use old version
                for (i in activeMap.starterLocations.indices) {
                    val location = activeMap.starterLocations[i]
                    if (usedStartLocation.containsValue(location)) {
                        // Already in use
                        continue
                    }
                    tpToArena(player, location)
                    return
                }
            }

            // Use fallback
            Log.warn("SurvivalGames", "Using random location for " + player.name + " since for some reason getting an empty spawn location for them failed")
            val backupLocation = activeMap.starterLocations[random.nextInt(activeMap.starterLocations.size)]
            tpToArena(player, backupLocation)
        } else {
            Log.error("SurvivalGames", "SurvivalGames#tpToArena() called without map")
        }
    }

    /**
     * Teleport a player to a provided start location
     *
     * @param player   [Player] to teleport
     * @param location [Location] to teleport the player to
     */
    @SuppressWarnings("WeakerAccess")
    fun tpToArena(player: Player, location: Location) {
        player.teleport(location.world.spawnLocation)
        usedStartLocation[player.uniqueId] = location
        PlayerUtils.clearPlayerInventory(player)
        PlayerUtils.clearPotionEffects(player)
        PlayerUtils.resetPlayerXP(player)
        player.health = player.maxHealth
        player.saturation = 20f
        player.foodLevel = 20
        player.teleport(location)
        player.gameMode = GameMode.SURVIVAL
    }


    override fun onStart() {
        if (started) {
            return
        }

        Bukkit.getServer().worlds.forEach {
            it.isAutoSave = false
        }

        world.isAutoSave = false

        this.isDropItemsOnCombatLog = true

        var toTeleport: ArrayList<Player> = ArrayList()
        for (player in Bukkit.getServer().onlinePlayers) {
            if (players.contains(player.uniqueId)) {
                Log.trace(player.name + " is in the player list")
                toTeleport.add(player)
            } else {
                PlayerUtils.clearPlayerInventory(player)
                tpToSpectator(player)
            }
        }

        Bukkit.getServer().onlinePlayers.forEach { PlayerUtils.setMaxHealth(it, 20.0) }

        if (plugin.survivalGamesConfig!!.shuffleSpawnLocations) {
            activeMap.mapData.starterLocations.shuffle(random)
            activeMap.starterLocations.shuffle(random)
        }

        activeMap.world.setGameRuleValue("doMobSpawning", "false")
        activeMap.world.difficulty = Difficulty.PEACEFUL

        Bukkit.getScheduler().scheduleSyncDelayedTask(getInstance(), { activeMap.world.difficulty = Difficulty.NORMAL }, 200L)

        if (NovaCore.getInstance().hasTeamManager()) {
            val teamOrder: ArrayList<UUID> = ArrayList()
            NovaCore.getInstance().teamManager.teams.forEach(Consumer { team: Team -> teamOrder.add(team.teamUuid) })
            teamOrder.shuffle(getRandom())
            val toTeleportReal: ArrayList<Player> = ArrayList()
            for (uuid in teamOrder) {
                for (pt in toTeleport) {
                    if (NovaCore.getInstance().teamManager.getPlayerTeam(pt)!!.teamUuid == uuid) {
                        toTeleportReal.add(pt)
                    }
                }
            }
            toTeleport = toTeleportReal
        }

        if (activeMap.mapData.hasMapModule(SurvivalGamesCountdownConfig::class.java)) {
            countdownTime = activeMap.mapData.getMapModule(SurvivalGamesCountdownConfig::class.java)!!.initialCountdownValue
        }

        Log.debug("Final toTeleport size: " + toTeleport.size)

        activeMap.starterLocations.forEach(Consumer { location: Location? -> setStartCage(location!!, true) })

        toTeleport.forEach(Consumer { player: Player? -> tpToArena(player!!) })

        started = true

        startCountdown()

        Bukkit.getOnlinePlayers().forEach { p: Player -> p.setBedSpawnLocation(activeMap.spectatorLocation, true) }

        getWorld().setGameRuleValue("announceAdvancements", "false")

        modifiers.stream().filter(Modifier::enabled).forEach(Modifier::onGameStart)

        val modifierCount = modifiers.stream().filter(Modifier::enabled).count().toInt()
        if (modifierCount > 0) {
            if (modifierCount == 1) {
                val singleMod = modifiers.stream().filter(Modifier::enabled).findFirst().get()
                VersionIndependentUtils.get().broadcastTitle("${ChatColor.GOLD}Active Modifier", ChatColor.AQUA.toString() + singleMod.getDisplayName(), 0, 60, 20)
            } else {
                VersionIndependentUtils.get().broadcastTitle("${ChatColor.GOLD}Multiple Modifiers", "", 0, 60, 20)
            }

            modifiers.stream().filter(Modifier::enabled).forEach {
                Bukkit.getServer().broadcastMessage("${ChatColor.GOLD.toString() + ChatColor.BOLD.toString()}Active modifier: ${ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + it.getDisplayName()}")
                Bukkit.getServer().broadcastMessage(ChatColor.WHITE.toString() + it.getShortDescription())
            }
        }
    }

    override fun onEnd(reason: GameEndReason?) {
        ended = true

        activeMap.starterLocations.forEach(Consumer { location: Location ->
            val fw = location.world.spawnEntity(location, EntityType.FIREWORK) as Firework
            val fwm = fw.fireworkMeta
            fwm.power = 2
            fwm.addEffect(RandomFireworkEffect.randomFireworkEffect())
            if (random.nextBoolean()) {
                fwm.addEffect(RandomFireworkEffect.randomFireworkEffect())
            }
            fw.fireworkMeta = fwm
        })

        Bukkit.getServer().onlinePlayers.forEach { player: Player ->
            VersionIndependentUtils.get().resetEntityMaxHealth(player)
            player.foodLevel = 20
            PlayerUtils.clearPlayerInventory(player)
            PlayerUtils.resetPlayerXP(player)
            player.gameMode = GameMode.SPECTATOR
            if (!plugin.survivalGamesConfig!!.disableDefaultEndSound) {
                VersionIndependentUtils.get().playSound(player, player.location, VersionIndependentSound.WITHER_DEATH, 1F, 1F)
            }
        }

        modifiers.stream().filter(Modifier::enabled).forEach(Modifier::onGameEnd)
    }

    fun loadModifier(c: Class<out Modifier?>): Boolean {
        if (modifiers.stream().anyMatch { it.javaClass.name.equals(c.javaClass.name, true) }) {
            return false
        }

        val mod: Modifier = c.newInstance()!!
        mod.onLoad()
        modifiers.add(mod)

        Log.debug("SurvivalGames", "Added modifier ${mod.name} to available modifiers")

        return true
    }

    @SuppressWarnings("WeakerAccess")
    fun startCountdown(): Boolean {
        if (!started) {
            return false
        }

        if (countdownStarted) {
            return false
        }

        countdownStarted = true
        Bukkit.getServer().onlinePlayers.forEach { player: Player ->
            if (!plugin.survivalGamesConfig!!.disableChatCountdown) {
                player.sendMessage(LanguageManager.getString(player, "survivalgames.game.starting_in", countdownTime))
            }
            VersionIndependentUtils.get().sendTitle(player, "", LanguageManager.getString(player, "survivalgames.game.starting_in_title", countdownTime), 10, 20, 10)
        }

        val startTimer = BasicTimer(countdownTime.toLong(), 20L)
        startTimer.addFinishCallback {
            countdownOver = true
            setCages(false)
            allowDamage = true
            Bukkit.getServer().onlinePlayers.forEach { player: Player? ->
                player!!.foodLevel = 20
                player.saturation = 20f
                if (!plugin.survivalGamesConfig!!.disableBuiltInCountdownSound) {
                    VersionIndependentUtils.get().playSound(player, player.location, VersionIndependentSound.NOTE_PLING, 1f, 1f)
                }
            }
            sendBeginEvent()
        }
        startTimer.addTickCallback { timeLeft ->
            val event: Event = SurvivalgamesCountdownEvent(timeLeft.toInt())
            Bukkit.getServer().pluginManager.callEvent(event)
            Bukkit.getServer().onlinePlayers.forEach { player: Player? ->
                if (!plugin.survivalGamesConfig!!.disableBuiltInCountdownSound) {
                    VersionIndependentUtils.get().playSound(player, player!!.location, VersionIndependentSound.NOTE_PLING, 1f, 1.3f)
                }
                if (plugin.survivalGamesConfig!!.disableActionBar) {
                    if (timeLeft > 0) {
                        VersionIndependentUtils.getInstance().sendTitle(player, "", LanguageManager.getString(player, "survivalgames.game.starting_in_title", timeLeft), 0, 20, 5)
                    }
                } else {
                    VersionIndependentUtils.get().sendActionBarMessage(player, LanguageManager.getString(player, "survivalgames.game.starting_in", timeLeft))
                }
                if (!plugin.survivalGamesConfig!!.disableChatCountdown) {
                    if (timeLeft > 0) {
                        player!!.sendMessage(LanguageManager.getString(player, "survivalgames.game.starting_in", timeLeft))
                    }
                }
            }
        }
        startTimer.start()

        if (hasWorldBorder) {
            val trigger = getTrigger("novacore.worldborder.start")
            if (trigger != null) {
                (trigger as DelayedGameTrigger).start()
            }
        }
        return true
    }

    @SuppressWarnings("WeakerAccess")
    fun setStartCage(location: Location, active: Boolean) {
        val cageMaterial = if (active) Material.BARRIER else Material.AIR
        if (plugin.survivalGamesConfig!!.useExtendedSpawnLocations) {
            // Box
            for (x in -2..2) {
                for (y in 0..3) {
                    for (z in -2..2) {
                        location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type = cageMaterial
                    }
                }
            }

            // Inside
            if (active) {
                for (x in -1..1) {
                    for (y in 0..2) {
                        for (z in -1..1) {
                            location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type = Material.AIR
                        }
                    }
                }
            }
            if (active) {
                // Cage floor
                val config = activeMap.mapData.getMapModule(ExtendedSpawnLocationConfig::class.java)
                var floorMaterial: WrappedMaterial? = SurvivalGamesPlugin.DEFAULT_EXTENDED_SPAWN_FLOOR_MATERIAL
                var keepFloor = false
                if (config != null) {
                    if (config.disabled) {
                        return
                    }
                    floorMaterial = config.floorMaterial
                    keepFloor = config.keepAfterStart
                }

                // Bottom
                val floor = location.clone().add(0.0, -1.0, 0.0)
                for (x in -1..1) {
                    for (z in -1..1) {
                        val l = floor.clone().add(x.toDouble(), 0.0, z.toDouble())
                        val block: Block = l.block
                        if (block.isLiquid || !block.type.isSolid) {
                            if (!keepFloor) {
                                temporaryCageBlocks[l] = block.type
                            }
                            floorMaterial!!.setBlock(block)
                        }
                    }
                }
            }
        } else {
            // Use old version
            location.clone().add(1.0, 0.0, 0.0).block.type = cageMaterial
            location.clone().add(-1.0, 0.0, 0.0).block.type = cageMaterial
            location.clone().add(0.0, 0.0, 1.0).block.type = cageMaterial
            location.clone().add(0.0, 0.0, -1.0).block.type = cageMaterial
            location.clone().add(1.0, 1.0, 0.0).block.type = cageMaterial
            location.clone().add(-1.0, 1.0, 0.0).block.type = cageMaterial
            location.clone().add(0.0, 1.0, 1.0).block.type = cageMaterial
            location.clone().add(0.0, 1.0, -1.0).block.type = cageMaterial
            location.clone().add(0.0, 2.0, 0.0).block.type = cageMaterial
        }
    }

    fun setCages(active: Boolean): Boolean {
        if (!hasActiveMap()) {
            return false
        }

        if (!active) {
            // Clean up cage floor blocks
            temporaryCageBlocks.forEach { (location: Any, material: Any?) -> location.block.type = material }
            temporaryCageBlocks.clear()
        }

        activeMap.starterLocations.forEach(Consumer<Location> { location: Location -> setStartCage(location, active) })

        return true
    }

    override fun onPlayerRespawnEvent(event: PlayerRespawnEvent) {
        if (hasActiveMap()) {
            event.respawnLocation = activeMap.spectatorLocation
        }
    }

    override fun onPlayerRespawn(player: Player) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(NovaCore.getInstance(), {
            Log.trace(name, "Calling tpToSpectator(" + player.name + ")")
            tpToSpectator(player)
        }, 5L)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onGameBegin(e: GameBeginEvent) {
        if(modifierBeginCalled) {
            return
        }

        modifierBeginCalled = true

        modifiers.stream().filter(Modifier::enabled).forEach(Modifier::onGameBegin)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamage(e: EntityDamageEvent) {
        if (e.entity is Player) {
            if (!allowDamage) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (!hasStarted()) {
            PlayerUtils.clearPotionEffects(e.player)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockBreak(e: BlockBreakEvent) {
        if (plugin.survivalGamesConfig!!.disableEarlyBlockBreakCheck) {
            return
        }
        if (!countdownOver) {
            if (e.player.gameMode != GameMode.CREATIVE) {
                e.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onGracePeriodBegin(e: GracePeriodBeginEvent) {
        Log.info("SurvivalGames", e.javaClass.name)
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (plugin.survivalGamesConfig!!.disableEarlyBlockBreakCheck) {
            return
        }
        if (!countdownOver) {
            if (e.player.gameMode != GameMode.CREATIVE) {
                e.isCancelled = true
            }
        }
    }
}
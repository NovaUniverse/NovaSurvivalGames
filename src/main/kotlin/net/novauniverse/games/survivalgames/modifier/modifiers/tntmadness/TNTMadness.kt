package net.novauniverse.games.survivalgames.modifier.modifiers.tntmadness

import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.spigot.abstraction.VersionIndependentUtils
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentSound
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.events.GameBeginEvent
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodules.graceperiod.graceperiod.GracePeriodMapModule
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.map.mapmodules.graceperiod.graceperiod.event.GracePeriodFinishEvent
import net.zeeraa.novacore.spigot.tasks.SimpleTask
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.util.Vector

class TNTMadness : Modifier("TNTMadness") {
    var task: SimpleTask? = null;

    val fallingBlocks = ArrayList<FallingBlock>()

    override fun getDisplayName(): String {
        return "TNT Madness"
    }

    override fun getIconMaterial(): Material {
        return Material.TNT
    }

    override fun getShortDescription(): String {
        return "Makes it rain tnt every 30 seconds"
    }

    override fun onLoad() {
        task = SimpleTask({
            fallingBlocks.removeIf(Entity::isDead)

            Bukkit.getOnlinePlayers().stream().filter { it.gameMode == GameMode.SURVIVAL && getGame().players.contains(it.uniqueId) }.forEach {
                val location = it.location.clone().add(Vector(0, 30, 0))
                val fallingBlock = VersionIndependentUtils.get().spawnFallingBlock(location, Material.TNT, 0, null)
                fallingBlocks.add(fallingBlock)
            }
        }, SPAWN_DELAY, SPAWN_DELAY)
    }

    override fun onGameStart() {
    }

    override fun onGameEnd() {
        task?.stop()
    }

    companion object {
        const val SPAWN_DELAY: Long = 20L * 30
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onGracePeriodFinish(e: GracePeriodFinishEvent) {
        task?.start()
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onGameBegin(e: GameBeginEvent) {
        if (!getGame().activeMap.mapData.hasMapModule(GracePeriodMapModule::class.java)) {
            task?.start()
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onEntityChangeBlock(e: EntityChangeBlockEvent) {
        if (fallingBlocks.contains(e.entity)) {
            e.isCancelled = true
            e.entity.remove()
            val location = e.entity.location
            location.world.spawnEntity(location, EntityType.PRIMED_TNT)
            VersionIndependentSound.FUSE.playAtLocation(location)
        }
    }
}
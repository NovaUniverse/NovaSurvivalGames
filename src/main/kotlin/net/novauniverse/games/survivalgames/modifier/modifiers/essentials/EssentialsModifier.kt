package net.novauniverse.games.survivalgames.modifier.modifiers.essentials

import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.commons.utils.DelayedRunner
import net.zeeraa.novacore.spigot.abstraction.enums.VersionIndependentMaterial
import net.zeeraa.novacore.spigot.loottable.event.LootTableGeneratedEvent
import net.zeeraa.novacore.spigot.module.ModuleManager
import net.zeeraa.novacore.spigot.module.modules.chestloot.events.ChestRefillEvent
import net.zeeraa.novacore.spigot.module.modules.compass.CompassTracker
import net.zeeraa.novacore.spigot.module.modules.customitems.CustomItemManager
import net.zeeraa.novacore.spigot.module.modules.lootdrop.event.LootDropSpawnEvent
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class EssentialsModifier : Modifier("SurvivalGames.Essentials") {
    override fun getDisplayName(): String {
        return "Essentials"
    }

    override fun getIconMaterial(): Material {
        return VersionIndependentMaterial.WOODEN_PICKAXE.toBukkitVersion()
    }

    override fun getShortDescription(): String {
        return "No Refills, Supply drops are not announced in chat, no custom items and no tracking compass"
    }

    override fun onGameStart() {
        DelayedRunner.runDelayed({
            Log.debug("EssentialsModifier", "Disabling trackers")
            ModuleManager.disable(CompassTracker::class.java)
        }, 20L)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onLootDropSpawn(e: LootDropSpawnEvent) {
        e.isHideMessage = true
        Log.debug("EssentialsModifier", "Disabling loot drop message")
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onChestRefill(e: ChestRefillEvent) {
        e.isCancelled = true
        Log.debug("EssentialsModifier", "Cancel chest refill")
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onLootTableGenerated(e: LootTableGeneratedEvent) {
        val customItemManager = ModuleManager.getModule(CustomItemManager::class.java)
        if (customItemManager.isEnabled) {
            val originalSize = e.generatedContent.size
            e.generatedContent.removeIf(customItemManager::isCustomItem)
            if (originalSize != e.generatedContent.size) {
                Log.debug("EssentialsModifier", "Changed content of generated loot table ${e.lootTable.name}. original size $originalSize. new size: ${e.generatedContent.size}")
            }
        }
    }
}

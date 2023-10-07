package net.novauniverse.games.survivalgames.modifier.modifiers.famine

import net.novauniverse.games.survivalgames.modifier.Modifier
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.commons.utils.RandomGenerator
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.events.GameBeginEvent
import net.zeeraa.novacore.spigot.loottable.event.LootTableGeneratedEvent
import org.bukkit.Difficulty
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class FamineModifier : Modifier("SurvivalGames.Famine") {
    override fun getDisplayName(): String {
        return "Famine"
    }

    override fun getIconMaterial(): Material {
        return Material.ROTTEN_FLESH
    }

    override fun getShortDescription(): String {
        return "Food is extremely rare and difficulty is set to hard"
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onGameBegin(e: GameBeginEvent) {
        e.game.world.difficulty = Difficulty.HARD
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onLootTableGenerate(e: LootTableGeneratedEvent) {
        for (i in e.generatedContent.indices.reversed()) {
            val item = e.generatedContent[i]
            if (item.type.isEdible) {
                if (RandomGenerator.generate(1, 4) != 3) {
                    Log.trace("FamineModifier", "Remove " + item + " from loot table " + e.lootTable.displayName)
                    e.generatedContent.removeAt(i)
                }
            }
        }
    }
}
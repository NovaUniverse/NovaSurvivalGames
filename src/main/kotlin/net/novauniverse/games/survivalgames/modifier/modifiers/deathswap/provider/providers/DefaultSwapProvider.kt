package net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider.providers

import net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider.DeathSwapProvider
import net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider.SwapResult
import net.zeeraa.novacore.commons.log.Log
import net.zeeraa.novacore.commons.utils.platformindependent.PlatformIndependentPlayerAPI
import net.zeeraa.novacore.spigot.gameengine.module.modules.game.GameManager
import net.zeeraa.novacore.spigot.teams.Team
import net.zeeraa.novacore.spigot.teams.TeamManager
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import java.util.*
import kotlin.collections.HashMap

class DefaultSwapProvider(val useTeams: Boolean) : DeathSwapProvider {
    private val random = Random()

    override fun onGameStart() {}

    override fun onGameEnd() {}

    override fun swap(): SwapResult {
        return if (useTeams) {
            swapWithTeams()
        } else {
            swapWithoutTeams()
        }
    }

    fun swapWithoutTeams(): SwapResult {
        val players: ArrayList<Player> = ArrayList()
        for (player in Bukkit.getServer().onlinePlayers) {
            if (player.isOnline) {
                if (GameManager.getInstance().activeGame.players.contains(player.uniqueId)) {
                    players.add(player)
                }
            }
        }
        if (players.size < 2) {
            return SwapResult.NOT_ENOUGH_PLAYERS
        }
        var tries = 0
        while (true) {
            /*
             * Should not happen, but I don't want the server to crash if my shitty code does
             * not work
             */
            if (tries > MAX_TRIES) {
                Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED.toString() + "Error: Swap provider " + this.javaClass.name + " timed out after " + tries + " tries")
                return SwapResult.ERROR
            }
            tries++
            val toAdd: MutableList<Int> = ArrayList()
            val swapData: MutableMap<Int, Int> = HashMap()

            // Setup toAdd list
            for (i in players.indices) {
                toAdd.add(i)
            }

            // Insert random data and hope that it forks first try
            for (i in players.indices) {
                swapData[i] = toAdd.removeAt(random.nextInt(toAdd.size))
            }

            /*
             * Validate the data because we use brute force instead of carefully created
             * algorithms
             */
            var isFailure = false
            for (i in swapData.keys) {
                if (swapData[i] as Int == i) {
                    isFailure = true // Relatable
                    break
                }
            }

            // Reject failures
            if (isFailure) {
                continue
            }

            // Check where to teleport players to
            val tpTo: HashMap<Player, Location> = HashMap()
            for (i in swapData.keys) {
                tpTo[players[i]] = players[swapData[i]!!].location.clone()
            }

            // Teleport them
            for (player in tpTo.keys) {
                player.teleport(tpTo[player], PlayerTeleportEvent.TeleportCause.PLUGIN)
            }
            Log.debug("DefaultSwapProvider", "Success after $tries tries")
            return SwapResult.SUCCESS
        }
    }

    fun swapWithTeams(): SwapResult {
        val teams: ArrayList<Team> = ArrayList()
        for (team in TeamManager.getTeamManager().teams) {
            for (uuid in team.members) {
                if (GameManager.getInstance().activeGame.players.contains(uuid)) {
                    // I could use the bukkit api but this will do it in one line
                    if (PlatformIndependentPlayerAPI.get().isOnline(uuid)) {
                        teams.add(team)
                        break // Almost forgot
                    }
                }
            }
        }
        if (teams.size < 2) {
            return SwapResult.NOT_ENOUGH_TEAMS
        }
        var tries = 0
        while (true) {
            /*
             * Should not happen, but I don't want the server to crash if my shitty code does
             * not work
             */
            if (tries > MAX_TRIES) {
                Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED.toString() + "Error: Swap provider " + this.javaClass.name + " timed out after " + tries + " tries")
                return SwapResult.ERROR
            }
            tries++
            val toAdd: ArrayList<Int> = ArrayList()
            val swapData: HashMap<Int, Int> = HashMap()

            // Setup toAdd list
            for (i in teams.indices) {
                toAdd.add(i)
            }

            // Insert random data and hope that it works first try
            for (i in teams.indices) {
                swapData[i] = toAdd.removeAt(random.nextInt(toAdd.size))
            }

            /*
             * Validate the data because we use brute force instead of carefully created
             * algorithms
             */
            var isFailure = false
            for (i in swapData.keys) {
                if (swapData[i] as Int == i) {
                    isFailure = true // Relatable
                    break
                }
            }

            // Reject failures
            if (isFailure) {
                continue
            }

            // Check where to teleport players to
            val tpTo: HashMap<Player, Location> = HashMap()
            for (i in swapData.keys) {
                val team1 = teams[i]
                val team2 = teams[swapData[i]!!]
                val team1Players = getTeamOnlineAndAlivePlayers(team1)
                val team2Players = getTeamOnlineAndAlivePlayers(team2)
                if (team1Players.size == 0 || team2Players.size == 0) {
                    Log.error("DefaultSwapProvider", "team1Player or team2Players is empty in DefaultSwapProvider")
                    continue
                }
                if (team2Players.size >= team1Players.size) {
                    // Use real swap
                    for (p in team1Players) {
                        tpTo[p] = team2Players.removeAt(random.nextInt(team2Players.size)).getLocation().clone()
                    }
                } else {
                    // Use random
                    for (p in team1Players) {
                        tpTo[p] = team2Players[random.nextInt(team2Players.size)].location.clone()
                    }
                }
            }

            // Teleport them
            for (player in tpTo.keys) {
                player.teleport(tpTo[player], PlayerTeleportEvent.TeleportCause.PLUGIN)
            }
            Log.debug("DefaultSwapProvider", "Success after $tries tries")
            return SwapResult.SUCCESS
        }
    }

    fun getTeamOnlineAndAlivePlayers(team: Team): ArrayList<Player> {
        val result: ArrayList<Player> = ArrayList()
        for (uuid in team.members) {
            val player = Bukkit.getServer().getPlayer(uuid)
            if (player != null) {
                if (player.isOnline) {
                    if (GameManager.getInstance().activeGame.players.contains(player.uniqueId)) {
                        if (player.gameMode != GameMode.SPECTATOR) {
                            result.add(player)
                        }
                    }
                }
            }
        }
        return result
    }

    companion object {
        const val MAX_TRIES = 10000
    }
}
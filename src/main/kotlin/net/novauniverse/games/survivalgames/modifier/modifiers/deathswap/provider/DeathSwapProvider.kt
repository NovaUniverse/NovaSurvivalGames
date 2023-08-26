package net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider

interface DeathSwapProvider {
    fun onGameStart()
    fun onGameEnd()
    fun swap(): SwapResult
}

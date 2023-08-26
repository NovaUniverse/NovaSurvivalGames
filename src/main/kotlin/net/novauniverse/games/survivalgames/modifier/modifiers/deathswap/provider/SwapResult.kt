package net.novauniverse.games.survivalgames.modifier.modifiers.deathswap.provider

enum class SwapResult {
    /**
     * Swap success
     */
    SUCCESS,

    /**
     * Not enough players to swap
     */
    NOT_ENOUGH_PLAYERS,

    /**
     * Not enough teams to swap
     */
    NOT_ENOUGH_TEAMS,

    /**
     * SwapPovider generated an exception or failed
     */
    ERROR
}

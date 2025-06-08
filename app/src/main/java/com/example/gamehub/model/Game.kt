package com.example.gamehub.model

import com.example.gamehub.R

sealed class Game(
    val id: String,
    val title: String,
    val iconRes: Int,
    val online: Boolean            // ← NEW flag
) {
    object BattleShips  : Game("battleships", "BattleShips",  R.drawable.ic_battleships, online = true)
    object OhPardon     : Game("ohpardon",   "Oh Pardon",    R.drawable.ic_ohpardon,    online = true)
    object WhereAndWhen : Game("whereandwhen", "Where & When", R.drawable.ic_where_and_when, online = true)
    object Codenames    : Game("codenames",  "Codenames",    R.drawable.ic_codenames,   online = true)
    object Triviatoe : Game("triviatoe", "Triviatoe", R.drawable.ic_triviatoe, online = true)


    /* single-player or local-only examples */
    object Spy          : Game("spy",        "Spy",          R.drawable.ic_spy,         online = false)
    object JorisJump    : Game("jorisjump",  "Joris Jump",   R.drawable.ic_jorisjump,   online = false)
    object ScreamoSaur  : Game("screamosaur","Scream-O-Saur",R.drawable.ic_screamosaur, online = false)
    object MemoryMatch  : Game("memoryMatching", "Memory Match", R.drawable.ic_memory_match, online = false) // New Game


    companion object {
        val all = listOf(BattleShips, OhPardon, Spy, JorisJump, ScreamoSaur, MemoryMatch, Triviatoe, Codenames, WhereAndWhen)
    }
}

package com.example.gamehub.model

import com.example.gamehub.R

sealed class Game(
    val id: String,
    val title: String,
    val iconRes: Int,
    val online: Boolean,
    val imageRes: Int,
) {
    object BattleShips  : Game("battleships", "BattleShips",  R.drawable.icon_battleships, online = true, R.drawable.battleships_menu)
    object OhPardon     : Game("ohpardon",   "Oh Pardon",    R.drawable.ic_ohpardon,    online = true, R.drawable.ohpardon_gamecard)
    object WhereAndWhen : Game("whereandwhen", "Where & When", R.drawable.ic_where_and_when, online = true, R.drawable.where_and_when_menu)
    object Codenames    : Game("codenames",  "Codenames",    R.drawable.ic_codenames,   online = true, R.drawable.codenames_preview)
    object Triviatoe : Game("triviatoe", "Triviatoe", R.drawable.icon_triviatoe, online = true, R.drawable.triviatoe_menu)


    /* single-player or local-only examples */
    object Spy          : Game("spy",        "Spy",          R.drawable.ic_spy,         online = false, R.drawable.spy_preview)
    object JorisJump    : Game("jorisjump",  "Joris Jump",   R.drawable.ic_jorisjump,   online = false, R.drawable.doodle_menu)
    object ScreamoSaur  : Game("screamosaur","Scream-O-Saur",R.drawable.ic_sceamosaur, online = false, R.drawable.dinasourtemplate)
    object SpaceInvaders  : Game("spaceinvaders","Space Invaders",R.drawable.ic_spaceinvaders, online = false, R.drawable.spaceinvaders_gamecard)
    object MemoryMatch  : Game("memoryMatching", "Memory Match", R.drawable.ic_memory_match, online = false, R.drawable.memory_match_template)


    companion object {
        val all = listOf(BattleShips, OhPardon, Spy, JorisJump, ScreamoSaur, MemoryMatch, Triviatoe, Codenames, WhereAndWhen, SpaceInvaders)
    }
}
